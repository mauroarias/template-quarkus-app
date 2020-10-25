package datadog.lib.datadog.trace.core.jfr;

public final class DDNoopScopeEvent implements DDScopeEvent {
    public static final DDNoopScopeEvent INSTANCE = new DDNoopScopeEvent();

    public DDNoopScopeEvent() {
    }

    public void start() {
    }

    public void finish() {
    }
}
