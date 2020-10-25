package datadog.lib.datadog.trace.core;

import datadog.lib.datadog.trace.util.AgentThreadFactory;
import datadog.lib.datadog.trace.util.AgentThreadFactory.AgentThread;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;

import java.util.concurrent.TimeUnit;

class PendingTraceBuffer implements AutoCloseable {
    private static final int BUFFER_SIZE = 4096;
    private final long FORCE_SEND_DELAY_MS;
    private final long SEND_DELAY_NS;
    private final long SLEEP_TIME_MS;
    private final MpscBlockingConsumerArrayQueue<PendingTrace> queue;
    private final Thread worker;
    private volatile boolean closed;

    PendingTraceBuffer() {
        this.FORCE_SEND_DELAY_MS = TimeUnit.SECONDS.toMillis(5L);
        this.SEND_DELAY_NS = TimeUnit.MILLISECONDS.toNanos(500L);
        this.SLEEP_TIME_MS = 100L;
        this.queue = new MpscBlockingConsumerArrayQueue(4096);
        this.worker = AgentThreadFactory.newAgentThread(AgentThread.TRACE_MONITOR, new Worker());
        this.closed = false;
    }

    public void enqueue(PendingTrace pendingTrace) {
        if (!this.queue.offer(pendingTrace)) {
            pendingTrace.write();
        }

    }

    public void start() {
        this.worker.start();
    }

    public void close() {
        this.closed = true;
        this.worker.interrupt();
    }

    public void flush() {
        this.queue.drain(WriteDrain.WRITE_DRAIN);
    }

    private final class Worker implements Runnable {
        private Worker() {
        }

        public void run() {
            while(true) {
                try {
                    if (!closed && !Thread.currentThread().isInterrupted()) {
                        PendingTrace pendingTrace = queue.take();
                        long oldestFinishedTime = pendingTrace.oldestFinishedTime();
                        long finishTimestampMillis = TimeUnit.NANOSECONDS.toMillis(oldestFinishedTime);
                        if (finishTimestampMillis <= System.currentTimeMillis() - FORCE_SEND_DELAY_MS) {
                            pendingTrace.write();
                            continue;
                        }

                        if (pendingTrace.lastReferencedNanosAgo(SEND_DELAY_NS)) {
                            pendingTrace.write();
                            continue;
                        }

                        enqueue(pendingTrace);
                        Thread.sleep(100L);
                        continue;
                    }
                } catch (InterruptedException var6) {
                    Thread.currentThread().interrupt();
                }

                return;
            }
        }
    }

    private static final class WriteDrain implements MessagePassingQueue.Consumer<PendingTrace> {
        private static final WriteDrain WRITE_DRAIN = new WriteDrain();

        private WriteDrain() {
        }

        public void accept(PendingTrace pendingTrace) {
            pendingTrace.write();
        }
    }
}
