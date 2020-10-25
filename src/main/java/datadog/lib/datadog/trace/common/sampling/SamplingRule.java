package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.DDSpan;

import java.util.regex.Pattern;

public abstract class SamplingRule {
    private final RateSampler sampler;

    public SamplingRule(RateSampler sampler) {
        this.sampler = sampler;
    }

    public abstract boolean matches(DDSpan var1);

    public boolean sample(DDSpan span) {
        return this.sampler.sample(span);
    }

    public RateSampler getSampler() {
        return this.sampler;
    }

    public static class OperationSamplingRule extends PatternMatchSamplingRule {
        public OperationSamplingRule(String regex, RateSampler sampler) {
            super(regex, sampler);
        }

        protected CharSequence getRelevantString(DDSpan span) {
            return span.getOperationName();
        }
    }

    public static class ServiceSamplingRule extends PatternMatchSamplingRule {
        public ServiceSamplingRule(String regex, RateSampler sampler) {
            super(regex, sampler);
        }

        protected String getRelevantString(DDSpan span) {
            return span.getServiceName();
        }
    }

    public abstract static class PatternMatchSamplingRule extends SamplingRule {
        private final Pattern pattern;

        public PatternMatchSamplingRule(String regex, RateSampler sampler) {
            super(sampler);
            this.pattern = Pattern.compile(regex);
        }

        public boolean matches(DDSpan span) {
            CharSequence relevantString = this.getRelevantString(span);
            return relevantString != null && this.pattern.matcher(relevantString).matches();
        }

        protected abstract CharSequence getRelevantString(DDSpan var1);
    }

    public static class AlwaysMatchesSamplingRule extends SamplingRule {
        public AlwaysMatchesSamplingRule(RateSampler sampler) {
            super(sampler);
        }

        public boolean matches(DDSpan span) {
            return true;
        }
    }
}
