package datadog.lib.datadog.trace.core.monitor;

public class NoOpRecording extends Recording {
    public static final Recording NO_OP = new NoOpRecording();

    public NoOpRecording() {
    }

    public Recording start() {
        return this;
    }

    public void reset() {
    }

    public void stop() {
    }

    public void flush() {
    }
}
