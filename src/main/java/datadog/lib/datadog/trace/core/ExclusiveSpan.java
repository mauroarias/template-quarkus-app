package datadog.lib.datadog.trace.core;

public final class ExclusiveSpan {
    private final DDSpanContext context;

    ExclusiveSpan(DDSpanContext context) {
        this.context = context;
    }

    public Object getTag(String tag) {
        return this.context.unsafeGetTag(tag);
    }

    public void setTag(String tag, Object value) {
        this.context.unsafeSetTag(tag, value);
    }

    public Object getAndRemoveTag(String tag) {
        return this.context.unsafeGetAndRemoveTag(tag);
    }

    public void setMetric(String key, Number value) {
        this.context.setMetric(key, value);
    }

    public boolean isResourceNameSet() {
        return this.context.isResourceNameSet();
    }

    public void setResourceName(CharSequence resourceName) {
        this.context.setResourceName(resourceName);
    }

    public String getServiceName() {
        return this.context.getServiceName();
    }

    public void setServiceName(String serviceName) {
        this.context.setServiceName(serviceName);
    }

    public boolean isError() {
        return this.context.getErrorFlag();
    }

    public void setError(boolean error) {
        this.context.setErrorFlag(error);
    }

    public CharSequence getType() {
        return this.context.getSpanType();
    }

    public void setType(CharSequence type) {
        this.context.setSpanType(type);
    }

    public int getHttpStatus() {
        Object status = this.getTag("http.status_code");
        if (status instanceof Number) {
            return ((Number)status).intValue();
        } else {
            if (null != status) {
                try {
                    return Integer.parseInt(String.valueOf(status));
                } catch (NumberFormatException var3) {
                }
            }

            return 0;
        }
    }

    public boolean setSamplingPriority(int newPriority) {
        return this.context.setSamplingPriority(newPriority);
    }

    public abstract static class Consumer {
        public Consumer() {
        }

        public abstract void accept(ExclusiveSpan var1);
    }
}
