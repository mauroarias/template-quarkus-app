package datadog.lib.datadog.trace.core.monitor;

import com.timgroup.statsd.StatsDClient;
import datadog.lib.datadog.trace.core.util.SystemAccess;

public class CPUTimer extends Timer {
    private final StatsDClient statsd;
    private final String name;
    private final String[] tags = getTags();
    private long start;
    private long cpuTime = 0L;

    CPUTimer(String name, StatsDClient statsd, long flushAfterNanos) {
        super(name, getTags(), statsd, flushAfterNanos);
        this.name = name + ".cpu";
        this.statsd = statsd;
    }

    public Recording start() {
        super.start();
        this.start = SystemAccess.getCurrentThreadCpuTime();
        return this;
    }

    public void reset() {
        long cpuNanos = SystemAccess.getCurrentThreadCpuTime();
        if (this.start > 0L) {
            this.cpuTime += cpuNanos - this.start;
        }

        this.start = cpuNanos;
        super.reset();
    }

    public void stop() {
        if (this.start > 0L) {
            this.cpuTime += SystemAccess.getCurrentThreadCpuTime() - this.start;
        }

        super.stop();
    }

    public void flush() {
        super.flush();
        this.statsd.gauge(this.name, this.cpuTime, this.tags);
        this.cpuTime = 0L;
    }

    private static String[] getTags() {
        return new String[]{"thread:" + Thread.currentThread().getName()};
    }
}
