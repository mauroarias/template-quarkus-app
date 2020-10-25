package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultLogHandler implements LogHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultLogHandler.class);

    public DefaultLogHandler() {
    }

    public void log(Map<String, ?> fields, AgentSpan span) {
        this.extractError(fields, span);
    }

    public void log(long timestampMicroseconds, Map<String, ?> fields, AgentSpan span) {
        this.extractError(fields, span);
    }

    public void log(String event, AgentSpan span) {
        log.debug("`log` method is not implemented. Provided log: {}", event);
    }

    public void log(long timestampMicroseconds, String event, AgentSpan span) {
        log.debug("`log` method is not implemented. Provided log: {}", event);
    }

    private boolean isErrorSpan(Map<String, ?> map, AgentSpan span) {
        String event = map.get("event") instanceof String ? (String)map.get("event") : "";
        return span.isError() || event.equalsIgnoreCase("error");
    }

    private void extractError(Map<String, ?> map, AgentSpan span) {
        if (map.get("error.object") instanceof Throwable) {
            Throwable error = (Throwable)map.get("error.object");
            span.addThrowable(error);
        } else if (this.isErrorSpan(map, span) && map.get("message") instanceof String) {
            span.setTag("error.msg", (String)map.get("message"));
        }

    }
}
