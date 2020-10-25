package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.TracerAPI;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.ScopeSource;
import datadog.trace.context.TraceScope;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

import java.util.Objects;

class OTScopeManager implements ScopeManager {
    private final TypeConverter converter;
    private final TracerAPI tracer;

    OTScopeManager(TracerAPI tracer, TypeConverter converter) {
        this.tracer = tracer;
        this.converter = converter;
    }

    public Scope activate(Span span) {
        return this.activate(span, false);
    }

    public Scope activate(Span span, boolean finishSpanOnClose) {
        AgentSpan agentSpan = this.converter.toAgentSpan(span);
        AgentScope agentScope = this.tracer.activateSpan(agentSpan, ScopeSource.MANUAL);
        return this.converter.toScope(agentScope, finishSpanOnClose);
    }

    /** @deprecated */
    @Deprecated
    public Scope active() {
        return this.converter.toScope(this.tracer.activeScope(), false);
    }

    public Span activeSpan() {
        return this.converter.toSpan(this.tracer.activeSpan());
    }

    static class OTTraceScope extends OTScope implements TraceScope {
        private final TraceScope delegate;

        OTTraceScope(TraceScope delegate, boolean finishSpanOnClose, TypeConverter converter) {
            super((AgentScope)delegate, finishSpanOnClose, converter);
            this.delegate = delegate;
        }

        public TraceScope.Continuation capture() {
            return this.delegate.capture();
        }

        public Continuation captureConcurrent() {
            return this.delegate.captureConcurrent();
        }

        public boolean isAsyncPropagating() {
            return this.delegate.isAsyncPropagating();
        }

        public void setAsyncPropagation(boolean value) {
            this.delegate.setAsyncPropagation(value);
        }
    }

    static class OTScope implements Scope {
        private final AgentScope delegate;
        private final boolean finishSpanOnClose;
        private final TypeConverter converter;

        OTScope(AgentScope delegate, boolean finishSpanOnClose, TypeConverter converter) {
            this.delegate = delegate;
            this.finishSpanOnClose = finishSpanOnClose;
            this.converter = converter;
        }

        public void close() {
            this.delegate.close();
            if (this.finishSpanOnClose) {
                this.delegate.span().finish();
            }

        }

        public Span span() {
            return this.converter.toSpan(this.delegate.span());
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof OTScope)) {
                return false;
            } else {
                OTScope otScope = (OTScope)o;
                return this.delegate.equals(otScope.delegate);
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.delegate});
        }
    }
}
