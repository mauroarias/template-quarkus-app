package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScopeManager;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.ScopeSource;
import datadog.trace.context.TraceScope;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

import java.util.Objects;

class CustomScopeManagerWrapper implements AgentScopeManager {
    private final ScopeManager delegate;
    private final TypeConverter converter;

    CustomScopeManagerWrapper(ScopeManager scopeManager, TypeConverter converter) {
        this.delegate = scopeManager;
        this.converter = converter;
    }

    public AgentScope activate(AgentSpan agentSpan, ScopeSource source) {
        Span span = this.converter.toSpan(agentSpan);
        Scope scope = this.delegate.activate(span);
        return new CustomScopeManagerWrapper.CustomScopeManagerScope(scope);
    }

    public AgentScope activate(AgentSpan agentSpan, ScopeSource source, boolean isAsyncPropagating) {
        Span span = this.converter.toSpan(agentSpan);
        Scope scope = this.delegate.activate(span);
        AgentScope agentScope = new CustomScopeManagerWrapper.CustomScopeManagerScope(scope);
        agentScope.setAsyncPropagation(isAsyncPropagating);
        return agentScope;
    }

    public TraceScope active() {
        return new CustomScopeManagerWrapper.CustomScopeManagerScope(this.delegate.active());
    }

    public AgentSpan activeSpan() {
        return this.converter.toAgentSpan(this.delegate.activeSpan());
    }

    class CustomScopeManagerScope implements AgentScope, TraceScope {
        private final Scope delegate;
        private final boolean traceScope;

        private CustomScopeManagerScope(Scope delegate) {
            this.delegate = delegate;
            this.traceScope = delegate instanceof TraceScope;
        }

        public AgentSpan span() {
            return CustomScopeManagerWrapper.this.converter.toAgentSpan(this.delegate.span());
        }

        public void setAsyncPropagation(boolean value) {
            if (this.traceScope) {
                ((TraceScope)this.delegate).setAsyncPropagation(value);
            }

        }

        public boolean isAsyncPropagating() {
            return this.traceScope && ((TraceScope)this.delegate).isAsyncPropagating();
        }

        public TraceScope.Continuation capture() {
            return this.traceScope ? ((TraceScope)this.delegate).capture() : null;
        }

        public TraceScope.Continuation captureConcurrent() {
            return this.traceScope ? ((TraceScope)this.delegate).captureConcurrent() : null;
        }

        public void close() {
            this.delegate.close();
        }

        Scope getDelegate() {
            return this.delegate;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                CustomScopeManagerWrapper.CustomScopeManagerScope that = (CustomScopeManagerWrapper.CustomScopeManagerScope)o;
                return this.delegate.equals(that.delegate);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.delegate});
        }
    }
}
