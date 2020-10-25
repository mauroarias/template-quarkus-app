package datadog.lib.datadog.trace.common.sampling;

public interface RateSampler extends Sampler {
    double getSampleRate();
}
