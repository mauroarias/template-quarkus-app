package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.trace.context.TraceScope.Continuation;

public interface AgentPropagation {
    Continuation capture();

    <C> void inject(AgentSpan var1, C var2, AgentPropagation.Setter<C> var3);

    <C> void inject(Context var1, C var2, AgentPropagation.Setter<C> var3);

    <C> Context extract(C var1, AgentPropagation.ContextVisitor<C> var2);

    public interface ContextVisitor<C> {
        void forEachKey(C var1, AgentPropagation.KeyClassifier var2);
    }

    public interface KeyClassifier {
        boolean accept(String var1, String var2);
    }

    public interface Setter<C> {
        void set(C var1, String var2, String var3);
    }
}
