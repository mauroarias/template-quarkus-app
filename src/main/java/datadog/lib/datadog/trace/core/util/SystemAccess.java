package datadog.lib.datadog.trace.core.util;

import datadog.lib.datadog.trace.api.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class SystemAccess {
    private static final Logger log = LoggerFactory.getLogger(SystemAccess.class);
    private static volatile SystemAccessProvider systemAccessProvider;

    public SystemAccess() {
    }

    public static void disableJmx() {
        log.debug("Disabling JMX system access provider");
        systemAccessProvider = SystemAccessProvider.NONE;
    }

    public static void enableJmx() {
        if (!Config.get().isProfilingEnabled() && !Config.get().isHealthMetricsEnabled()) {
            log.debug("Will not enable JMX access. Profiling and metrics are both disabled.");
        } else {
            try {
                log.debug("Enabling JMX system provider");
                systemAccessProvider = (SystemAccessProvider)Class.forName("datadog.lib.datadog.trace.core.util.JmxSystemAccessProvider").getField("INSTANCE").get((Object)null);
            } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException var1) {
                log.info("Unable to initialize JMX system provider", var1);
            }

        }
    }

    public static long getCurrentThreadCpuTime() {
        return systemAccessProvider.getThreadCpuTime();
    }

    public static int getCurrentPid() {
        return systemAccessProvider.getCurrentPid();
    }

    public static String executeDiagnosticCommand(String command, Object[] args, String[] sig) {
        return systemAccessProvider.executeDiagnosticCommand(command, args, sig);
    }

    public static List<String> getVMArguments() {
        return systemAccessProvider.getVMArguments();
    }

    static {
        systemAccessProvider = SystemAccessProvider.NONE;
    }
}
