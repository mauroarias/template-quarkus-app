package datadog.lib.datadog.trace.util;

import java.util.concurrent.ThreadFactory;

public final class AgentThreadFactory implements ThreadFactory {
    public static final ThreadGroup AGENT_THREAD_GROUP = new ThreadGroup("dd-trace-java");
    private final AgentThreadFactory.AgentThread agentThread;

    public AgentThreadFactory(AgentThreadFactory.AgentThread agentThread) {
        this.agentThread = agentThread;
    }

    public Thread newThread(Runnable runnable) {
        return newAgentThread(this.agentThread, runnable);
    }

    public static Thread newAgentThread(AgentThreadFactory.AgentThread agentThread, Runnable runnable) {
        Thread thread = new Thread(AGENT_THREAD_GROUP, runnable, agentThread.threadName);
        thread.setDaemon(true);
        thread.setContextClassLoader((ClassLoader)null);
        return thread;
    }

    public static enum AgentThread {
        TASK_SCHEDULER("dd-task-scheduler"),
        TRACE_STARTUP("dd-agent-startup-datadog-tracer"),
        TRACE_MONITOR("dd-trace-monitor"),
        TRACE_PROCESSOR("dd-trace-processor"),
        TRACE_CASSANDRA_ASYNC_SESSION("dd-cassandra-session-executor"),
        JMX_STARTUP("dd-agent-startup-jmxfetch"),
        JMX_COLLECTOR("dd-jmx-collector"),
        PROFILER_STARTUP("dd-agent-startup-datadog-profiler"),
        PROFILER_RECORDING_SCHEDULER("dd-profiler-recording-scheduler"),
        PROFILER_HTTP_DISPATCHER("dd-profiler-http-dispatcher");

        public final String threadName;

        private AgentThread(String threadName) {
            this.threadName = threadName;
        }
    }
}
