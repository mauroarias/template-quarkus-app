package datadog.lib.datadog.trace.core.scopemanager;

import com.timgroup.statsd.StatsDClient;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScopeManager;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTrace;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.NoopAgentScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.ScopeSource;
import datadog.trace.context.ScopeListener;
import datadog.trace.context.TraceScope;
import datadog.lib.datadog.trace.core.jfr.DDScopeEvent;
import datadog.lib.datadog.trace.core.jfr.DDScopeEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ContinuableScopeManager implements AgentScopeManager {
    private static final Logger log = LoggerFactory.getLogger(ContinuableScopeManager.class);
    final ThreadLocal<ScopeStack> tlsScopeStack;
    private final DDScopeEventFactory scopeEventFactory;
    private final List<ScopeListener> scopeListeners;
    private final int depthLimit;
    private final StatsDClient statsDClient;
    private final boolean strictMode;
    private final boolean inheritAsyncPropagation;

    public ContinuableScopeManager(int depthLimit, DDScopeEventFactory scopeEventFactory, StatsDClient statsDClient, boolean strictMode, boolean inheritAsyncPropagation) {
        this(depthLimit, scopeEventFactory, statsDClient, strictMode, inheritAsyncPropagation, new CopyOnWriteArrayList());
    }

    private ContinuableScopeManager(int depthLimit, DDScopeEventFactory scopeEventFactory, StatsDClient statsDClient, boolean strictMode, boolean inheritAsyncPropagation, List<ScopeListener> scopeListeners) {
        this.tlsScopeStack = new ThreadLocal<ContinuableScopeManager.ScopeStack>() {
            protected final ContinuableScopeManager.ScopeStack initialValue() {
                return new ContinuableScopeManager.ScopeStack();
            }
        };
        this.scopeEventFactory = scopeEventFactory;
        this.depthLimit = depthLimit == 0 ? 2147483647 : depthLimit;
        this.statsDClient = statsDClient;
        this.strictMode = strictMode;
        this.inheritAsyncPropagation = inheritAsyncPropagation;
        this.scopeListeners = scopeListeners;
    }

    public AgentScope activate(AgentSpan span, ScopeSource source) {
        return this.activate(span, source, false, false);
    }

    public AgentScope activate(AgentSpan span, ScopeSource source, boolean isAsyncPropagating) {
        return this.activate(span, source, true, isAsyncPropagating);
    }

    private AgentScope activate(AgentSpan span, ScopeSource source, boolean overrideAsyncPropagation, boolean isAsyncPropagating) {
        ContinuableScopeManager.ScopeStack scopeStack = this.scopeStack();
        ContinuableScopeManager.ContinuableScope active = scopeStack.top();
        if (active != null && active.span.equals(span)) {
            active.incrementReferences();
            return active;
        } else {
            int currentDepth = scopeStack.depth();
            if (this.depthLimit <= currentDepth) {
                log.debug("Scope depth limit exceeded ({}).  Returning NoopScope.", currentDepth);
                return NoopAgentScope.INSTANCE;
            } else {
                return this.handleSpan(this.inheritAsyncPropagation ? active : null, (ContinuableScopeManager.Continuation)null, span, source, overrideAsyncPropagation, isAsyncPropagating);
            }
        }
    }

    private ContinuableScopeManager.ContinuableScope handleSpan(ContinuableScopeManager.Continuation continuation, AgentSpan span, ScopeSource source, boolean overrideAsyncPropagation, boolean isAsyncPropagating) {
        ContinuableScopeManager.ContinuableScope active = this.inheritAsyncPropagation ? this.scopeStack().top() : null;
        return this.handleSpan(active, continuation, span, source, overrideAsyncPropagation, isAsyncPropagating);
    }

    private ContinuableScopeManager.ContinuableScope handleSpan(ContinuableScopeManager.ContinuableScope active, ContinuableScopeManager.Continuation continuation, AgentSpan span, ScopeSource source, boolean overrideAsyncPropagation, boolean isAsyncPropagating) {
        boolean asyncPropagation = overrideAsyncPropagation ? isAsyncPropagating : active != null && active.isAsyncPropagating();
        ContinuableScopeManager.ContinuableScope scope = new ContinuableScopeManager.ContinuableScope(this, continuation, span, source, asyncPropagation);
        this.scopeStack().push(scope);
        return scope;
    }

    public TraceScope active() {
        return this.scopeStack().top();
    }

    public AgentSpan activeSpan() {
        AgentScope active = this.scopeStack().top();
        return active == null ? null : active.span();
    }

    public void addScopeListener(ScopeListener listener) {
        this.scopeListeners.add(listener);
    }

    protected ContinuableScopeManager.ScopeStack scopeStack() {
        return (ContinuableScopeManager.ScopeStack)this.tlsScopeStack.get();
    }

    private static final class ConcurrentContinuation extends ContinuableScopeManager.Continuation {
        private static final int START = 1;
        private static final int CLOSED = -1073741824;
        private static final int BARRIER = -536870912;
        private final AtomicInteger count;

        private ConcurrentContinuation(ContinuableScopeManager scopeManager, AgentSpan spanUnderScope, ScopeSource source) {
            super(scopeManager, spanUnderScope, source);
            this.count = new AtomicInteger(1);
        }

        private boolean tryActivate() {
            int current = this.count.incrementAndGet();
            if (current < 1) {
                this.count.decrementAndGet();
            }

            return current > 1;
        }

        private boolean tryClose() {
            int current = this.count.get();
            if (current < -536870912) {
                return false;
            } else {
                for(current = this.count.decrementAndGet(); current < 1 && current > -536870912; current = this.count.get()) {
                    if (this.count.compareAndSet(current, -1073741824)) {
                        return true;
                    }
                }

                return false;
            }
        }

        public AgentScope activate() {
            if (this.tryActivate()) {
                boolean overrideAsyncPropagation = true;
                boolean isAsyncPropagating = true;
                AgentScope scope = this.scopeManager.handleSpan(this, this.spanUnderScope, this.source, true, true);
                ContinuableScopeManager.log.debug("t_id={} -> activating continuation {}", this.spanUnderScope.getTraceId(), this);
                return scope;
            } else {
                return null;
            }
        }

        public void cancel() {
            if (this.tryClose()) {
                this.trace.cancelContinuation(this);
            }

            ContinuableScopeManager.log.debug("t_id={} -> canceling continuation {}", this.spanUnderScope.getTraceId(), this);
        }

        void cancelFromContinuedScopeClose() {
            this.cancel();
        }

        public String toString() {
            int c = this.count.get();
            String s = c < -536870912 ? "CANCELED" : String.valueOf(c);
            return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode()) + "(" + s + ")->" + this.spanUnderScope;
        }
    }

    private static final class SingleContinuation extends ContinuableScopeManager.Continuation {
        private final AtomicBoolean used;

        private SingleContinuation(ContinuableScopeManager scopeManager, AgentSpan spanUnderScope, ScopeSource source) {
            super(scopeManager, spanUnderScope, source);
            this.used = new AtomicBoolean(false);
        }

        public AgentScope activate() {
            boolean overrideAsyncPropagation = true;
            boolean isAsyncPropagating = false;
            if (this.used.compareAndSet(false, true)) {
                AgentScope scope = this.scopeManager.handleSpan(this, this.spanUnderScope, this.source, true, false);
                ContinuableScopeManager.log.debug("t_id={} -> activating continuation {}", this.spanUnderScope.getTraceId(), this);
                return scope;
            } else {
                ContinuableScopeManager.log.debug("Failed to activate continuation. Reusing a continuation not allowed. Spans may be reported separately.");
                return this.scopeManager.handleSpan((ContinuableScopeManager.Continuation)null, this.spanUnderScope, this.source, true, false);
            }
        }

        public void cancel() {
            if (this.used.compareAndSet(false, true)) {
                this.trace.cancelContinuation(this);
            } else {
                ContinuableScopeManager.log.debug("Failed to close continuation {}. Already used.", this);
            }

        }

        void cancelFromContinuedScopeClose() {
            this.trace.cancelContinuation(this);
        }

        public String toString() {
            return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode()) + "->" + this.spanUnderScope;
        }
    }

    private abstract static class Continuation implements AgentScope.Continuation {
        final ContinuableScopeManager scopeManager;
        final AgentSpan spanUnderScope;
        final ScopeSource source;
        final AgentTrace trace;

        public Continuation(ContinuableScopeManager scopeManager, AgentSpan spanUnderScope, ScopeSource source) {
            this.scopeManager = scopeManager;
            this.spanUnderScope = spanUnderScope;
            this.source = source;
            this.trace = spanUnderScope.context().getTrace();
        }

        private ContinuableScopeManager.Continuation register() {
            this.trace.registerContinuation(this);
            return this;
        }

        abstract void cancelFromContinuedScopeClose();
    }

    static final class ScopeStack {
        private final ArrayDeque<ContinuableScopeManager.ContinuableScope> stack = new ArrayDeque();

        ScopeStack() {
        }

        final ContinuableScopeManager.ContinuableScope top() {
            return (ContinuableScopeManager.ContinuableScope)this.stack.peek();
        }

        void cleanup() {
            ContinuableScopeManager.ContinuableScope curScope = (ContinuableScopeManager.ContinuableScope)this.stack.peek();

            for(boolean changedTop = false; curScope != null; curScope = (ContinuableScopeManager.ContinuableScope)this.stack.peek()) {
                if (curScope.alive()) {
                    if (changedTop) {
                        curScope.afterActivated();
                    }
                    break;
                }

                curScope.onProperClose();
                this.stack.poll();
                changedTop = true;
            }

        }

        final void push(ContinuableScopeManager.ContinuableScope scope) {
            this.stack.push(scope);
            scope.afterActivated();
        }

        final boolean checkTop(ContinuableScopeManager.ContinuableScope expectedScope) {
            return expectedScope.equals(this.stack.peek());
        }

        final int depth() {
            return this.stack.size();
        }

        final void clear() {
            this.stack.clear();
        }
    }

    private static final class ContinuableScope implements AgentScope {
        private final ContinuableScopeManager scopeManager;
        private final ContinuableScopeManager.Continuation continuation;
        private volatile boolean isAsyncPropagating;
        private final ScopeSource source;
        private final AtomicInteger referenceCount = new AtomicInteger(1);
        private final DDScopeEvent event;
        private final AgentSpan span;

        ContinuableScope(ContinuableScopeManager scopeManager, ContinuableScopeManager.Continuation continuation, AgentSpan span, ScopeSource source, boolean isAsyncPropagating) {
            this.isAsyncPropagating = isAsyncPropagating;
            this.span = span;
            this.event = scopeManager.scopeEventFactory.create(span.context());
            this.scopeManager = scopeManager;
            this.continuation = continuation;
            this.source = source;
        }

        public void close() {
            ContinuableScopeManager.ScopeStack scopeStack = this.scopeManager.scopeStack();
            boolean onTop = scopeStack.checkTop(this);
            if (!onTop) {
                if (ContinuableScopeManager.log.isDebugEnabled()) {
                    ContinuableScopeManager.log.debug("Tried to close {} scope when not on top.  Current top: {}", this, scopeStack.top());
                }

                this.scopeManager.statsDClient.incrementCounter("scope.close.error", new String[0]);
                if (this.source == ScopeSource.MANUAL) {
                    this.scopeManager.statsDClient.incrementCounter("scope.user.close.error", new String[0]);
                    if (this.scopeManager.strictMode) {
                        throw new RuntimeException("Tried to close scope when not on top");
                    }
                }
            }

            boolean alive = this.decrementReferences();
            if (!alive) {
                scopeStack.cleanup();
                if (null != this.continuation) {
                    this.continuation.cancelFromContinuedScopeClose();
                }

            }
        }

        final void onProperClose() {
            this.event.finish();
            Iterator var1 = this.scopeManager.scopeListeners.iterator();

            while(var1.hasNext()) {
                ScopeListener listener = (ScopeListener)var1.next();
                listener.afterScopeClosed();
            }

        }

        final void incrementReferences() {
            this.referenceCount.incrementAndGet();
        }

        final boolean decrementReferences() {
            return this.referenceCount.decrementAndGet() > 0;
        }

        final boolean alive() {
            return this.referenceCount.get() > 0;
        }

        public boolean isAsyncPropagating() {
            return this.isAsyncPropagating;
        }

        public AgentSpan span() {
            return this.span;
        }

        public void setAsyncPropagation(boolean value) {
            this.isAsyncPropagating = value;
        }

        public ContinuableScopeManager.Continuation capture() {
            return this.capture(false);
        }

        public ContinuableScopeManager.Continuation captureConcurrent() {
            return this.capture(true);
        }

        private ContinuableScopeManager.Continuation capture(boolean concurrent) {
            if (this.isAsyncPropagating()) {
                Object continuation;
                if (concurrent) {
                    continuation = new ContinuableScopeManager.ConcurrentContinuation(this.scopeManager, this.span, this.source);
                } else {
                    continuation = new ContinuableScopeManager.SingleContinuation(this.scopeManager, this.span, this.source);
                }

                return ((ContinuableScopeManager.Continuation)continuation).register();
            } else {
                return null;
            }
        }

        public String toString() {
            return super.toString() + "->" + this.span;
        }

        public void afterActivated() {
            Iterator var1 = this.scopeManager.scopeListeners.iterator();

            while(var1.hasNext()) {
                ScopeListener listener = (ScopeListener)var1.next();
                listener.afterScopeActivated();
            }

            this.event.start();
        }
    }
}
