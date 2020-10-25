package datadog.lib.datadog.trace.core.util;

import datadog.lib.datadog.trace.api.time.SystemTimeSource;
import datadog.lib.datadog.trace.api.time.TimeSource;
import datadog.lib.datadog.trace.util.MathUtils;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleRateLimiter {
    private static final long REFILL_INTERVAL;
    private final long capacity;
    private final AtomicLong tokens;
    private final AtomicLong lastRefillTime;
    private final TimeSource timeSource;

    public SimpleRateLimiter(long rate) {
        this(rate, SystemTimeSource.INSTANCE);
    }

    protected SimpleRateLimiter(long rate, TimeSource timeSource) {
        this.timeSource = timeSource;
        this.capacity = Math.max(1L, rate);
        this.tokens = new AtomicLong(this.capacity);
        this.lastRefillTime = new AtomicLong(timeSource.getNanoTime());
    }

    public boolean tryAcquire() {
        long now = this.timeSource.getNanoTime();
        long localRefill = this.lastRefillTime.get();
        long timeElapsedSinceLastRefill = now - localRefill;
        if (timeElapsedSinceLastRefill > REFILL_INTERVAL) {
            if (this.lastRefillTime.compareAndSet(localRefill, now)) {
                this.tokens.set(this.capacity);
            }

            return this.tryAcquire();
        } else {
            return MathUtils.boundedDecrement(this.tokens, 0L);
        }
    }

    static {
        REFILL_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
    }
}
