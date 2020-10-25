package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.util.SimpleRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RuleBasedSampler implements Sampler, PrioritySampler {
    private static final Logger log = LoggerFactory.getLogger(RuleBasedSampler.class);
    private final List<SamplingRule> samplingRules;
    private final PrioritySampler fallbackSampler;
    private final SimpleRateLimiter rateLimiter;
    private final long rateLimit;
    public static final String SAMPLING_RULE_RATE = "_dd.rule_psr";
    public static final String SAMPLING_LIMIT_RATE = "_dd.limit_psr";

    public RuleBasedSampler(List<SamplingRule> samplingRules, long rateLimit, PrioritySampler fallbackSampler) {
        this.samplingRules = samplingRules;
        this.fallbackSampler = fallbackSampler;
        this.rateLimiter = new SimpleRateLimiter(rateLimit);
        this.rateLimit = rateLimit;
    }

    public static RuleBasedSampler build(Map<String, String> serviceRules, Map<String, String> operationRules, Double defaultRate, long rateLimit) {
        List<SamplingRule> samplingRules = new ArrayList();
        Iterator var6;
        Entry entry;
        double rateForEntry;
        if (serviceRules != null) {
            var6 = serviceRules.entrySet().iterator();

            while(var6.hasNext()) {
                entry = (Entry)var6.next();

                try {
                    rateForEntry = Double.parseDouble((String)entry.getValue());
                    SamplingRule samplingRule = new SamplingRule.ServiceSamplingRule((String)entry.getKey(), new DeterministicSampler(rateForEntry));
                    samplingRules.add(samplingRule);
                } catch (NumberFormatException var12) {
                    log.error("Unable to parse rate for service: {}", entry, var12);
                }
            }
        }

        if (operationRules != null) {
            var6 = operationRules.entrySet().iterator();

            while(var6.hasNext()) {
                entry = (Entry)var6.next();

                try {
                    rateForEntry = Double.parseDouble((String)entry.getValue());
                    SamplingRule samplingRule = new SamplingRule.OperationSamplingRule((String)entry.getKey(), new DeterministicSampler(rateForEntry));
                    samplingRules.add(samplingRule);
                } catch (NumberFormatException var11) {
                    log.error("Unable to parse rate for operation: {}", entry, var11);
                }
            }
        }

        if (defaultRate != null) {
            SamplingRule samplingRule = new SamplingRule.AlwaysMatchesSamplingRule(new DeterministicSampler(defaultRate));
            samplingRules.add(samplingRule);
        }

        return new RuleBasedSampler(samplingRules, rateLimit, new RateByServiceSampler());
    }

    public boolean sample(DDSpan span) {
        return true;
    }

    public void setSamplingPriority(DDSpan span) {
        SamplingRule matchedRule = null;
        Iterator var3 = this.samplingRules.iterator();

        while(var3.hasNext()) {
            SamplingRule samplingRule = (SamplingRule)var3.next();
            if (samplingRule.matches(span)) {
                matchedRule = samplingRule;
                break;
            }
        }

        if (matchedRule == null) {
            this.fallbackSampler.setSamplingPriority(span);
        } else {
            boolean usedRateLimiter = false;
            boolean priorityWasSet;
            if (matchedRule.sample(span)) {
                usedRateLimiter = true;
                if (this.rateLimiter.tryAcquire()) {
                    priorityWasSet = span.context().setSamplingPriority(1);
                } else {
                    priorityWasSet = span.context().setSamplingPriority(0);
                }
            } else {
                priorityWasSet = span.context().setSamplingPriority(0);
            }

            if (priorityWasSet) {
                span.context().setMetric("_dd.rule_psr", matchedRule.getSampler().getSampleRate());
                if (usedRateLimiter) {
                    span.context().setMetric("_dd.limit_psr", this.rateLimit);
                }
            }
        }

    }
}
