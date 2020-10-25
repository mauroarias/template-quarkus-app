package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.DDSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForcePrioritySampler implements Sampler, PrioritySampler {
    private static final Logger log = LoggerFactory.getLogger(ForcePrioritySampler.class);
    private final int prioritySampling;

    public ForcePrioritySampler(int prioritySampling) {
        this.prioritySampling = prioritySampling;
    }

    public boolean sample(DDSpan span) {
        return true;
    }

    public void setSamplingPriority(DDSpan span) {
        span.context().setSamplingPriority(this.prioritySampling);
    }
}
