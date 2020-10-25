package datadog.lib.datadog.trace.core.monitor;

public class ThreadLocalRecording extends Recording {
    private final ThreadLocal<Recording> tls;

    public ThreadLocalRecording(ThreadLocal<Recording> tls) {
        this.tls = tls;
    }

    public Recording start() {
        return ((Recording)this.tls.get()).start();
    }

    public void reset() {
        ((Recording)this.tls.get()).reset();
    }

    public void stop() {
        ((Recording)this.tls.get()).stop();
    }

    public void flush() {
        ((Recording)this.tls.get()).flush();
    }
}
