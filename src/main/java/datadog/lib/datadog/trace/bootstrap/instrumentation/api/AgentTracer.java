package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import datadog.trace.api.DDId;
import datadog.trace.api.Tracer;
import datadog.trace.api.interceptor.MutableSpan;
import datadog.trace.api.interceptor.TraceInterceptor;
import datadog.trace.context.ScopeListener;
import datadog.trace.context.TraceScope;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AgentTracer {
    private static final AgentTracer.TracerAPI DEFAULT = new AgentTracer.NoopTracerAPI();
    private static final AtomicReference<AgentTracer.TracerAPI> provider;

    public static AgentSpan startSpan(CharSequence spanName) {
        return get().startSpan(spanName);
    }

    public static AgentSpan startSpan(CharSequence spanName, long startTimeMicros) {
        return get().startSpan(spanName, startTimeMicros);
    }

    public static AgentSpan startSpan(CharSequence spanName, AgentSpan.Context parent) {
        return get().startSpan(spanName, parent);
    }

    public static AgentSpan startSpan(CharSequence spanName, AgentSpan.Context parent, long startTimeMicros) {
        return get().startSpan(spanName, parent, startTimeMicros);
    }

    public static AgentScope activateSpan(AgentSpan span) {
        return get().activateSpan(span, ScopeSource.INSTRUMENTATION, false);
    }

    public static AgentScope activateSpan(AgentSpan span, boolean isAsyncPropagating) {
        return get().activateSpan(span, ScopeSource.INSTRUMENTATION, isAsyncPropagating);
    }

    public static AgentSpan activeSpan() {
        return get().activeSpan();
    }

    public static TraceScope activeScope() {
        return get().activeScope();
    }

    public static AgentPropagation propagate() {
        return get().propagate();
    }

    public static AgentSpan noopSpan() {
        return get().noopSpan();
    }

    public static boolean isRegistered() {
        return provider.get() != DEFAULT;
    }

    public static void registerIfAbsent(AgentTracer.TracerAPI trace) {
        if (trace != null && trace != DEFAULT) {
            provider.compareAndSet(DEFAULT, trace);
        }

    }

    public static AgentTracer.TracerAPI get() {
        return (AgentTracer.TracerAPI)provider.get();
    }

    private AgentTracer() {
    }

    static {
        provider = new AtomicReference(DEFAULT);
    }

    public static class NoopAgentTrace implements AgentTrace {
        public static final AgentTracer.NoopAgentTrace INSTANCE = new AgentTracer.NoopAgentTrace();

        public NoopAgentTrace() {
        }

        public void registerContinuation(AgentScope.Continuation continuation) {
        }

        public void cancelContinuation(AgentScope.Continuation continuation) {
        }
    }

    public static class NoopContext implements AgentSpan.Context {
        public static final AgentTracer.NoopContext INSTANCE = new AgentTracer.NoopContext();

        public NoopContext() {
        }

        public DDId getTraceId() {
            return DDId.ZERO;
        }

        public DDId getSpanId() {
            return DDId.ZERO;
        }

        public AgentTrace getTrace() {
            return AgentTracer.NoopAgentTrace.INSTANCE;
        }

        public Iterable<Map.Entry<String, String>> baggageItems() {
            return Collections.emptyList();
        }
    }

    static class NoopContinuation implements AgentScope.Continuation {
        static final AgentTracer.NoopContinuation INSTANCE = new AgentTracer.NoopContinuation();

        NoopContinuation() {
        }

        public TraceScope activate() {
            return AgentTracer.NoopAgentScope.INSTANCE;
        }

        public void cancel() {
        }
    }

    static class NoopAgentPropagation implements AgentPropagation {
        static final AgentTracer.NoopAgentPropagation INSTANCE = new AgentTracer.NoopAgentPropagation();

        NoopAgentPropagation() {
        }

        public AgentScope.Continuation capture() {
            return AgentTracer.NoopContinuation.INSTANCE;
        }

        public <C> void inject(AgentSpan span, C carrier, Setter<C> setter) {
        }

        public <C> void inject(AgentSpan.Context context, C carrier, Setter<C> setter) {
        }

        public <C> AgentSpan.Context extract(C carrier, ContextVisitor<C> getter) {
            return AgentTracer.NoopContext.INSTANCE;
        }
    }

    public static class NoopAgentScope implements AgentScope, TraceScope {
        public static final AgentTracer.NoopAgentScope INSTANCE = new AgentTracer.NoopAgentScope();

        public NoopAgentScope() {
        }

        public AgentSpan span() {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public void setAsyncPropagation(boolean value) {
        }

        public AgentScope.Continuation capture() {
            return AgentTracer.NoopContinuation.INSTANCE;
        }

        public AgentScope.Continuation captureConcurrent() {
            return AgentTracer.NoopContinuation.INSTANCE;
        }

        public void close() {
        }

        public boolean isAsyncPropagating() {
            return false;
        }
    }

    public static class NoopAgentSpan implements AgentSpan {
        public static final AgentTracer.NoopAgentSpan INSTANCE = new AgentTracer.NoopAgentSpan();

        public NoopAgentSpan() {
        }

        public DDId getTraceId() {
            return DDId.ZERO;
        }

        public AgentSpan setTag(String key, boolean value) {
            return this;
        }

        public MutableSpan setTag(String tag, Number value) {
            return this;
        }

        public Boolean isError() {
            return false;
        }

        public AgentSpan setTag(String key, int value) {
            return this;
        }

        public AgentSpan setTag(String key, long value) {
            return this;
        }

        public AgentSpan setTag(String key, double value) {
            return this;
        }

        public AgentSpan setTag(String key, Object value) {
            return this;
        }

        public AgentSpan setMetric(CharSequence key, int value) {
            return this;
        }

        public AgentSpan setMetric(CharSequence key, long value) {
            return this;
        }

        public AgentSpan setMetric(CharSequence key, double value) {
            return this;
        }

        public Object getTag(String key) {
            return null;
        }

        public long getStartTime() {
            return 0L;
        }

        public long getDurationNano() {
            return 0L;
        }

        public String getOperationName() {
            return null;
        }

        public MutableSpan setOperationName(CharSequence serviceName) {
            return this;
        }

        public String getServiceName() {
            return null;
        }

        public MutableSpan setServiceName(String serviceName) {
            return this;
        }

        public CharSequence getResourceName() {
            return null;
        }

        public MutableSpan setResourceName(CharSequence resourceName) {
            return this;
        }

        public Integer getSamplingPriority() {
            return -2147483648;
        }

        public MutableSpan setSamplingPriority(int newPriority) {
            return this;
        }

        public String getSpanType() {
            return null;
        }

        public AgentSpan setSpanType(CharSequence type) {
            return this;
        }

        public Map<String, Object> getTags() {
            return Collections.emptyMap();
        }

        public AgentSpan setTag(String key, String value) {
            return this;
        }

        public AgentSpan setTag(String key, CharSequence value) {
            return this;
        }

        public AgentSpan setError(boolean error) {
            return this;
        }

        public MutableSpan getRootSpan() {
            return this;
        }

        public AgentSpan setErrorMessage(String errorMessage) {
            return this;
        }

        public AgentSpan addThrowable(Throwable throwable) {
            return this;
        }

        public AgentSpan getLocalRootSpan() {
            return this;
        }

        public boolean isSameTrace(AgentSpan otherSpan) {
            return otherSpan instanceof AgentTracer.NoopAgentSpan;
        }

        public AgentSpan.Context context() {
            return AgentTracer.NoopContext.INSTANCE;
        }

        public String getBaggageItem(String key) {
            return null;
        }

        public AgentSpan setBaggageItem(String key, String value) {
            return this;
        }

        public void finish() {
        }

        public void finish(long finishMicros) {
        }

        public String getSpanName() {
            return "";
        }

        public void setSpanName(CharSequence spanName) {
        }

        public boolean hasResourceName() {
            return false;
        }
    }

    static class NoopTracerAPI implements AgentTracer.TracerAPI {
        protected NoopTracerAPI() {
        }

        public AgentSpan startSpan(CharSequence spanName) {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public AgentSpan startSpan(CharSequence spanName, long startTimeMicros) {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public AgentSpan startSpan(CharSequence spanName, AgentSpan.Context parent) {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public AgentSpan startSpan(CharSequence spanName, AgentSpan.Context parent, long startTimeMicros) {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public AgentScope activateSpan(AgentSpan span, ScopeSource source) {
            return AgentTracer.NoopAgentScope.INSTANCE;
        }

        public AgentScope activateSpan(AgentSpan span, ScopeSource source, boolean isAsyncPropagating) {
            return AgentTracer.NoopAgentScope.INSTANCE;
        }

        public AgentSpan activeSpan() {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public TraceScope activeScope() {
            return null;
        }

        public AgentPropagation propagate() {
            return AgentTracer.NoopAgentPropagation.INSTANCE;
        }

        public AgentSpan noopSpan() {
            return AgentTracer.NoopAgentSpan.INSTANCE;
        }

        public AgentTracer.SpanBuilder buildSpan(CharSequence spanName) {
            return null;
        }

        public void close() {
        }

        public void flush() {
        }

        public String getTraceId() {
            return null;
        }

        public String getSpanId() {
            return null;
        }

        public boolean addTraceInterceptor(TraceInterceptor traceInterceptor) {
            return false;
        }

        public void addScopeListener(ScopeListener listener) {
        }

        public TraceScope.Continuation capture() {
            return null;
        }

        public <C> void inject(AgentSpan span, C carrier, Setter<C> setter) {
        }

        public <C> void inject(AgentSpan.Context context, C carrier, Setter<C> setter) {
        }

        public <C> AgentSpan.Context extract(C carrier, ContextVisitor<C> getter) {
            return null;
        }
    }

    public interface SpanBuilder {
        AgentSpan start();

        AgentTracer.SpanBuilder asChildOf(AgentSpan.Context var1);

        AgentTracer.SpanBuilder ignoreActiveSpan();

        AgentTracer.SpanBuilder withTag(String var1, String var2);

        AgentTracer.SpanBuilder withTag(String var1, boolean var2);

        AgentTracer.SpanBuilder withTag(String var1, Number var2);

        AgentTracer.SpanBuilder withTag(String var1, Object var2);

        AgentTracer.SpanBuilder withStartTimestamp(long var1);

        AgentTracer.SpanBuilder withServiceName(String var1);

        AgentTracer.SpanBuilder withResourceName(String var1);

        AgentTracer.SpanBuilder withErrorFlag();

        AgentTracer.SpanBuilder withSpanType(CharSequence var1);
    }

    public interface TracerAPI extends Tracer, AgentPropagation {
        AgentSpan startSpan(CharSequence var1);

        AgentSpan startSpan(CharSequence var1, long var2);

        AgentSpan startSpan(CharSequence var1, AgentSpan.Context var2);

        AgentSpan startSpan(CharSequence var1, AgentSpan.Context var2, long var3);

        AgentScope activateSpan(AgentSpan var1, ScopeSource var2);

        AgentScope activateSpan(AgentSpan var1, ScopeSource var2, boolean var3);

        AgentSpan activeSpan();

        TraceScope activeScope();

        AgentPropagation propagate();

        AgentSpan noopSpan();

        AgentTracer.SpanBuilder buildSpan(CharSequence var1);

        void close();

        void flush();
    }
}
