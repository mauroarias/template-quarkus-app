package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope.Continuation;

public interface AgentTrace {
    void registerContinuation(Continuation var1);

    void cancelContinuation(Continuation var1);
}
