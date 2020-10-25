package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import datadog.trace.context.TraceScope;

import java.io.Closeable;

public interface AgentScope extends TraceScope, Closeable {
    AgentSpan span();

    void setAsyncPropagation(boolean var1);

    void close();

    public interface Continuation extends TraceScope.Continuation {
    }
}
