package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.common.writer.ddagent.DDAgentResponseListener;
import datadog.lib.datadog.trace.core.DDSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RateByServiceSampler implements Sampler, PrioritySampler, DDAgentResponseListener {
    private static final Logger log = LoggerFactory.getLogger(RateByServiceSampler.class);
    public static final String SAMPLING_AGENT_RATE = "_dd.agent_psr";
    private static final String DEFAULT_KEY = "service:,env:";
    private static final double DEFAULT_RATE = 1.0D;
    private volatile Map<String, RateSampler> serviceRates = Collections.unmodifiableMap(Collections.singletonMap("service:,env:", this.createRateSampler(1.0D)));

    public RateByServiceSampler() {
    }

    public boolean sample(DDSpan span) {
        return true;
    }

    public void setSamplingPriority(DDSpan span) {
        String serviceName = span.getServiceName();
        String env = getSpanEnv(span);
        String key = "service:" + serviceName + ",env:" + env;
        Map<String, RateSampler> rates = this.serviceRates;
        RateSampler sampler = (RateSampler)this.serviceRates.get(key);
        if (sampler == null) {
            sampler = (RateSampler)rates.get("service:,env:");
        }

        boolean priorityWasSet;
        if (sampler.sample(span)) {
            priorityWasSet = span.context().setSamplingPriority(1);
        } else {
            priorityWasSet = span.context().setSamplingPriority(0);
        }

        if (priorityWasSet) {
            span.context().setMetric("_dd.agent_psr", sampler.getSampleRate());
        }

    }

    private static String getSpanEnv(DDSpan span) {
        Object env = span.getTag("env");
        return null == env ? "" : String.valueOf(env);
    }

    public void onResponse(String endpoint, Map<String, Map<String, Number>> responseJson) {
        Map<String, Number> newServiceRates = (Map)responseJson.get("rate_by_service");
        if (null != newServiceRates) {
            log.debug("Update service sampler rates: {} -> {}", endpoint, responseJson);
            Map<String, RateSampler> updatedServiceRates = new HashMap();
            Iterator var5 = newServiceRates.entrySet().iterator();

            while(var5.hasNext()) {
                Entry<String, Number> entry = (Entry)var5.next();
                if (entry.getValue() != null) {
                    updatedServiceRates.put(entry.getKey(), this.createRateSampler(((Number)entry.getValue()).doubleValue()));
                }
            }

            if (!updatedServiceRates.containsKey("service:,env:")) {
                updatedServiceRates.put("service:,env:", this.createRateSampler(1.0D));
            }

            this.serviceRates = Collections.unmodifiableMap(updatedServiceRates);
        }

    }

    private RateSampler createRateSampler(double sampleRate) {
        double sanitizedRate;
        if (sampleRate < 0.0D) {
            log.error("SampleRate is negative or null, disabling the sampler");
            sanitizedRate = 1.0D;
        } else if (sampleRate > 1.0D) {
            sanitizedRate = 1.0D;
        } else {
            sanitizedRate = sampleRate;
        }

        return new DeterministicSampler(sanitizedRate);
    }
}
