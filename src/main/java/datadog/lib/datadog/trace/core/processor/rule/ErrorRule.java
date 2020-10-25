package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;

public class ErrorRule implements Rule {
    public ErrorRule() {
    }

    public String[] aliases() {
        return new String[]{"ErrorFlag"};
    }

    public void processSpan(ExclusiveSpan span) {
        Object value = span.getAndRemoveTag("error");
        if (value instanceof Boolean) {
            span.setError((Boolean)value);
        } else if (value != null) {
            span.setError(Boolean.parseBoolean(value.toString()));
        }

    }
}
