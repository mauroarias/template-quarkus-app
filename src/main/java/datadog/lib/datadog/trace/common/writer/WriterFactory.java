package datadog.lib.datadog.trace.common.writer;

import com.timgroup.statsd.StatsDClient;
import datadog.lib.datadog.common.container.ServerlessInfo;
import datadog.lib.datadog.trace.api.Config;
import datadog.trace.api.ConfigDefaults;
import datadog.lib.datadog.trace.common.sampling.Sampler;
import datadog.lib.datadog.trace.common.writer.ddagent.DDAgentApi;
import datadog.lib.datadog.trace.common.writer.ddagent.DDAgentResponseListener;
import datadog.lib.datadog.trace.common.writer.ddagent.Prioritization;
import datadog.lib.datadog.trace.core.monitor.HealthMetrics;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class WriterFactory {
    private static final Logger log = LoggerFactory.getLogger(WriterFactory.class);

    public static Writer createWriter(Config config, Sampler sampler, StatsDClient statsDClient, Monitoring monitoring) {
        return createWriter(config, sampler, statsDClient, monitoring, config.getWriterType());
    }

    public static Writer createWriter(Config config, Sampler sampler, StatsDClient statsDClient, Monitoring monitoring, String configuredType) {
        if ("LoggingWriter".equals(configuredType)) {
            return new LoggingWriter();
        } else if ("PrintingWriter".equals(configuredType)) {
            return new PrintingWriter(System.out, true);
        } else if (configuredType.startsWith("TraceStructureWriter")) {
            return new TraceStructureWriter(configuredType.replace("TraceStructureWriter", ""));
        } else if (configuredType.startsWith("MultiWriter")) {
            return new MultiWriter(config, sampler, statsDClient, monitoring, configuredType);
        } else {
            if (!"DDAgentWriter".equals(configuredType)) {
                log.warn("Writer type not configured correctly: Type {} not recognized. Ignoring", configuredType);
            }

            if (config.isAgentConfiguredUsingDefault() && ServerlessInfo.get().isRunningInServerlessEnvironment()) {
                log.info("Detected serverless environment.  Using PrintingWriter");
                return new PrintingWriter(System.out, true);
            } else {
                String unixDomainSocket = config.getAgentUnixDomainSocket();
                if (unixDomainSocket != ConfigDefaults.DEFAULT_AGENT_UNIX_DOMAIN_SOCKET && isWindows()) {
                    log.warn("{} setting not supported on {}.  Reverting to the default.", "trace.agent.unix.domain.socket", System.getProperty("os.name"));
                    unixDomainSocket = ConfigDefaults.DEFAULT_AGENT_UNIX_DOMAIN_SOCKET;
                }

                DDAgentApi ddAgentApi = new DDAgentApi(config.getAgentUrl(), unixDomainSocket, TimeUnit.SECONDS.toMillis((long)config.getAgentTimeout()), Config.get().isTraceAgentV05Enabled(), monitoring);
                String prioritizationType = config.getPrioritizationType();
                Prioritization prioritization = null;
                if ("EnsureTrace".equals(prioritizationType)) {
                    prioritization = Prioritization.ENSURE_TRACE;
                    log.info("Using 'EnsureTrace' prioritization type. (Do not use this type if your application is running in production mode)");
                }

                DDAgentWriter ddAgentWriter = DDAgentWriter.builder().agentApi(ddAgentApi).prioritization(prioritization).healthMetrics(new HealthMetrics(statsDClient)).monitoring(monitoring).build();
                if (sampler instanceof DDAgentResponseListener) {
                    ddAgentWriter.addResponseListener((DDAgentResponseListener)sampler);
                }

                return ddAgentWriter;
            }
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    private WriterFactory() {
    }
}
