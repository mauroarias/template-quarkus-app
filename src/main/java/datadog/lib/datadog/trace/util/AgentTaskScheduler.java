package datadog.lib.datadog.trace.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AgentTaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(AgentTaskScheduler.class);
    public static final AgentTaskScheduler INSTANCE;
    private static final long SHUTDOWN_WAIT_MILLIS = 5000L;
    private final DelayQueue<PeriodicTask<?>> workQueue = new DelayQueue();
    private final AgentThreadFactory.AgentThread agentThread;
    private volatile Thread worker;
    private volatile boolean shutdown;
    private static final AtomicInteger TASK_SEQUENCE_GENERATOR;

    public AgentTaskScheduler(AgentThreadFactory.AgentThread agentThread) {
        this.agentThread = agentThread;
    }

    public <T> void weakScheduleAtFixedRate(AgentTaskScheduler.Task<T> task, T target, long initialDelay, long period, TimeUnit unit) {
        this.scheduleAtFixedRate(task, new WeakTarget(target), initialDelay, period, unit);
    }

    public <T> void scheduleAtFixedRate(Task<T> task, Target<T> target, long initialDelay, long period, TimeUnit unit) {
        if (target != null && target.get() != null) {
            if (!this.shutdown && this.worker == null) {
                synchronized(this.workQueue) {
                    if (!this.shutdown && this.worker == null) {
                        this.prepareWorkQueue();

                        try {
                            this.worker = AgentThreadFactory.newAgentThread(this.agentThread, new Worker());
                            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
                            this.worker.start();
                        } catch (IllegalStateException var11) {
                            this.shutdown = true;
                        }
                    }
                }
            }

            if (!this.shutdown) {
                this.workQueue.offer(new PeriodicTask(task, target, initialDelay, period, unit));
            } else {
                log.warn("Agent task scheduler is shutdown. Will not run {}", describeTask(task, target));
            }

        }
    }

    private void prepareWorkQueue() {
        try {
            this.workQueue.poll(1L, TimeUnit.NANOSECONDS);
        } catch (InterruptedException var2) {
        }

    }

    public boolean isShutdown() {
        return this.shutdown;
    }

    private static <T> String describeTask(Task<T> task, Target<T> target) {
        return "periodic task " + task.getClass().getSimpleName() + " with target " + target.get();
    }

    static {
        INSTANCE = new AgentTaskScheduler(AgentThreadFactory.AgentThread.TASK_SCHEDULER);
        TASK_SEQUENCE_GENERATOR = new AtomicInteger();
    }

    private static final class PeriodicTask<T> implements Delayed {
        private final Task<T> task;
        private final Target<T> target;
        private final int period;
        private final int taskSequence;
        private long time;

        public PeriodicTask(Task<T> task, Target<T> target, long initialDelay, long period, TimeUnit unit) {
            this.task = task;
            this.target = target;
            this.period = (int)unit.toNanos(period);
            this.taskSequence = TASK_SEQUENCE_GENERATOR.getAndIncrement();
            this.time = System.nanoTime() + unit.toNanos(initialDelay);
        }

        public void run() {
            T t = this.target.get();
            if (t != null) {
                this.task.run(t);
            }

        }

        public boolean reschedule() {
            if (this.target.get() != null) {
                this.time += (long)this.period;
                return true;
            } else {
                return false;
            }
        }

        public long getDelay(TimeUnit unit) {
            return unit.convert(this.time - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        public int compareTo(Delayed other) {
            if (this == other) {
                return 0;
            } else {
                long taskOrder;
                if (other instanceof PeriodicTask) {
                    PeriodicTask<?> otherTask = (PeriodicTask)other;
                    taskOrder = this.time - otherTask.time;
                    if (taskOrder == 0L) {
                        taskOrder = (long)(this.taskSequence - otherTask.taskSequence);
                    }
                } else {
                    taskOrder = this.getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
                }

                return taskOrder < 0L ? -1 : (taskOrder > 0L ? 1 : 0);
            }
        }

        public String toString() {
            return describeTask(this.task, this.target);
        }
    }

    private final class Worker implements Runnable {
        private Worker() {
        }

        public void run() {
            while(!shutdown) {
                PeriodicTask work = null;

                try {
                    work = (PeriodicTask) workQueue.take();
                    work.run();
                } catch (Throwable var6) {
                    if (work != null) {
                        log.warn("Uncaught exception from {}", work, var6);
                    }
                } finally {
                    if (work != null && work.reschedule()) {
                        workQueue.offer(work);
                    }

                }
            }

            worker = null;
        }
    }

    private final class ShutdownHook extends Thread {
        ShutdownHook() {
            super(AgentThreadFactory.AGENT_THREAD_GROUP, agentThread.threadName + "-shutdown-hook");
        }

        public void run() {
            shutdown = true;
            Thread t = worker;
            if (t != null) {
                t.interrupt();

                try {
                    t.join(5000L);
                } catch (InterruptedException var3) {
                }
            }

        }
    }

    private static final class WeakTarget<T> extends WeakReference<T> implements Target<T> {
        public WeakTarget(T referent) {
            super(referent);
        }
    }

    public interface Target<T> {
        T get();
    }

    public interface Task<T> {
        void run(T var1);
    }
}
