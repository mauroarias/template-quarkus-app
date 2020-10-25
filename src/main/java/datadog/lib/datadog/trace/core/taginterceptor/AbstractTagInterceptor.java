package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.core.ExclusiveSpan;

public abstract class AbstractTagInterceptor {
    private final String matchingTag;

    protected AbstractTagInterceptor(String matchingTag) {
        this.matchingTag = matchingTag;
    }

    public abstract boolean shouldSetTag(ExclusiveSpan var1, String var2, Object var3);

    public String getMatchingTag() {
        return this.matchingTag;
    }
}
