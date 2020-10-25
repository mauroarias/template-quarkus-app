package datadog.lib.datadog.trace.core.util;

import java.util.Collections;
import java.util.List;

final class NoneSystemAccessProvider implements SystemAccessProvider {
    NoneSystemAccessProvider() {
    }

    public long getThreadCpuTime() {
        return -9223372036854775808L;
    }

    public int getCurrentPid() {
        return 0;
    }

    public String executeDiagnosticCommand(String command, Object[] args, String[] sig) {
        return "Not executed, JMX not initialized.";
    }

    public List<String> getVMArguments() {
        return Collections.emptyList();
    }
}
