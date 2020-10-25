//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.opentracing.CustomScopeManagerWrapper.CustomScopeManagerScope;
import datadog.lib.datadog.opentracing.OTScopeManager.OTScope;
import datadog.lib.datadog.opentracing.OTScopeManager.OTTraceScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.NoopAgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.NoopContext;
import datadog.trace.context.TraceScope;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

class TypeConverter {
    private final LogHandler logHandler;

    public TypeConverter(LogHandler logHandler) {
        this.logHandler = logHandler;
    }

    public AgentSpan toAgentSpan(Span span) {
        if (span == null) {
            return null;
        } else {
            return (AgentSpan)(span instanceof OTSpan ? ((OTSpan)span).getDelegate() : NoopAgentSpan.INSTANCE);
        }
    }

    public Span toSpan(AgentSpan agentSpan) {
        return agentSpan == null ? null : new OTSpan(agentSpan, this, this.logHandler);
    }

    public Scope toScope(Object scope, boolean finishSpanOnClose) {
        if (scope == null) {
            return null;
        } else if (scope instanceof CustomScopeManagerScope) {
            return ((CustomScopeManagerScope)scope).getDelegate();
        } else {
            return (Scope)(scope instanceof TraceScope ? new OTTraceScope((TraceScope)scope, finishSpanOnClose, this) : new OTScope((AgentScope)scope, finishSpanOnClose, this));
        }
    }

    public SpanContext toSpanContext(Context context) {
        return context == null ? null : new OTSpanContext(context);
    }

    public Context toContext(SpanContext spanContext) {
        if (spanContext == null) {
            return null;
        } else {
            return (Context)(spanContext instanceof OTSpanContext ? ((OTSpanContext)spanContext).getDelegate() : NoopContext.INSTANCE);
        }
    }
}
