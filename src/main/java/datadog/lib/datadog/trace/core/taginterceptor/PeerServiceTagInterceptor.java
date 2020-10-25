package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.core.ExclusiveSpan;

class PeerServiceTagInterceptor extends AbstractTagInterceptor {
    public PeerServiceTagInterceptor() {
        super("peer.service");
    }

    public boolean shouldSetTag(ExclusiveSpan span, String tag, Object value) {
        span.setServiceName(String.valueOf(value));
        return false;
    }
}
