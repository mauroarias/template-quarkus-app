package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.monitor.HealthMetrics;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import datadog.lib.datadog.trace.core.monitor.Recording;
import datadog.lib.datadog.trace.core.processor.TraceProcessor;
import datadog.lib.datadog.trace.util.AgentThreadFactory;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TraceProcessingWorker implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TraceProcessingWorker.class);
    private final PrioritizationStrategy prioritizationStrategy;
    private final MpscBlockingConsumerArrayQueue<Object> primaryQueue;
    private final MpscBlockingConsumerArrayQueue<Object> secondaryQueue;
    private final TraceSerializingHandler serializingHandler;
    private final Thread serializerThread;
    private final int capacity;

    public TraceProcessingWorker(int capacity, HealthMetrics healthMetrics, Monitoring monitoring, PayloadDispatcher dispatcher, Prioritization prioritization, long flushInterval, TimeUnit timeUnit) {
        this(capacity, healthMetrics, monitoring, dispatcher, new TraceProcessor(), prioritization, flushInterval, timeUnit);
    }

    public TraceProcessingWorker(int capacity, HealthMetrics healthMetrics, Monitoring monitoring, PayloadDispatcher dispatcher, TraceProcessor processor, Prioritization prioritization, long flushInterval, TimeUnit timeUnit) {
        this.capacity = capacity;
        this.primaryQueue = createQueue(capacity);
        this.secondaryQueue = createQueue(capacity);
        this.prioritizationStrategy = prioritization.create(this.primaryQueue, this.secondaryQueue);
        this.serializingHandler = new TraceSerializingHandler(this.primaryQueue, this.secondaryQueue, healthMetrics, monitoring, processor, dispatcher, flushInterval, timeUnit);
        this.serializerThread = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.TRACE_PROCESSOR, this.serializingHandler);
    }

    public void start() {
        this.serializerThread.start();
    }

    public boolean flush(long timeout, TimeUnit timeUnit) {
        CountDownLatch latch = new CountDownLatch(1);
        FlushEvent flush = new FlushEvent(latch);

        boolean offered;
        do {
            offered = this.primaryQueue.offer(flush);
        } while(!offered && this.serializerThread.isAlive());

        try {
            return latch.await(timeout, timeUnit);
        } catch (InterruptedException var8) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void close() {
        this.serializerThread.interrupt();
    }

    public boolean publish(int samplingPriority, List<DDSpan> trace) {
        return this.prioritizationStrategy.publish(samplingPriority, trace);
    }

    public int getCapacity() {
        return this.capacity;
    }

    public long getRemainingCapacity() {
        return (long)this.primaryQueue.remainingCapacity();
    }

    private static MpscBlockingConsumerArrayQueue<Object> createQueue(int capacity) {
        return new MpscBlockingConsumerArrayQueue(capacity);
    }

    public static class TraceSerializingHandler implements Runnable, MessagePassingQueue.Consumer<Object> {
        private final MpscBlockingConsumerArrayQueue<Object> primaryQueue;
        private final MpscBlockingConsumerArrayQueue<Object> secondaryQueue;
        private final TraceProcessor processor;
        private final HealthMetrics healthMetrics;
        private final long ticksRequiredToFlush;
        private final boolean doTimeFlush;
        private final PayloadDispatcher payloadDispatcher;
        private long lastTicks;
        private final Recording dutyCycleTimer;

        public TraceSerializingHandler(MpscBlockingConsumerArrayQueue<Object> primaryQueue, MpscBlockingConsumerArrayQueue<Object> secondaryQueue, HealthMetrics healthMetrics, Monitoring monitoring, TraceProcessor traceProcessor, PayloadDispatcher payloadDispatcher, long flushInterval, TimeUnit timeUnit) {
            this.primaryQueue = primaryQueue;
            this.secondaryQueue = secondaryQueue;
            this.healthMetrics = healthMetrics;
            this.dutyCycleTimer = monitoring.newCPUTimer("tracer.duty.cycle");
            this.processor = traceProcessor;
            this.doTimeFlush = flushInterval > 0L;
            this.payloadDispatcher = payloadDispatcher;
            if (this.doTimeFlush) {
                this.lastTicks = System.nanoTime();
                this.ticksRequiredToFlush = timeUnit.toNanos(flushInterval);
            } else {
                this.ticksRequiredToFlush = 9223372036854775807L;
            }

        }

        public void onEvent(Object event) {
            try {
                if (event instanceof List) {
                    List<DDSpan> trace = (List)event;
                    this.payloadDispatcher.addTrace(this.processor.onTraceComplete(trace));
                } else if (event instanceof FlushEvent) {
                    this.payloadDispatcher.flush();
                    ((FlushEvent)event).sync();
                }
            } catch (Throwable var4) {
                if (log.isDebugEnabled()) {
                    log.debug("Error while serializing trace", var4);
                }

                List<DDSpan> data = event instanceof List ? (List)event : null;
                this.healthMetrics.onFailedSerialize(data, var4);
            }

        }

        public void run() {
            try {
                this.runDutyCycle();
            } catch (InterruptedException var2) {
                Thread.currentThread().interrupt();
            }

            log.debug("Datadog trace processor exited. Publishing traces stopped");
        }

        private void runDutyCycle() throws InterruptedException {
            Thread thread = Thread.currentThread();
            this.dutyCycleTimer.start();

            while(!thread.isInterrupted()) {
                this.consumeFromPrimaryQueue();
                this.consumeFromSecondaryQueue();
                this.flushIfNecessary();
                this.dutyCycleTimer.reset();
            }

            this.dutyCycleTimer.stop();
        }

        private void consumeFromPrimaryQueue() throws InterruptedException {
            Object event = this.primaryQueue.poll(100L, TimeUnit.MILLISECONDS);
            if (null != event) {
                this.onEvent(event);
                this.consumeBatch(this.primaryQueue);
            }

        }

        private void consumeFromSecondaryQueue() {
            Object event = this.secondaryQueue.poll();
            if (null != event) {
                this.onEvent(event);
                this.consumeBatch(this.secondaryQueue);
            }

        }

        private void flushIfNecessary() {
            if (this.shouldFlush()) {
                this.payloadDispatcher.flush();
            }

        }

        private boolean shouldFlush() {
            if (this.doTimeFlush) {
                long nanoTime = System.nanoTime();
                long ticks = nanoTime - this.lastTicks;
                if (ticks > this.ticksRequiredToFlush) {
                    this.lastTicks = nanoTime;
                    return true;
                }
            }

            return false;
        }

        private void consumeBatch(MessagePassingQueue<Object> queue) {
            queue.drain(this, queue.size());
        }

        public void accept(Object event) {
            this.onEvent(event);
        }
    }
}
