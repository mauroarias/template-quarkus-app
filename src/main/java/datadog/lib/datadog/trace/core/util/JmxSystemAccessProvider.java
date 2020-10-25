package datadog.lib.datadog.trace.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.List;

final class JmxSystemAccessProvider implements SystemAccessProvider {
    private static final Logger log = LoggerFactory.getLogger(JmxSystemAccessProvider.class);
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final boolean cpuTimeSupported;
    public static final JmxSystemAccessProvider INSTANCE = new JmxSystemAccessProvider();

    JmxSystemAccessProvider() {
        this.cpuTimeSupported = this.threadMXBean.isCurrentThreadCpuTimeSupported();
    }

    public long getThreadCpuTime() {
        return this.cpuTimeSupported ? this.threadMXBean.getCurrentThreadCpuTime() : -9223372036854775808L;
    }

    public int getCurrentPid() {
        String name = this.runtimeMXBean.getName();
        if (name == null) {
            return 0;
        } else {
            int idx = name.indexOf(64);
            if (idx == -1) {
                return 0;
            } else {
                String pid = name.substring(0, idx);
                return Integer.parseInt(pid);
            }
        }
    }

    public String executeDiagnosticCommand(String command, Object[] args, String[] sig) {
        ObjectName diagnosticCommandMBean = null;

        try {
            diagnosticCommandMBean = new ObjectName("com.sun.management:type=DiagnosticCommand");
        } catch (MalformedObjectNameException var7) {
            log.warn("Error during executeDiagnosticCommand: ", var7);
            return var7.getMessage();
        }

        try {
            Object result = ManagementFactory.getPlatformMBeanServer().invoke(diagnosticCommandMBean, command, args, sig);
            return result != null ? result.toString().trim() : null;
        } catch (Throwable var6) {
            log.warn("Error invoking diagnostic command: ", var6);
            return var6.getMessage();
        }
    }

    public List<String> getVMArguments() {
        List args = Collections.emptyList();

        try {
            args = this.runtimeMXBean.getInputArguments();
        } catch (Throwable var3) {
            log.warn("Error invoking runtimeMxBean.getInputArguments: ", var3);
        }

        return args;
    }
}
