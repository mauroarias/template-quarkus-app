package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.util.Map;

public interface LogHandler {
    void log(Map<String, ?> var1, AgentSpan var2);

    void log(long var1, Map<String, ?> var3, AgentSpan var4);

    void log(String var1, AgentSpan var2);

    void log(long var1, String var3, AgentSpan var4);
}
