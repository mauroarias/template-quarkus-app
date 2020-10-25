package datadog.lib.datadog.trace.core.util;

import java.util.List;

public interface SystemAccessProvider {
    SystemAccessProvider NONE = new NoneSystemAccessProvider();

    long getThreadCpuTime();

    int getCurrentPid();

    String executeDiagnosticCommand(String var1, Object[] var2, String[] var3);

    List<String> getVMArguments();
}
