package datadog.lib.datadog.trace.core.monitor;

public interface Counter {
    void increment(int var1);

    void incrementErrorCount(String var1, int var2);
}
