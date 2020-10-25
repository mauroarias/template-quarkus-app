package datadog.lib.datadog.trace.common.writer;

import com.timgroup.statsd.NoOpStatsDClient;
import datadog.lib.datadog.trace.api.Config;
import datadog.trace.api.ConfigDefaults;
import datadog.lib.datadog.trace.common.writer.ddagent.DDAgentApi;
import datadog.lib.datadog.trace.common.writer.ddagent.DDAgentResponseListener;
import datadog.lib.datadog.trace.common.writer.ddagent.PayloadDispatcher;
import datadog.lib.datadog.trace.common.writer.ddagent.Prioritization;
import datadog.lib.datadog.trace.common.writer.ddagent.TraceProcessingWorker;
import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.monitor.HealthMetrics;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DDAgentWriter implements Writer {
    private static final Logger log = LoggerFactory.getLogger(DDAgentWriter.class);
    private static final int BUFFER_SIZE = 1024;
    private final DDAgentApi api;
    private final TraceProcessingWorker traceProcessingWorker;
    private final PayloadDispatcher dispatcher;
    private volatile boolean closed;
    public final HealthMetrics healthMetrics;

    private DDAgentWriter(DDAgentApi agentApi, String agentHost, int traceAgentPort, String unixDomainSocket, long timeoutMillis, int traceBufferSize, HealthMetrics healthMetrics, int flushFrequencySeconds, Prioritization prioritization, Monitoring monitoring, boolean traceAgentV05Enabled) {
        if (agentApi != null) {
            this.api = agentApi;
        } else {
            this.api = new DDAgentApi(String.format("http://%s:%d", agentHost, traceAgentPort), unixDomainSocket, timeoutMillis, traceAgentV05Enabled, monitoring);
        }

        this.healthMetrics = healthMetrics;
        this.dispatcher = new PayloadDispatcher(this.api, healthMetrics, monitoring);
        this.traceProcessingWorker = new TraceProcessingWorker(traceBufferSize, healthMetrics, monitoring, this.dispatcher, null == prioritization ? Prioritization.FAST_LANE : prioritization, (long)flushFrequencySeconds, TimeUnit.SECONDS);
    }

    private DDAgentWriter(DDAgentApi agentApi, HealthMetrics healthMetrics, Monitoring monitoring, TraceProcessingWorker traceProcessingWorker) {
        this.api = agentApi;
        this.healthMetrics = healthMetrics;
        this.dispatcher = new PayloadDispatcher(this.api, healthMetrics, monitoring);
        this.traceProcessingWorker = traceProcessingWorker;
    }

    public void addResponseListener(DDAgentResponseListener listener) {
        this.api.addResponseListener(listener);
    }

    public final long getCapacity() {
        return (long)this.traceProcessingWorker.getCapacity();
    }

    public void write(List<DDSpan> trace) {
        if (!this.closed) {
            if (trace.isEmpty()) {
                this.handleDroppedTrace("Trace was empty", trace);
            } else {
                DDSpan root = (DDSpan)trace.get(0);
                int samplingPriority = root.context().getSamplingPriority();
                if (this.traceProcessingWorker.publish(samplingPriority, trace)) {
                    this.healthMetrics.onPublish(trace, samplingPriority);
                } else {
                    this.handleDroppedTrace("Trace written to overfilled buffer", trace, samplingPriority);
                }
            }
        } else {
            this.handleDroppedTrace("Trace written after shutdown.", trace);
        }

    }

    private void handleDroppedTrace(String reason, List<DDSpan> trace) {
        this.incrementTraceCount();
        log.debug("{}. Counted but dropping trace: {}", reason, trace);
        this.healthMetrics.onFailedPublish(-2147483648);
    }

    private void handleDroppedTrace(String reason, List<DDSpan> trace, int samplingPriority) {
        this.incrementTraceCount();
        log.debug("{}. Counted but dropping trace: {}", reason, trace);
        this.healthMetrics.onFailedPublish(samplingPriority);
    }

    public boolean flush() {
        if (!this.closed && this.traceProcessingWorker.flush(1L, TimeUnit.SECONDS)) {
            this.healthMetrics.onFlush(false);
            return true;
        } else {
            return false;
        }
    }

    public void incrementTraceCount() {
        this.dispatcher.onTraceDropped();
    }

    public DDAgentApi getApi() {
        return this.api;
    }

    public void start() {
        if (!this.closed) {
            this.traceProcessingWorker.start();
            this.healthMetrics.onStart((int)this.getCapacity());
        }

    }

    public void close() {
        boolean flushed = this.flush();
        this.closed = true;
        this.traceProcessingWorker.close();
        this.healthMetrics.onShutdown(flushed);
    }

    public static DDAgentWriter.DDAgentWriterBuilder builder() {
        return new DDAgentWriter.DDAgentWriterBuilder();
    }

    public static class DDAgentWriterBuilder {
        private DDAgentApi agentApi;
        private Prioritization prioritization;
        String agentHost = "localhost";
        int traceAgentPort = 8126;
        String unixDomainSocket;
        long timeoutMillis;
        int traceBufferSize;
        HealthMetrics healthMetrics;
        int flushFrequencySeconds;
        Monitoring monitoring;
        boolean traceAgentV05Enabled;

        DDAgentWriterBuilder() {
            this.unixDomainSocket = ConfigDefaults.DEFAULT_AGENT_UNIX_DOMAIN_SOCKET;
            this.timeoutMillis = TimeUnit.SECONDS.toMillis(10L);
            this.traceBufferSize = 1024;
            this.healthMetrics = new HealthMetrics(new NoOpStatsDClient());
            this.flushFrequencySeconds = 1;
            this.monitoring = Monitoring.DISABLED;
            this.traceAgentV05Enabled = Config.get().isTraceAgentV05Enabled();
        }

        public DDAgentWriterBuilder agentApi(DDAgentApi agentApi) {
            this.agentApi = agentApi;
            return this;
        }

        public DDAgentWriterBuilder agentHost(String agentHost) {
            this.agentHost = agentHost;
            return this;
        }

        public DDAgentWriterBuilder traceAgentPort(int traceAgentPort) {
            this.traceAgentPort = traceAgentPort;
            return this;
        }

        public DDAgentWriterBuilder unixDomainSocket(String unixDomainSocket) {
            this.unixDomainSocket = unixDomainSocket;
            return this;
        }

        public DDAgentWriterBuilder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public DDAgentWriterBuilder traceBufferSize(int traceBufferSize) {
            this.traceBufferSize = traceBufferSize;
            return this;
        }

        public DDAgentWriterBuilder healthMetrics(HealthMetrics healthMetrics) {
            this.healthMetrics = healthMetrics;
            return this;
        }

        public DDAgentWriterBuilder flushFrequencySeconds(int flushFrequencySeconds) {
            this.flushFrequencySeconds = flushFrequencySeconds;
            return this;
        }

        public DDAgentWriterBuilder prioritization(Prioritization prioritization) {
            this.prioritization = prioritization;
            return this;
        }

        public DDAgentWriterBuilder monitoring(Monitoring monitoring) {
            this.monitoring = monitoring;
            return this;
        }

        public DDAgentWriterBuilder traceAgentV05Enabled(boolean traceAgentV05Enabled) {
            this.traceAgentV05Enabled = traceAgentV05Enabled;
            return this;
        }

        public DDAgentWriter build() {
            return new DDAgentWriter(this.agentApi, this.agentHost, this.traceAgentPort, this.unixDomainSocket, this.timeoutMillis, this.traceBufferSize, this.healthMetrics, this.flushFrequencySeconds, this.prioritization, this.monitoring, this.traceAgentV05Enabled);
        }

        public String toString() {
            return "DDAgentWriter.DDAgentWriterBuilder(agentApi=" + this.agentApi + ", agentHost=" + this.agentHost + ", traceAgentPort=" + this.traceAgentPort + ", unixDomainSocket=" + this.unixDomainSocket + ", timeoutMillis=" + this.timeoutMillis + ", traceBufferSize=" + this.traceBufferSize + ", healthMetrics=" + this.healthMetrics + ", flushFrequencySeconds=" + this.flushFrequencySeconds + ", prioritization=" + this.prioritization + ", monitoring=" + this.monitoring + ", traceAgentV05Enabled=" + this.traceAgentV05Enabled + ")";
        }
    }
}
