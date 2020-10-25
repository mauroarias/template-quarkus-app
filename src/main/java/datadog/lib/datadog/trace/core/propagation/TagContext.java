package datadog.lib.datadog.trace.core.propagation;

import datadog.trace.api.DDId;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTrace;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.NoopAgentTrace;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public class TagContext implements Context {
    private final String origin;
    private final Map<String, String> tags;

    public TagContext(String origin, Map<String, String> tags) {
        this.origin = origin;
        this.tags = tags;
    }

    public String getOrigin() {
        return this.origin;
    }

    public Map<String, String> getTags() {
        return this.tags;
    }

    public Iterable<Entry<String, String>> baggageItems() {
        return Collections.emptyList();
    }

    public DDId getTraceId() {
        return DDId.ZERO;
    }

    public DDId getSpanId() {
        return DDId.ZERO;
    }

    public AgentTrace getTrace() {
        return NoopAgentTrace.INSTANCE;
    }
}
