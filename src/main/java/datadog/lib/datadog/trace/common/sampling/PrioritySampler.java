package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.DDSpan;

public interface PrioritySampler {
    void setSamplingPriority(DDSpan var1);
}
