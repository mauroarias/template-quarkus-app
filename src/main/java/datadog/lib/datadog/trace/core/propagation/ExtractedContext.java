package datadog.lib.datadog.trace.core.propagation;

import datadog.trace.api.DDId;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtractedContext extends TagContext {
    private final DDId traceId;
    private final DDId spanId;
    private final int samplingPriority;
    private final Map<String, String> baggage;
    private final AtomicBoolean samplingPriorityLocked = new AtomicBoolean(false);

    public ExtractedContext(DDId traceId, DDId spanId, int samplingPriority, String origin, Map<String, String> baggage, Map<String, String> tags) {
        super(origin, tags);
        this.traceId = traceId;
        this.spanId = spanId;
        this.samplingPriority = samplingPriority;
        this.baggage = baggage;
    }

    public Iterable<Entry<String, String>> baggageItems() {
        return this.baggage.entrySet();
    }

    public void lockSamplingPriority() {
        this.samplingPriorityLocked.set(true);
    }

    public DDId getTraceId() {
        return this.traceId;
    }

    public DDId getSpanId() {
        return this.spanId;
    }

    public int getSamplingPriority() {
        return this.samplingPriority;
    }

    public Map<String, String> getBaggage() {
        return this.baggage;
    }

    public boolean getSamplingPriorityLocked() {
        return this.samplingPriorityLocked.get();
    }
}
