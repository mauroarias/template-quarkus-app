package datadog.lib.datadog.trace.api;

import datadog.lib.datadog.trace.api.time.SystemTimeSource;
import datadog.lib.datadog.trace.api.time.TimeSource;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

public class RatelimitedLogger {
    private final Logger log;
    private final long delay;
    private final TimeSource timeSource;
    private final AtomicLong previousErrorLogNanos;

    public RatelimitedLogger(Logger log, long delay) {
        this(log, delay, SystemTimeSource.INSTANCE);
    }

    RatelimitedLogger(Logger log, long delay, TimeSource timeSource) {
        this.previousErrorLogNanos = new AtomicLong();
        this.log = log;
        this.delay = delay;
        this.timeSource = timeSource;
    }

    public boolean warn(String format, Object... arguments) {
        if (this.log.isDebugEnabled()) {
            this.log.warn(format, arguments);
            return true;
        } else {
            if (this.log.isWarnEnabled()) {
                long previous = this.previousErrorLogNanos.get();
                long now = this.timeSource.getNanoTime();
                if (now - previous >= this.delay && this.previousErrorLogNanos.compareAndSet(previous, now)) {
                    this.log.warn(format + " (Will not log errors for 5 minutes)", arguments);
                    return true;
                }
            }

            return false;
        }
    }
}
