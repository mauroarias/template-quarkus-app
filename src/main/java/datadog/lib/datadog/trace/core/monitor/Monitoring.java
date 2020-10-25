package datadog.lib.datadog.trace.core.monitor;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.util.concurrent.TimeUnit;

public final class Monitoring {
    public static final Monitoring DISABLED = new Monitoring();
    private final StatsDClient statsd;
    private final long flushAfterNanos;
    private final boolean enabled;

    public Monitoring(StatsDClient statsd, long flushInterval, TimeUnit flushUnit) {
        this.statsd = statsd;
        this.flushAfterNanos = flushUnit.toNanos(flushInterval);
        this.enabled = true;
    }

    private Monitoring() {
        this.statsd = new NoOpStatsDClient();
        this.flushAfterNanos = 0L;
        this.enabled = false;
    }

    public Recording newTimer(String name) {
        return (Recording)(!this.enabled ? NoOpRecording.NO_OP : new Timer(name, this.statsd, this.flushAfterNanos));
    }

    public Recording newTimer(String name, String... tags) {
        return (Recording)(!this.enabled ? NoOpRecording.NO_OP : new Timer(name, tags, this.statsd, this.flushAfterNanos));
    }

    public Recording newThreadLocalTimer(final String name) {
        return (Recording)(!this.enabled ? NoOpRecording.NO_OP : new ThreadLocalRecording(new ThreadLocal<Recording>() {
            protected Recording initialValue() {
                return Monitoring.this.newTimer(name, "thread:" + Thread.currentThread().getName());
            }
        }));
    }

    public Recording newCPUTimer(String name) {
        return (Recording)(!this.enabled ? NoOpRecording.NO_OP : new CPUTimer(name, this.statsd, this.flushAfterNanos));
    }

    public Counter newCounter(String name) {
        return (Counter)(!this.enabled ? NoOpCounter.NO_OP : new StatsDCounter(name, this.statsd));
    }
}
