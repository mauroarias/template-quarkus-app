package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.core.DDSpanData;
import datadog.lib.datadog.trace.core.monitor.HealthMetrics;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import datadog.lib.datadog.trace.core.monitor.Recording;
import datadog.lib.datadog.trace.core.serialization.msgpack.ByteBufferConsumer;
import datadog.lib.datadog.trace.core.serialization.msgpack.Packer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadDispatcher implements ByteBufferConsumer {
    private static final Logger log = LoggerFactory.getLogger(PayloadDispatcher.class);
    private final AtomicInteger droppedCount = new AtomicInteger();
    private final DDAgentApi api;
    private final HealthMetrics healthMetrics;
    private final Monitoring monitoring;
    private Recording batchTimer;
    private TraceMapper traceMapper;
    private Packer packer;

    public PayloadDispatcher(DDAgentApi api, HealthMetrics healthMetrics, Monitoring monitoring) {
        this.api = api;
        this.healthMetrics = healthMetrics;
        this.monitoring = monitoring;
    }

    void flush() {
        if (null != this.packer) {
            this.packer.flush();
        }

    }

    public void onTraceDropped() {
        this.droppedCount.incrementAndGet();
    }

    void addTrace(List<? extends DDSpanData> trace) {
        this.selectTraceMapper();
        if (null != this.traceMapper) {
            this.packer.format(trace, this.traceMapper);
        } else {
            this.onTraceDropped();
            log.debug("dropping {} traces because no agent was detected", 1);
        }

    }

    private void selectTraceMapper() {
        if (null == this.traceMapper) {
            this.traceMapper = this.api.selectTraceMapper();
            if (null != this.traceMapper && null == this.packer) {
                this.batchTimer = this.monitoring.newTimer("tracer.trace.buffer.fill.time", new String[]{"endpoint:" + this.traceMapper.endpoint()});
                this.packer = new Packer(this, ByteBuffer.allocate(this.traceMapper.messageBufferSize()));
                this.batchTimer.start();
            }
        }

    }

    public void accept(int messageCount, ByteBuffer buffer) {
        if (messageCount > 0) {
            this.batchTimer.reset();
            int representativeCount = this.droppedCount.getAndSet(0) + messageCount;
            Payload payload = this.traceMapper.newPayload().withRepresentativeCount(representativeCount).withBody(messageCount, buffer);
            int sizeInBytes = payload.sizeInBytes();
            this.healthMetrics.onSerialize(sizeInBytes);
            DDAgentApi.Response response = this.api.sendSerializedTraces(payload);
            this.traceMapper.reset();
            if (response.success()) {
                if (log.isDebugEnabled()) {
                    log.debug("Successfully sent {} traces to the API", messageCount);
                }

                this.healthMetrics.onSend(representativeCount, sizeInBytes, response);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to send {} traces (representing {}) of size {} bytes to the API", new Object[]{messageCount, representativeCount, sizeInBytes});
                }

                this.healthMetrics.onFailedSend(representativeCount, sizeInBytes, response);
            }
        }

    }
}
