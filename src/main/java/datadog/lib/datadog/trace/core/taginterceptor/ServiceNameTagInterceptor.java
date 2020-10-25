package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.core.ExclusiveSpan;

class ServiceNameTagInterceptor extends AbstractTagInterceptor {
    private final boolean setTag;

    public ServiceNameTagInterceptor() {
        this("service.name", false);
    }

    public ServiceNameTagInterceptor(String splitByTag, boolean setTag) {
        super(splitByTag);
        this.setTag = setTag;
    }

    public boolean shouldSetTag(ExclusiveSpan span, String tag, Object value) {
        span.setServiceName(String.valueOf(value));
        return this.setTag;
    }
}
