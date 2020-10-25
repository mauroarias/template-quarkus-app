package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import datadog.trace.api.DDId;
import datadog.trace.api.interceptor.MutableSpan;

import java.util.Map;

public interface AgentSpan extends MutableSpan {
    DDId getTraceId();

    AgentSpan setTag(String var1, boolean var2);

    AgentSpan setTag(String var1, int var2);

    AgentSpan setTag(String var1, long var2);

    AgentSpan setTag(String var1, double var2);

    AgentSpan setTag(String var1, String var2);

    AgentSpan setTag(String var1, CharSequence var2);

    AgentSpan setTag(String var1, Object var2);

    AgentSpan setMetric(CharSequence var1, int var2);

    AgentSpan setMetric(CharSequence var1, long var2);

    AgentSpan setMetric(CharSequence var1, double var2);

    AgentSpan setSpanType(CharSequence var1);

    Object getTag(String var1);

    AgentSpan setError(boolean var1);

    AgentSpan setErrorMessage(String var1);

    AgentSpan addThrowable(Throwable var1);

    AgentSpan getLocalRootSpan();

    boolean isSameTrace(AgentSpan var1);

    AgentSpan.Context context();

    String getBaggageItem(String var1);

    AgentSpan setBaggageItem(String var1, String var2);

    void finish();

    void finish(long var1);

    CharSequence getSpanName();

    void setSpanName(CharSequence var1);

    boolean hasResourceName();

    public interface Context {
        DDId getTraceId();

        DDId getSpanId();

        AgentTrace getTrace();

        Iterable<Map.Entry<String, String>> baggageItems();
    }
}
