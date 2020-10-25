package datadog.lib.datadog.trace.core;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.core.util.Clock;
import datadog.trace.api.DDId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DDSpan implements AgentSpan, DDSpanData {
    private static final Logger log = LoggerFactory.getLogger(DDSpan.class);
    private final DDSpanContext context;
    private final long startTimeMicro;
    private final long startTimeNano;
    private final AtomicLong durationNano = new AtomicLong();

    static DDSpan create(long timestampMicro, DDSpanContext context) {
        DDSpan span = new DDSpan(timestampMicro, context);
        log.debug("Started span: {}", span);
        context.getTrace().registerSpan(span);
        return span;
    }

    private DDSpan(long timestampMicro, DDSpanContext context) {
        this.context = context;
        if (timestampMicro <= 0L) {
            this.startTimeMicro = Clock.currentMicroTime();
            this.startTimeNano = context.getTrace().getCurrentTimeNano();
        } else {
            this.startTimeMicro = timestampMicro;
            this.startTimeNano = 0L;
            context.getTrace().touch();
        }

    }

    public boolean isFinished() {
        return this.durationNano.get() != 0L;
    }

    private void finishAndAddToTrace(long durationNano) {
        if (this.durationNano.compareAndSet(0L, Math.max(1L, durationNano))) {
            log.debug("Finished span: {}", this);
            this.context.getTrace().addFinishedSpan(this);
        } else {
            log.debug("Already finished: {}", this);
        }

    }

    public final void finish() {
        if (this.startTimeNano > 0L) {
            this.finishAndAddToTrace(this.context.getTrace().getCurrentTimeNano() - this.startTimeNano);
        } else {
            this.finish(Clock.currentMicroTime());
        }

    }

    public final void finish(long stoptimeMicros) {
        this.context.getTrace().touch();
        this.finishAndAddToTrace(TimeUnit.MICROSECONDS.toNanos(stoptimeMicros - this.startTimeMicro));
    }

    public DDSpan setError(boolean error) {
        this.context.setErrorFlag(error);
        return this;
    }

    public final boolean isRootSpan() {
        return DDId.ZERO.equals(this.context.getParentId());
    }

    /** @deprecated */
    @Deprecated
    public AgentSpan getRootSpan() {
        return this.getLocalRootSpan();
    }

    public DDSpan getLocalRootSpan() {
        return this.context.getTrace().getRootSpan();
    }

    public boolean isSameTrace(AgentSpan otherSpan) {
        return otherSpan instanceof DDSpan ? this.getTraceId().equals(otherSpan.getTraceId()) : false;
    }

    public AgentSpan setErrorMessage(String errorMessage) {
        return this.setTag("error.msg", errorMessage);
    }

    public AgentSpan addThrowable(Throwable error) {
        this.setError(true);
        this.setTag("error.msg", error.getMessage());
        this.setTag("error.type", error.getClass().getName());
        StringWriter errorString = new StringWriter();
        error.printStackTrace(new PrintWriter(errorString));
        this.setTag("error.stack", errorString.toString());
        return this;
    }

    public final DDSpan setTag(String tag, String value) {
        this.context.setTag(tag, value);
        return this;
    }

    public final DDSpan setTag(String tag, boolean value) {
        this.context.setTag(tag, value);
        return this;
    }

    public AgentSpan setTag(String tag, int value) {
        this.context.setTag(tag, value);
        return this;
    }

    public AgentSpan setTag(String tag, long value) {
        this.context.setTag(tag, value);
        return this;
    }

    public AgentSpan setTag(String tag, double value) {
        this.context.setTag(tag, value);
        return this;
    }

    public DDSpan setTag(String tag, Number value) {
        this.context.setTag(tag, value);
        return this;
    }

    public DDSpan setMetric(CharSequence metric, int value) {
        this.context.setMetric(metric, value);
        return this;
    }

    public DDSpan setMetric(CharSequence metric, long value) {
        this.context.setMetric(metric, value);
        return this;
    }

    public DDSpan setMetric(CharSequence metric, double value) {
        this.context.setMetric(metric, value);
        return this;
    }

    public DDSpan setTag(String tag, CharSequence value) {
        this.context.setTag(tag, value);
        return this;
    }

    public DDSpan setTag(String tag, Object value) {
        this.context.setTag(tag, value);
        return this;
    }

    public AgentSpan removeTag(String tag) {
        this.context.setTag(tag, (Object)null);
        return this;
    }

    public Object getAndRemoveTag(String tag) {
        return this.context.getAndRemoveTag(tag);
    }

    public Object getTag(String tag) {
        return this.context.getTag(tag);
    }

    public final DDSpanContext context() {
        return this.context;
    }

    public final String getBaggageItem(String key) {
        return this.context.getBaggageItem(key);
    }

    public final DDSpan setBaggageItem(String key, String value) {
        this.context.setBaggageItem(key, value);
        return this;
    }

    public final DDSpan setOperationName(CharSequence operationName) {
        this.context.setOperationName(operationName);
        return this;
    }

    public final DDSpan setServiceName(String serviceName) {
        this.context.setServiceName(serviceName);
        return this;
    }

    public final DDSpan setResourceName(CharSequence resourceName) {
        this.context.setResourceName(resourceName);
        return this;
    }

    public final DDSpan setSamplingPriority(int newPriority) {
        this.context.setSamplingPriority(newPriority);
        return this;
    }

    public final DDSpan setSpanType(CharSequence type) {
        this.context.setSpanType(type);
        return this;
    }

    public Map<CharSequence, Number> getMetrics() {
        return this.context.getMetrics();
    }

    public long getStartTime() {
        return this.startTimeNano > 0L ? this.startTimeNano : TimeUnit.MICROSECONDS.toNanos(this.startTimeMicro);
    }

    public long getDurationNano() {
        return this.durationNano.get();
    }

    public String getServiceName() {
        return this.context.getServiceName();
    }

    public DDId getTraceId() {
        return this.context.getTraceId();
    }

    public DDId getSpanId() {
        return this.context.getSpanId();
    }

    public DDId getParentId() {
        return this.context.getParentId();
    }

    public CharSequence getResourceName() {
        return this.context.getResourceName();
    }

    public CharSequence getOperationName() {
        return this.context.getOperationName();
    }

    public CharSequence getSpanName() {
        return this.context.getOperationName();
    }

    public void setSpanName(CharSequence spanName) {
        this.context.setOperationName(spanName);
    }

    public boolean hasResourceName() {
        return this.context.hasResourceName();
    }

    public Integer getSamplingPriority() {
        int samplingPriority = this.context.getSamplingPriority();
        return samplingPriority == -2147483648 ? null : samplingPriority;
    }

    public String getSpanType() {
        CharSequence spanType = this.context.getSpanType();
        return null == spanType ? null : spanType.toString();
    }

    public Map<String, Object> getTags() {
        return this.context.getTags();
    }

    public CharSequence getType() {
        return this.context.getSpanType();
    }

    public void processTagsAndBaggage(TagsAndBaggageConsumer consumer) {
        this.context.processTagsAndBaggage(consumer);
    }

    public Boolean isError() {
        return this.context.getErrorFlag();
    }

    public int getError() {
        return this.context.getErrorFlag() ? 1 : 0;
    }

    public Map<String, String> getBaggage() {
        return Collections.unmodifiableMap(this.context.getBaggageItems());
    }

    public String toString() {
        return this.context.toString() + ", duration_ns=" + this.durationNano;
    }
}
