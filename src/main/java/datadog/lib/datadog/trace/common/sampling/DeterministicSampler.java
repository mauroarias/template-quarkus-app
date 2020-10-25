package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.CoreTracer;
import datadog.lib.datadog.trace.core.DDSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class DeterministicSampler implements RateSampler {
    private static final Logger log = LoggerFactory.getLogger(DeterministicSampler.class);
    private static final long KNUTH_FACTOR = 1111111111111111111L;
    private final long cutoff;
    private final double rate;

    public DeterministicSampler(double rate) {
        this.rate = rate;
        this.cutoff = (new BigDecimal(rate)).multiply(new BigDecimal(CoreTracer.TRACE_ID_MAX)).toBigInteger().longValue() + -9223372036854775808L;
        log.debug("Initializing the RateSampler, sampleRate: {} %", rate * 100.0D);
    }

    public boolean sample(DDSpan span) {
        boolean sampled = false;
        if (this.rate >= 1.0D) {
            sampled = true;
        } else if (this.rate > 0.0D) {
            long mod = span.getTraceId().toLong() * 1111111111111111111L;
            if (mod + -9223372036854775808L < this.cutoff) {
                sampled = true;
            }
        }

        log.debug("{} - Span is sampled: {}", span, sampled);
        return sampled;
    }

    public double getSampleRate() {
        return this.rate;
    }
}
