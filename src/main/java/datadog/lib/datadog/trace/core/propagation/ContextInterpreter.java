package datadog.lib.datadog.trace.core.propagation;

import datadog.trace.api.DDId;
import datadog.lib.datadog.trace.api.Functions.LowerCase;
import datadog.lib.datadog.trace.api.cache.DDCache;
import datadog.lib.datadog.trace.api.cache.DDCaches;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation.KeyClassifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class ContextInterpreter implements KeyClassifier {
    protected final Map<String, String> taggedHeaders;
    protected DDId traceId;
    protected DDId spanId;
    protected int samplingPriority;
    protected Map<String, String> tags;
    protected Map<String, String> baggage;
    protected String origin;
    protected boolean valid;
    private static final DDCache<String, String> CACHE = DDCaches.newFixedSizeCache(64);

    protected String toLowerCase(String key) {
        return (String)CACHE.computeIfAbsent(key, LowerCase.INSTANCE);
    }

    protected ContextInterpreter(Map<String, String> taggedHeaders) {
        this.taggedHeaders = taggedHeaders;
        this.reset();
    }

    public ContextInterpreter reset() {
        this.traceId = DDId.ZERO;
        this.spanId = DDId.ZERO;
        this.samplingPriority = this.defaultSamplingPriority();
        this.origin = null;
        this.tags = Collections.emptyMap();
        this.baggage = Collections.emptyMap();
        this.valid = true;
        return this;
    }

    TagContext build() {
        if (this.valid) {
            if (!DDId.ZERO.equals(this.traceId)) {
                ExtractedContext context = new ExtractedContext(this.traceId, this.spanId, this.samplingPriority, this.origin, this.baggage, this.tags);
                context.lockSamplingPriority();
                return context;
            }

            if (this.origin != null || !this.tags.isEmpty()) {
                return new TagContext(this.origin, this.tags);
            }
        }

        return null;
    }

    protected void invalidateContext() {
        this.valid = false;
    }

    protected int defaultSamplingPriority() {
        return -2147483648;
    }

    public abstract static class Factory {
        public Factory() {
        }

        public ContextInterpreter create(Map<String, String> tagsMapping) {
            return this.construct(this.cleanMapping(tagsMapping));
        }

        protected abstract ContextInterpreter construct(Map<String, String> var1);

        protected Map<String, String> cleanMapping(Map<String, String> taggedHeaders) {
            Map<String, String> cleanedMapping = new HashMap(taggedHeaders.size() * 4 / 3);
            Iterator var3 = taggedHeaders.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry<String, String> association = (Map.Entry)var3.next();
                cleanedMapping.put(((String)association.getKey()).trim().toLowerCase(), ((String)association.getValue()).trim().toLowerCase());
            }

            return cleanedMapping;
        }
    }
}
