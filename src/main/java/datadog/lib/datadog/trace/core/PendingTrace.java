package datadog.lib.datadog.trace.core;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope.Continuation;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTrace;
import datadog.lib.datadog.trace.core.monitor.Recording;
import datadog.lib.datadog.trace.core.util.Clock;
import datadog.trace.api.DDId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PendingTrace implements AgentTrace {
    private static final Logger log = LoggerFactory.getLogger(PendingTrace.class);
    private final CoreTracer tracer;
    private final DDId traceId;
    private final PendingTraceBuffer pendingTraceBuffer;
    private final long startTimeNano;
    private final long startNanoTicks;
    private final ConcurrentLinkedDeque<DDSpan> finishedSpans;
    private final AtomicInteger completedSpanCount;
    private final AtomicInteger pendingReferenceCount;
    private final AtomicReference<WeakReference<DDSpan>> rootSpan;
    private final AtomicBoolean rootSpanWritten;
    private final AtomicBoolean traceValid;
    private volatile long lastReferenced;

    private PendingTrace(CoreTracer tracer, DDId traceId, PendingTraceBuffer pendingTraceBuffer) {
        this.finishedSpans = new ConcurrentLinkedDeque();
        this.completedSpanCount = new AtomicInteger(0);
        this.pendingReferenceCount = new AtomicInteger(0);
        this.rootSpan = new AtomicReference();
        this.rootSpanWritten = new AtomicBoolean(false);
        this.traceValid = new AtomicBoolean(true);
        this.lastReferenced = 0L;
        this.tracer = tracer;
        this.traceId = traceId;
        this.pendingTraceBuffer = pendingTraceBuffer;
        this.startTimeNano = Clock.currentNanoTime();
        this.startNanoTicks = Clock.currentNanoTicks();
    }

    public long getCurrentTimeNano() {
        long nanoTicks = Clock.currentNanoTicks();
        this.lastReferenced = nanoTicks;
        return this.startTimeNano + Math.max(0L, nanoTicks - this.startNanoTicks);
    }

    public void touch() {
        this.lastReferenced = Clock.currentNanoTicks();
    }

    public boolean lastReferencedNanosAgo(long nanos) {
        long currentNanoTicks = Clock.currentNanoTicks();
        long age = currentNanoTicks - this.lastReferenced;
        return nanos < age;
    }

    public void registerSpan(DDSpan span) {
        if (this.traceId != null && span.context() != null) {
            if (!this.traceId.equals(span.context().getTraceId())) {
                log.debug("t_id={} -> registered for wrong trace {}", this.traceId, span);
            } else {
                if (!this.rootSpanWritten.get()) {
                    this.rootSpan.compareAndSet(null, new WeakReference(span));
                }

                int count = this.pendingReferenceCount.incrementAndGet();
                if (log.isDebugEnabled()) {
                    log.debug("t_id={} -> registered span {}. count = {}", new Object[]{this.traceId, span, count});
                }

            }
        } else {
            log.error("Failed to register span ({}) due to null PendingTrace traceId or null span context", span);
        }
    }

    public void addFinishedSpan(DDSpan span) {
        if (span.getDurationNano() == 0L) {
            log.debug("t_id={} -> added to trace, but not complete: {}", this.traceId, span);
        } else if (this.traceId != null && span.context() != null) {
            if (!this.traceId.equals(span.getTraceId())) {
                log.debug("t_id={} -> span expired for wrong trace {}", this.traceId, span);
            } else if (this.traceId != null && span.context() != null) {
                this.finishedSpans.addFirst(span);
                this.completedSpanCount.incrementAndGet();
                this.decrementRefAndMaybeWrite(span == this.getRootSpan());
            } else {
                log.error("Failed to expire span ({}) due to null PendingTrace traceId or null span context", span);
            }
        } else {
            log.error("Failed to add span ({}) due to null PendingTrace traceId or null span context", span);
        }
    }

    public DDSpan getRootSpan() {
        WeakReference<DDSpan> rootRef = (WeakReference)this.rootSpan.get();
        return rootRef == null ? null : (DDSpan)rootRef.get();
    }

    long oldestFinishedTime() {
        long oldest = 9223372036854775807L;

        DDSpan span;
        for(Iterator var3 = this.finishedSpans.iterator(); var3.hasNext(); oldest = Math.min(oldest, span.getStartTime() + span.getDurationNano())) {
            span = (DDSpan)var3.next();
        }

        return oldest;
    }

    public void registerContinuation(Continuation continuation) {
        int count = this.pendingReferenceCount.incrementAndGet();
        if (log.isDebugEnabled()) {
            log.debug("t_id={} -> registered continuation {} -- count = {}", new Object[]{this.traceId, continuation, count});
        }

    }

    public void cancelContinuation(Continuation continuation) {
        this.decrementRefAndMaybeWrite(false);
    }

    private void decrementRefAndMaybeWrite(boolean isRootSpan) {
        if (this.traceValid.get()) {
            int count = this.pendingReferenceCount.decrementAndGet();
            if (count == 0 && !this.rootSpanWritten.get()) {
                this.write();
            } else if (isRootSpan) {
                this.pendingTraceBuffer.enqueue(this);
            } else {
                int partialFlushMinSpans = this.tracer.getPartialFlushMinSpans();
                if (0 < partialFlushMinSpans && partialFlushMinSpans < this.size()) {
                    this.partialFlush();
                } else if (this.rootSpanWritten.get()) {
                    this.pendingTraceBuffer.enqueue(this);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("t_id={} -> expired reference. pending count={}", this.traceId, count);
            }

        }
    }

    private void partialFlush() {
        int size = this.write(true);
        if (log.isDebugEnabled()) {
            log.debug("t_id={} -> writing partial trace of size {}", this.traceId, size);
        }

    }

    void write() {
        this.rootSpanWritten.set(true);
        int size = this.write(false);
        if (log.isDebugEnabled()) {
            log.debug("t_id={} -> writing {} spans to {}.", new Object[]{this.traceId, size, this.tracer.writer});
        }

    }

    private int write(boolean isPartial) {
        if (!this.finishedSpans.isEmpty()) {
            Recording recording = this.tracer.writeTimer();
            Throwable var3 = null;

            try {
                synchronized(this) {
                    int size = this.size();
                    if (isPartial && size <= this.tracer.getPartialFlushMinSpans()) {
                        return 0;
                    } else {
                        List<DDSpan> trace = new ArrayList(size);
                        Iterator it = this.finishedSpans.iterator();

                        while(it.hasNext()) {
                            DDSpan span = (DDSpan)it.next();
                            trace.add(span);
                            this.completedSpanCount.decrementAndGet();
                            it.remove();
                        }

                        this.tracer.write(trace);
                        int var23 = size;
                        return var23;
                    }
                }
            } catch (Throwable var21) {
                var3 = var21;
                throw var21;
            } finally {
                if (recording != null) {
                    if (var3 != null) {
                        try {
                            recording.close();
                        } catch (Throwable var19) {
                            var3.addSuppressed(var19);
                        }
                    } else {
                        recording.close();
                    }
                }

            }
        } else {
            return 0;
        }
    }

    public int size() {
        return this.completedSpanCount.get();
    }

    static class Factory {
        private final CoreTracer tracer;
        private final PendingTraceBuffer pendingTraceBuffer;

        Factory(CoreTracer tracer, PendingTraceBuffer pendingTraceBuffer) {
            this.tracer = tracer;
            this.pendingTraceBuffer = pendingTraceBuffer;
        }

        PendingTrace create(DDId traceId) {
            return new PendingTrace(this.tracer, traceId, this.pendingTraceBuffer);
        }
    }
}
