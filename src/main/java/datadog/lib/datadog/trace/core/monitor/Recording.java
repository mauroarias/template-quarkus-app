package datadog.lib.datadog.trace.core.monitor;

public abstract class Recording implements AutoCloseable {
    public Recording() {
    }

    public void close() {
        this.stop();
    }

    public abstract Recording start();

    public abstract void reset();

    public abstract void stop();

    public abstract void flush();
}
