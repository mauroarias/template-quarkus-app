package datadog.lib.datadog.trace.core.jfr;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;

public interface DDScopeEventFactory {
    DDScopeEvent create(Context var1);
}
