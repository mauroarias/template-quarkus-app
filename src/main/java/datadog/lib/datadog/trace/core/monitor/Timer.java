package datadog.lib.datadog.trace.core.monitor;

import com.timgroup.statsd.StatsDClient;
import org.HdrHistogram.PackedHistogram;

import java.util.concurrent.TimeUnit;

public class Timer extends Recording {
    private static final long THIRTY_SECONDS_AS_NANOS;
    private static final String[] MEAN;
    private static final String[] P_50;
    private static final String[] P_99;
    private static final String[] MAX;
    private final String name;
    private final StatsDClient statsd;
    private final PackedHistogram histogram;
    private final long flushAfterNanos;
    private final String[] meanTags;
    private final String[] p50Tags;
    private final String[] p99Tags;
    private final String[] maxTags;
    private long start;
    private long lastFlush;

    Timer(String name, String[] tags, StatsDClient statsd, long flushAfterNanos) {
        this.lastFlush = 0L;
        this.name = name;
        this.statsd = statsd;
        this.flushAfterNanos = flushAfterNanos;
        this.histogram = new PackedHistogram(THIRTY_SECONDS_AS_NANOS, 3);
        this.meanTags = Utils.mergeTags(MEAN, tags);
        this.p50Tags = Utils.mergeTags(P_50, tags);
        this.p99Tags = Utils.mergeTags(P_99, tags);
        this.maxTags = Utils.mergeTags(MAX, tags);
    }

    Timer(String name, StatsDClient statsd, long flushAfterNanos) {
        this(name, (String[])null, statsd, flushAfterNanos);
    }

    public Recording start() {
        this.start = System.nanoTime();
        return this;
    }

    public void reset() {
        long now = System.nanoTime();
        this.record(now);
        this.start = now;
    }

    public void stop() {
        this.record(System.nanoTime());
    }

    private void record(long now) {
        this.histogram.recordValue(Math.min(now - this.start, THIRTY_SECONDS_AS_NANOS));
        if (now - this.lastFlush > this.flushAfterNanos) {
            this.lastFlush = now;
            this.flush();
        }

    }

    public void flush() {
        this.statsd.gauge(this.name, (long)this.histogram.getMean(), this.meanTags);
        this.statsd.gauge(this.name, this.histogram.getValueAtPercentile(50.0D), this.p50Tags);
        this.statsd.gauge(this.name, this.histogram.getValueAtPercentile(99.0D), this.p99Tags);
        this.statsd.gauge(this.name, this.histogram.getMaxValue(), this.maxTags);
    }

    static {
        THIRTY_SECONDS_AS_NANOS = TimeUnit.SECONDS.toNanos(30L);
        MEAN = new String[]{"stat:avg"};
        P_50 = new String[]{"stat:p50"};
        P_99 = new String[]{"stat:p99"};
        MAX = new String[]{"stat:max"};
    }
}
