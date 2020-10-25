package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import io.opentracing.SpanContext;
import java.util.Objects;
import java.util.Map.Entry;

class OTSpanContext implements SpanContext {
    private final Context delegate;

    OTSpanContext(Context delegate) {
        this.delegate = delegate;
    }

    public String toTraceId() {
        return this.delegate.getTraceId().toString();
    }

    public String toSpanId() {
        return this.delegate.getSpanId().toString();
    }

    public Iterable<Entry<String, String>> baggageItems() {
        return this.delegate.baggageItems();
    }

    Context getDelegate() {
        return this.delegate;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            OTSpanContext that = (OTSpanContext)o;
            return this.delegate.equals(that.delegate);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.delegate});
    }
}
