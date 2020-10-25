package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import datadog.trace.context.TraceScope;

public interface AgentScopeManager {
    AgentScope activate(AgentSpan var1, ScopeSource var2);

    AgentScope activate(AgentSpan var1, ScopeSource var2, boolean var3);

    TraceScope active();

    AgentSpan activeSpan();
}
