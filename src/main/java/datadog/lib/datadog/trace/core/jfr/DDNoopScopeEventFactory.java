package datadog.lib.datadog.trace.core.jfr;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;

public final class DDNoopScopeEventFactory implements DDScopeEventFactory {
    public DDNoopScopeEventFactory() {
    }

    public DDScopeEvent create(Context context) {
        return DDNoopScopeEvent.INSTANCE;
    }
}
