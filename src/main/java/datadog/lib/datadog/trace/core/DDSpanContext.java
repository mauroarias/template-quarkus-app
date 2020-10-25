package datadog.lib.datadog.trace.core;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.lib.datadog.trace.core.ExclusiveSpan.Consumer;
import datadog.lib.datadog.trace.core.taginterceptor.AbstractTagInterceptor;
import datadog.trace.api.DDId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DDSpanContext implements Context {
    private static final Logger log = LoggerFactory.getLogger(DDSpanContext.class);
    public static final String PRIORITY_SAMPLING_KEY = "_sampling_priority_v1";
    public static final String SAMPLE_RATE_KEY = "_sample_rate";
    public static final String ORIGIN_KEY = "_dd.origin";
    private static final Map<CharSequence, Number> EMPTY_METRICS = Collections.emptyMap();
    private final CoreTracer tracer;
    private final PendingTrace trace;
    private final Map<String, String> baggageItems;
    private final DDId traceId;
    private final DDId spanId;
    private final DDId parentId;
    private final Map<String, Object> unsafeTags;
    private volatile String serviceName;
    private volatile CharSequence resourceName;
    private volatile CharSequence operationName;
    private volatile CharSequence spanType;
    private volatile boolean errorFlag;
    private boolean samplingPriorityLocked = false;
    private final String origin;
    private final AtomicReference<Map<CharSequence, Number>> metrics = new AtomicReference();
    private final Map<String, String> serviceNameMappings;
    private final ExclusiveSpan exclusiveSpan;

    public DDSpanContext(DDId traceId, DDId spanId, DDId parentId, String serviceName, CharSequence operationName, CharSequence resourceName, int samplingPriority, String origin, Map<String, String> baggageItems, boolean errorFlag, CharSequence spanType, int tagsSize, PendingTrace trace, CoreTracer tracer, Map<String, String> serviceNameMappings) {
        assert tracer != null;

        assert trace != null;

        this.tracer = tracer;
        this.trace = trace;

        assert traceId != null;

        assert spanId != null;

        assert parentId != null;

        this.traceId = traceId;
        this.spanId = spanId;
        this.parentId = parentId;
        if (baggageItems == null) {
            this.baggageItems = new ConcurrentHashMap(0);
        } else {
            this.baggageItems = new ConcurrentHashMap(baggageItems);
        }

        int capacity = (tagsSize <= 0 ? 3 : tagsSize + 3) * 4 / 3;
        this.unsafeTags = new HashMap(capacity);
        this.serviceNameMappings = serviceNameMappings;
        this.setServiceName(serviceName);
        this.operationName = operationName;
        this.resourceName = resourceName;
        this.errorFlag = errorFlag;
        this.spanType = spanType;
        this.origin = origin;
        if (samplingPriority != -2147483648) {
            this.setSamplingPriority(samplingPriority);
        }

        if (origin != null) {
            this.unsafeTags.put("_dd.origin", origin);
        }

        Thread current = Thread.currentThread();
        this.unsafeTags.put("thread.name", current.getName());
        this.unsafeTags.put("thread.id", current.getId());
        this.exclusiveSpan = new ExclusiveSpan(this);
    }

    public DDId getTraceId() {
        return this.traceId;
    }

    public DDId getParentId() {
        return this.parentId;
    }

    public DDId getSpanId() {
        return this.spanId;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        String mappedServiceName = (String)this.serviceNameMappings.get(serviceName);
        this.serviceName = mappedServiceName == null ? serviceName : mappedServiceName;
    }

    public CharSequence getResourceName() {
        return this.isResourceNameSet() ? this.resourceName : this.operationName;
    }

    public boolean isResourceNameSet() {
        return this.resourceName != null && this.resourceName.length() != 0;
    }

    public boolean hasResourceName() {
        return this.isResourceNameSet() || this.getTag("resource.name") != null;
    }

    public void setResourceName(CharSequence resourceName) {
        this.resourceName = resourceName;
    }

    public CharSequence getOperationName() {
        return this.operationName;
    }

    public void setOperationName(CharSequence operationName) {
        this.operationName = operationName;
    }

    public boolean getErrorFlag() {
        return this.errorFlag;
    }

    public void setErrorFlag(boolean errorFlag) {
        this.errorFlag = errorFlag;
    }

    public CharSequence getSpanType() {
        return this.spanType;
    }

    public void setSpanType(CharSequence spanType) {
        this.spanType = spanType;
    }

    public boolean setSamplingPriority(int newPriority) {
        if (newPriority == -2147483648) {
            log.debug("{}: Refusing to set samplingPriority to UNSET", this);
            return false;
        } else {
            if (this.trace != null) {
                DDSpan rootSpan = this.trace.getRootSpan();
                if (null != rootSpan && rootSpan.context() != this) {
                    return rootSpan.context().setSamplingPriority(newPriority);
                }
            }

            synchronized(this) {
                if (this.samplingPriorityLocked) {
                    log.debug("samplingPriority locked at {}. Refusing to set to {}", this.getMetrics().get("_sampling_priority_v1"), newPriority);
                    return false;
                } else {
                    this.setMetric("_sampling_priority_v1", newPriority);
                    log.debug("Set sampling priority to {}", this.getMetrics().get("_sampling_priority_v1"));
                    return true;
                }
            }
        }
    }

    public int getSamplingPriority() {
        DDSpan rootSpan = this.trace.getRootSpan();
        if (null != rootSpan && rootSpan.context() != this) {
            return rootSpan.context().getSamplingPriority();
        } else {
            Number val = (Number)this.getMetrics().get("_sampling_priority_v1");
            return null == val ? -2147483648 : val.intValue();
        }
    }

    public boolean lockSamplingPriority() {
        DDSpan rootSpan = this.trace.getRootSpan();
        if (null != rootSpan && rootSpan.context() != this) {
            return rootSpan.context().lockSamplingPriority();
        } else {
            synchronized(this) {
                if (this.getMetrics().get("_sampling_priority_v1") == null) {
                    log.debug("{} : refusing to lock unset samplingPriority", this);
                } else if (!this.samplingPriorityLocked) {
                    this.samplingPriorityLocked = true;
                    log.debug("{} : locked samplingPriority to {}", this, this.getMetrics().get("_sampling_priority_v1"));
                }

                return this.samplingPriorityLocked;
            }
        }
    }

    public String getOrigin() {
        DDSpan rootSpan = this.trace.getRootSpan();
        return null != rootSpan ? rootSpan.context().origin : this.origin;
    }

    public void setBaggageItem(String key, String value) {
        this.baggageItems.put(key, value);
    }

    public String getBaggageItem(String key) {
        return (String)this.baggageItems.get(key);
    }

    public Map<String, String> getBaggageItems() {
        return this.baggageItems;
    }

    public Iterable<Entry<String, String>> baggageItems() {
        return this.baggageItems.entrySet();
    }

    public PendingTrace getTrace() {
        return this.trace;
    }

    /** @deprecated */
    @Deprecated
    public CoreTracer getTracer() {
        return this.tracer;
    }

    public Map<CharSequence, Number> getMetrics() {
        Map<CharSequence, Number> metrics = (Map)this.metrics.get();
        return metrics == null ? EMPTY_METRICS : metrics;
    }

    public void setMetric(CharSequence key, Number value) {
        if (this.metrics.get() == null) {
            this.metrics.compareAndSet(null, new ConcurrentHashMap());
        }

        if (value instanceof Float) {
            ((Map)this.metrics.get()).put(key, value.doubleValue());
        } else {
            ((Map)this.metrics.get()).put(key, value);
        }

    }

    public void setTag(String tag, Object value) {
        byte var4 = -1;
        switch(tag.hashCode()) {
            case -688795810:
                if (tag.equals("span.type")) {
                    var4 = 0;
                }
            default:
                switch(var4) {
                    case 0:
                        if (value instanceof CharSequence) {
                            this.spanType = (CharSequence)value;
                        }
                        break;
                    default:
                        synchronized(this.unsafeTags) {
                            this.unsafeSetTag(tag, value);
                        }
                }

        }
    }

    void setAllTags(Map<String, ? extends Object> map) {
        if (map != null && !map.isEmpty()) {
            synchronized(this.unsafeTags) {
                Iterator var3 = map.entrySet().iterator();

                while(var3.hasNext()) {
                    Entry<String, ? extends Object> tag = (Entry)var3.next();
                    this.unsafeSetTag((String)tag.getKey(), tag.getValue());
                }

            }
        }
    }

    void unsafeSetTag(String tag, Object value) {
        if (value != null && (!(value instanceof String) || !((String)value).isEmpty())) {
            boolean addTag = true;
            List<AbstractTagInterceptor> interceptors = this.tracer.getSpanTagInterceptors(tag);
            if (interceptors != null) {
                ExclusiveSpan span = this.exclusiveSpan;
                Iterator var6 = interceptors.iterator();

                while(var6.hasNext()) {
                    AbstractTagInterceptor interceptor = (AbstractTagInterceptor)var6.next();

                    try {
                        addTag &= interceptor.shouldSetTag(span, tag, value);
                    } catch (Throwable var9) {
                        log.debug("Could not intercept the span interceptor={}: {}", interceptor.getClass().getSimpleName(), var9.getMessage());
                    }
                }
            }

            if (addTag) {
                this.unsafeTags.put(tag, value);
            }

        } else {
            this.unsafeTags.remove(tag);
        }
    }

    Object getTag(String key) {
        synchronized(this.unsafeTags) {
            return this.unsafeGetTag(key);
        }
    }

    Object unsafeGetTag(String tag) {
        return this.unsafeTags.get(tag);
    }

    Object getAndRemoveTag(String tag) {
        synchronized(this.unsafeTags) {
            return this.unsafeGetAndRemoveTag(tag);
        }
    }

    Object unsafeGetAndRemoveTag(String tag) {
        return this.unsafeTags.remove(tag);
    }

    public Map<String, Object> getTags() {
        synchronized(this.unsafeTags) {
            return Collections.unmodifiableMap(new HashMap(this.unsafeTags));
        }
    }

    public void processTagsAndBaggage(TagsAndBaggageConsumer consumer) {
        synchronized(this.unsafeTags) {
            consumer.accept(this.unsafeTags, this.baggageItems);
        }
    }

    public void processExclusiveSpan(Consumer consumer) {
        synchronized(this.unsafeTags) {
            consumer.accept(this.exclusiveSpan);
        }
    }

    public String toString() {
        StringBuilder s = (new StringBuilder()).append("DDSpan [ t_id=").append(this.traceId).append(", s_id=").append(this.spanId).append(", p_id=").append(this.parentId).append(" ] trace=").append(this.getServiceName()).append("/").append(this.getOperationName()).append("/").append(this.getResourceName()).append(" metrics=").append(new TreeMap(this.getMetrics()));
        if (this.errorFlag) {
            s.append(" *errored*");
        }

        synchronized(this.unsafeTags) {
            s.append(" tags=").append(new TreeMap(this.unsafeTags));
        }

        return s.toString();
    }
}
