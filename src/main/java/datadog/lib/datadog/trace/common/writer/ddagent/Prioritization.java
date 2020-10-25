package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.core.DDSpan;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public enum Prioritization {
    ENSURE_TRACE {
        public PrioritizationStrategy create(Queue<Object> primary, Queue<Object> secondary) {
            return new EnsureTraceStrategy(primary, secondary);
        }
    },
    FAST_LANE {
        public PrioritizationStrategy create(Queue<Object> primary, Queue<Object> secondary) {
            return new FastLaneStrategy(primary, secondary);
        }
    },
    DEAD_LETTERS {
        public PrioritizationStrategy create(Queue<Object> primary, Queue<Object> secondary) {
            return new DeadLettersStrategy(primary, secondary);
        }
    };

    private Prioritization() {
    }

    public abstract PrioritizationStrategy create(Queue<Object> var1, Queue<Object> var2);

    private static final class DeadLettersStrategy implements PrioritizationStrategy {
        private final Queue<Object> primary;
        private final Queue<Object> secondary;

        private DeadLettersStrategy(Queue<Object> primary, Queue<Object> secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public boolean publish(int priority, List<DDSpan> trace) {
            if (!this.primary.offer(trace)) {
                switch(priority) {
                    case -1:
                    case 0:
                        return false;
                    default:
                        return this.secondary.offer(trace);
                }
            } else {
                return true;
            }
        }

        public boolean flush(long timeout, TimeUnit timeUnit) {
            CountDownLatch latch = new CountDownLatch(2);
            FlushEvent event = new FlushEvent(latch);
            this.offer(this.primary, event);
            this.offer(this.secondary, event);

            try {
                return latch.await(timeout, timeUnit);
            } catch (InterruptedException var7) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private void offer(Queue<Object> queue, FlushEvent event) {
            boolean offered;
            do {
                offered = queue.offer(event);
            } while(!offered);

        }
    }

    private static final class FastLaneStrategy implements PrioritizationStrategy {
        private final Queue<Object> primary;
        private final Queue<Object> secondary;

        private FastLaneStrategy(Queue<Object> primary, Queue<Object> secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public boolean publish(int priority, List<DDSpan> trace) {
            switch(priority) {
                case -1:
                case 0:
                    return this.secondary.offer(trace);
                default:
                    return this.primary.offer(trace);
            }
        }

        public boolean flush(long timeout, TimeUnit timeUnit) {
            CountDownLatch latch = new CountDownLatch(1);
            FlushEvent event = new FlushEvent(latch);
            this.offer(this.primary, event);

            try {
                return latch.await(timeout, timeUnit);
            } catch (InterruptedException var7) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private void offer(Queue<Object> queue, FlushEvent event) {
            boolean offered;
            do {
                offered = queue.offer(event);
            } while(!offered);

        }
    }

    private static final class EnsureTraceStrategy implements PrioritizationStrategy {
        private final Queue<Object> primary;
        private final Queue<Object> secondary;

        private EnsureTraceStrategy(Queue<Object> primary, Queue<Object> secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public boolean publish(int priority, List<DDSpan> trace) {
            switch(priority) {
                case -1:
                case 0:
                    return this.secondary.offer(trace);
                default:
                    this.blockingOffer(this.primary, trace);
                    return true;
            }
        }

        public boolean flush(long timeout, TimeUnit timeUnit) {
            CountDownLatch latch = new CountDownLatch(1);
            FlushEvent event = new FlushEvent(latch);
            this.blockingOffer(this.primary, event);

            try {
                return latch.await(timeout, timeUnit);
            } catch (InterruptedException var7) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        private void blockingOffer(Queue<Object> queue, Object data) {
            boolean offered;
            do {
                offered = queue.offer(data);
            } while(!offered);

        }
    }
}
