package datadog.lib.datadog.trace.common.writer.ddagent;

import java.util.Map;

public interface DDAgentResponseListener {
    void onResponse(String var1, Map<String, Map<String, Number>> var2);
}
