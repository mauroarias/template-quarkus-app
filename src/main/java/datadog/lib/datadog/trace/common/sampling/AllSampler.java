package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.DDSpan;

public class AllSampler extends AbstractSampler {
    public AllSampler() {
    }

    public boolean doSample(DDSpan span) {
        return true;
    }

    public String toString() {
        return "AllSampler { sample=true }";
    }
}
