package datadog.lib.datadog.trace.api.time;

public class SystemTimeSource implements TimeSource {
    public static final TimeSource INSTANCE = new SystemTimeSource();

    public SystemTimeSource() {
    }

    public long getNanoTime() {
        return System.nanoTime();
    }
}
