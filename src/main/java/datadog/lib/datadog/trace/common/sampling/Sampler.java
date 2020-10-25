package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.api.Config;
import datadog.lib.datadog.trace.core.DDSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public interface Sampler {
    boolean sample(DDSpan var1);

    public static final class Builder {
        private static final Logger log = LoggerFactory.getLogger(Builder.class);

        public static Sampler forConfig(Config config) {
            Object sampler;
            if (config != null) {
                Map<String, String> serviceRules = config.getTraceSamplingServiceRules();
                Map<String, String> operationRules = config.getTraceSamplingOperationRules();
                if ((serviceRules == null || serviceRules.isEmpty()) && (operationRules == null || operationRules.isEmpty()) && config.getTraceSampleRate() == null) {
                    if (config.isPrioritySamplingEnabled()) {
                        if ("keep".equalsIgnoreCase(config.getPrioritySamplingForce())) {
                            log.debug("Force Sampling Priority to: SAMPLER_KEEP.");
                            sampler = new ForcePrioritySampler(1);
                        } else if ("drop".equalsIgnoreCase(config.getPrioritySamplingForce())) {
                            log.debug("Force Sampling Priority to: SAMPLER_DROP.");
                            sampler = new ForcePrioritySampler(0);
                        } else {
                            sampler = new RateByServiceSampler();
                        }
                    } else {
                        sampler = new AllSampler();
                    }
                } else {
                    try {
                        sampler = RuleBasedSampler.build(serviceRules, operationRules, config.getTraceSampleRate(), (long)config.getTraceRateLimit());
                    } catch (IllegalArgumentException var5) {
                        log.error("Invalid sampler configuration. Using AllSampler", var5);
                        sampler = new AllSampler();
                    }
                }
            } else {
                sampler = new AllSampler();
            }

            return (Sampler)sampler;
        }

        public static Sampler forConfig(Properties config) {
            return forConfig(Config.get(config));
        }

        private Builder() {
        }
    }
}
