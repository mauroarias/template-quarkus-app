package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.core.ExclusiveSpan;

class ForceManualDropTagInterceptor extends AbstractTagInterceptor {
    public ForceManualDropTagInterceptor() {
        super("manual.drop");
    }

    public boolean shouldSetTag(ExclusiveSpan span, String tag, Object value) {
        if (value instanceof Boolean && (Boolean)value) {
            span.setSamplingPriority(-1);
        } else if (value instanceof String && Boolean.parseBoolean((String)value)) {
            span.setSamplingPriority(-1);
        }

        return false;
    }
}
