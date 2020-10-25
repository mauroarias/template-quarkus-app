package datadog.lib.datadog.trace.core.monitor;

import com.timgroup.statsd.StatsDClient;

public final class StatsDCounter implements Counter {
    private final String name;
    private final String[] tags;
    private final StatsDClient statsd;

    StatsDCounter(String name, StatsDClient statsd) {
        this.name = name;
        this.tags = new String[0];
        this.statsd = statsd;
    }

    public void increment(int delta) {
        this.statsd.count(this.name, (long)delta, this.tags);
    }

    public void incrementErrorCount(String cause, int delta) {
        this.statsd.count(this.name, (long)delta, Utils.mergeTags(this.tags, new String[]{"cause:" + cause.replace(' ', '_')}));
    }
}
