package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.core.ExclusiveSpan;

class ForceManualKeepTagInterceptor extends AbstractTagInterceptor {
    public ForceManualKeepTagInterceptor() {
        super("manual.keep");
    }

    public boolean shouldSetTag(ExclusiveSpan span, String tag, Object value) {
        if (value instanceof Boolean && (Boolean)value) {
            span.setSamplingPriority(2);
        } else if (value instanceof String && Boolean.parseBoolean((String)value)) {
            span.setSamplingPriority(2);
        }

        return false;
    }
}
