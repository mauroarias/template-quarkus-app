package datadog.lib.datadog.opentracing;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.api.interceptor.MutableSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tag;

import java.util.Map;
import java.util.Objects;

class OTSpan implements Span, MutableSpan {
    private final AgentSpan delegate;
    private final TypeConverter converter;
    private final LogHandler logHandler;

    OTSpan(AgentSpan delegate, TypeConverter converter, LogHandler logHandler) {
        this.delegate = delegate;
        this.converter = converter;
        this.logHandler = logHandler;
    }

    public SpanContext context() {
        return this.converter.toSpanContext(this.delegate.context());
    }

    public OTSpan setTag(String key, String value) {
        this.delegate.setTag(key, value);
        return this;
    }

    public OTSpan setTag(String key, boolean value) {
        this.delegate.setTag(key, value);
        return this;
    }

    public OTSpan setTag(String key, Number value) {
        this.delegate.setTag(key, value);
        return this;
    }

    public OTSpan setMetric(CharSequence metric, int value) {
        this.delegate.setMetric(metric, value);
        return this;
    }

    public OTSpan setMetric(CharSequence metric, long value) {
        this.delegate.setMetric(metric, value);
        return this;
    }

    public OTSpan setMetric(CharSequence metric, double value) {
        this.delegate.setMetric(metric, value);
        return this;
    }

    public Boolean isError() {
        return this.delegate.isError();
    }

    public MutableSpan setError(boolean value) {
        return this.delegate.setError(value);
    }

    public MutableSpan getRootSpan() {
        return this.delegate.getLocalRootSpan();
    }

    public MutableSpan getLocalRootSpan() {
        return this.delegate.getLocalRootSpan();
    }

    public <T> Span setTag(Tag<T> tag, T value) {
        this.delegate.setTag(tag.getKey(), value);
        return this;
    }

    public Span log(Map<String, ?> fields) {
        this.logHandler.log(fields, this.delegate);
        return this;
    }

    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        this.logHandler.log(timestampMicroseconds, fields, this.delegate);
        return this;
    }

    public Span log(String event) {
        this.logHandler.log(event, this.delegate);
        return this;
    }

    public Span log(long timestampMicroseconds, String event) {
        this.logHandler.log(timestampMicroseconds, event, this.delegate);
        return this;
    }

    public Span setBaggageItem(String key, String value) {
        this.delegate.setBaggageItem(key, value);
        return this;
    }

    public String getBaggageItem(String key) {
        return this.delegate.getBaggageItem(key);
    }

    public Span setOperationName(String operationName) {
        return this.setOperationName((CharSequence)UTF8BytesString.create(operationName));
    }

    public long getStartTime() {
        return this.delegate.getStartTime();
    }

    public long getDurationNano() {
        return this.delegate.getDurationNano();
    }

    public CharSequence getOperationName() {
        return this.delegate.getOperationName();
    }

    public OTSpan setOperationName(CharSequence operationName) {
        this.delegate.setOperationName(operationName);
        return this;
    }

    public String getServiceName() {
        return this.delegate.getServiceName();
    }

    public MutableSpan setServiceName(String serviceName) {
        return this.delegate.setServiceName(serviceName);
    }

    public CharSequence getResourceName() {
        return this.delegate.getResourceName();
    }

    public MutableSpan setResourceName(CharSequence resourceName) {
        return this.delegate.setResourceName(resourceName);
    }

    public Integer getSamplingPriority() {
        return this.delegate.getSamplingPriority();
    }

    public MutableSpan setSamplingPriority(int newPriority) {
        return this.delegate.setSamplingPriority(newPriority);
    }

    public String getSpanType() {
        return this.delegate.getSpanType();
    }

    public MutableSpan setSpanType(CharSequence type) {
        return this.delegate.setSpanType(type);
    }

    public Map<String, Object> getTags() {
        return this.delegate.getTags();
    }

    public void finish() {
        this.delegate.finish();
    }

    public void finish(long finishMicros) {
        this.delegate.finish(finishMicros);
    }

    public AgentSpan getDelegate() {
        return this.delegate;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            OTSpan otSpan = (OTSpan)o;
            return this.delegate.equals(otSpan.delegate);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.delegate});
    }
}
