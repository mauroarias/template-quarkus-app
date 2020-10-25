package datadog.lib.datadog.trace.core.jfr;

public interface DDScopeEvent {
    void start();

    void finish();
}
