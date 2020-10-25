package datadog.lib.datadog.trace.api.time;

public class ControllableTimeSource implements TimeSource {
    private long currentTime = 0L;

    public ControllableTimeSource() {
    }

    public void advance(long nanosIncrement) {
        this.currentTime += nanosIncrement;
    }

    public void set(long nanos) {
        this.currentTime = nanos;
    }

    public long getNanoTime() {
        return this.currentTime;
    }
}
