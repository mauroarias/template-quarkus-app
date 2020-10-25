package datadog.lib.datadog.trace.common.writer;

import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ListWriter extends CopyOnWriteArrayList<List<DDSpan>> implements Writer {
    private final TraceProcessor processor = new TraceProcessor();
    private final List<CountDownLatch> latches = new ArrayList();
    private final AtomicInteger traceCount = new AtomicInteger();
    private final TraceStructureWriter structureWriter = new TraceStructureWriter();

    public ListWriter() {
    }

    public List<DDSpan> firstTrace() {
        return (List)this.get(0);
    }

    public void write(List<DDSpan> trace) {
        this.incrementTraceCount();
        synchronized(this.latches) {
            trace = this.processor.onTraceComplete(trace);
            this.add(trace);
            Iterator var3 = this.latches.iterator();

            while(var3.hasNext()) {
                CountDownLatch latch = (CountDownLatch)var3.next();
                if ((long)this.size() >= latch.getCount()) {
                    while(latch.getCount() > 0L) {
                        latch.countDown();
                    }
                }
            }
        }

        this.structureWriter.write(trace);
    }

    public void waitForTraces(int number) throws InterruptedException, TimeoutException {
        CountDownLatch latch = new CountDownLatch(number);
        synchronized(this.latches) {
            if (this.size() >= number) {
                return;
            }

            this.latches.add(latch);
        }

        if (!latch.await(20L, TimeUnit.SECONDS)) {
            throw new TimeoutException("Timeout waiting for " + number + " trace(s). ListWriter.size() == " + this.size());
        }
    }

    public void waitUntilReported(DDSpan span) throws InterruptedException, TimeoutException {
        CountDownLatch latch;
        do {
            latch = new CountDownLatch(this.size() + 1);
            synchronized(this.latches) {
                this.latches.add(latch);
            }

            if (this.isReported(span)) {
                return;
            }
        } while(latch.await(20L, TimeUnit.SECONDS));

        throw new TimeoutException("Timeout waiting for span to be reported: " + span);
    }

    private boolean isReported(DDSpan span) {
        Iterator var2 = this.iterator();

        while(var2.hasNext()) {
            List<DDSpan> trace = (List)var2.next();
            Iterator var4 = trace.iterator();

            while(var4.hasNext()) {
                DDSpan aSpan = (DDSpan)var4.next();
                if (aSpan == span) {
                    return true;
                }
            }
        }

        return false;
    }

    public void incrementTraceCount() {
        this.traceCount.incrementAndGet();
    }

    public void start() {
        this.close();
    }

    public boolean flush() {
        return true;
    }

    public void close() {
        this.clear();
        synchronized(this.latches) {
            Iterator var2 = this.latches.iterator();

            while(var2.hasNext()) {
                CountDownLatch latch = (CountDownLatch)var2.next();

                while(latch.getCount() > 0L) {
                    latch.countDown();
                }
            }

            this.latches.clear();
        }
    }

    public String toString() {
        return "ListWriter { size=" + this.size() + " }";
    }
}
