package datadog.lib.datadog.opentracing;

import com.timgroup.statsd.StatsDClient;
import datadog.lib.datadog.trace.api.Config;
import datadog.trace.api.GlobalTracer;
import datadog.trace.api.interceptor.TraceInterceptor;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.TracerAPI;
import datadog.lib.datadog.trace.common.sampling.Sampler;
import datadog.lib.datadog.trace.common.writer.Writer;
import datadog.trace.context.ScopeListener;
import datadog.lib.datadog.trace.core.CoreTracer;
import datadog.lib.datadog.trace.core.CoreTracer.CoreTracerBuilder;
import datadog.lib.datadog.trace.core.DDSpanContext;
import datadog.lib.datadog.trace.core.propagation.ExtractedContext;
import datadog.lib.datadog.trace.core.propagation.HttpCodec.Extractor;
import datadog.lib.datadog.trace.core.propagation.HttpCodec.Injector;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation.KeyClassifier;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation.ContextVisitor;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation.Setter;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtract;
import io.opentracing.propagation.TextMapInject;
import io.opentracing.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class DDTracer implements Tracer, datadog.trace.api.Tracer {
    private static final Logger log = LoggerFactory.getLogger(DDTracer.class);
    private final TypeConverter converter;
    private final TracerAPI tracer;
    private ScopeManager scopeManager;

    /** @deprecated */
    @Deprecated
    public DDTracer() {
        this((TracerAPI)CoreTracer.builder().build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(String serviceName) {
        this((TracerAPI)CoreTracer.builder().serviceName(serviceName).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(Properties properties) {
        this((TracerAPI)CoreTracer.builder().withProperties(properties).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(Config config) {
        this((TracerAPI)CoreTracer.builder().config(config).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(String serviceName, Writer writer, Sampler sampler) {
        this((TracerAPI)CoreTracer.builder().serviceName(serviceName).writer(writer).sampler(sampler).build());
    }

    /** @deprecated */
    @Deprecated
    DDTracer(String serviceName, Writer writer, Sampler sampler, Map<String, String> runtimeTags) {
        this((TracerAPI)CoreTracer.builder().serviceName(serviceName).writer(writer).sampler(sampler).localRootSpanTags(runtimeTags).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(Writer writer) {
        this((TracerAPI)CoreTracer.builder().writer(writer).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(Config config, Writer writer) {
        this((TracerAPI)CoreTracer.builder().config(config).writer(writer).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(String serviceName, Writer writer, Sampler sampler, String runtimeId, Map<String, String> localRootSpanTags, Map<String, String> defaultSpanTags, Map<String, String> serviceNameMappings, Map<String, String> taggedHeaders) {
        this((TracerAPI)CoreTracer.builder().serviceName(serviceName).writer(writer).sampler(sampler).localRootSpanTags(customRuntimeTags(runtimeId, localRootSpanTags)).defaultSpanTags(defaultSpanTags).serviceNameMappings(serviceNameMappings).taggedHeaders(taggedHeaders).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(String serviceName, Writer writer, Sampler sampler, Map<String, String> localRootSpanTags, Map<String, String> defaultSpanTags, Map<String, String> serviceNameMappings, Map<String, String> taggedHeaders) {
        this((TracerAPI)CoreTracer.builder().serviceName(serviceName).writer(writer).sampler(sampler).localRootSpanTags(localRootSpanTags).defaultSpanTags(defaultSpanTags).serviceNameMappings(serviceNameMappings).taggedHeaders(taggedHeaders).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(String serviceName, Writer writer, Sampler sampler, Map<String, String> localRootSpanTags, Map<String, String> defaultSpanTags, Map<String, String> serviceNameMappings, Map<String, String> taggedHeaders, int partialFlushMinSpans) {
        this((TracerAPI)CoreTracer.builder().serviceName(serviceName).writer(writer).sampler(sampler).localRootSpanTags(localRootSpanTags).defaultSpanTags(defaultSpanTags).serviceNameMappings(serviceNameMappings).taggedHeaders(taggedHeaders).partialFlushMinSpans(partialFlushMinSpans).build());
    }

    /** @deprecated */
    @Deprecated
    public DDTracer(TracerAPI tracer) {
        this.tracer = tracer;
        this.converter = new TypeConverter(new DefaultLogHandler());
        this.scopeManager = new OTScopeManager(tracer, this.converter);
    }

    private DDTracer(@Deprecated Config config, String serviceName, Writer writer, Sampler sampler, Injector injector, Extractor extractor, ScopeManager scopeManager, Map<String, String> localRootSpanTags, Map<String, String> defaultSpanTags, Map<String, String> serviceNameMappings, Map<String, String> taggedHeaders, int partialFlushMinSpans, LogHandler logHandler, StatsDClient statsDClient) {
        if (GlobalTracer.get().getClass().getName().equals("datadog.lib.datadog.trace.agent.core.CoreTracer")) {
            log.error("Datadog Tracer already installed by `dd-java-agent`. NOTE: Manually creating the tracer while using `dd-java-agent` is not supported");
            throw new IllegalStateException("Datadog Tracer already installed");
        } else {
            if (logHandler != null) {
                this.converter = new TypeConverter(logHandler);
            } else {
                this.converter = new TypeConverter(new DefaultLogHandler());
            }

            CoreTracerBuilder builder = CoreTracer.builder();
            if (config != null) {
                builder = builder.config(config);
            }

            if (serviceName != null) {
                builder = builder.serviceName(serviceName);
            }

            if (writer != null) {
                builder = builder.writer(writer);
            }

            if (sampler != null) {
                builder = builder.sampler(sampler);
            }

            if (injector != null) {
                builder = builder.injector(injector);
            }

            if (extractor != null) {
                builder = builder.extractor(extractor);
            }

            if (scopeManager != null) {
                this.scopeManager = scopeManager;
                builder = builder.scopeManager(new CustomScopeManagerWrapper(scopeManager, this.converter));
            }

            if (localRootSpanTags != null) {
                builder = builder.localRootSpanTags(localRootSpanTags);
            }

            if (defaultSpanTags != null) {
                builder = builder.defaultSpanTags(defaultSpanTags);
            }

            if (serviceNameMappings != null) {
                builder = builder.serviceNameMappings(serviceNameMappings);
            }

            if (taggedHeaders != null) {
                builder = builder.taggedHeaders(taggedHeaders);
            }

            if (partialFlushMinSpans != 0) {
                builder = builder.partialFlushMinSpans(partialFlushMinSpans);
            }

            if (statsDClient != null) {
                builder = builder.statsDClient(statsDClient);
            }

            this.tracer = builder.build();
            if (scopeManager == null) {
                this.scopeManager = new OTScopeManager(this.tracer, this.converter);
            }

        }
    }

    private static Map<String, String> customRuntimeTags(String runtimeId, Map<String, String> applicationRootSpanTags) {
        Map<String, String> runtimeTags = new HashMap(applicationRootSpanTags);
        runtimeTags.put("runtime-id", runtimeId);
        return Collections.unmodifiableMap(runtimeTags);
    }

    public String getTraceId() {
        return this.tracer.getTraceId();
    }

    public String getSpanId() {
        return this.tracer.getSpanId();
    }

    public boolean addTraceInterceptor(TraceInterceptor traceInterceptor) {
        return this.tracer.addTraceInterceptor(traceInterceptor);
    }

    public void addScopeListener(ScopeListener listener) {
        this.tracer.addScopeListener(listener);
    }

    public ScopeManager scopeManager() {
        return this.scopeManager;
    }

    public Span activeSpan() {
        return this.scopeManager.activeSpan();
    }

    public Scope activateSpan(Span span) {
        return this.scopeManager.activate(span);
    }

    public DDTracer.DDSpanBuilder buildSpan(String operationName) {
        return new DDTracer.DDSpanBuilder(operationName);
    }

    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (carrier instanceof TextMapInject) {
            Context context = this.converter.toContext(spanContext);
            this.tracer.inject(context, (TextMapInject)carrier, DDTracer.TextMapInjectSetter.INSTANCE);
        } else {
            log.debug("Unsupported format for propagation - {}", format.getClass().getName());
        }

    }

    public <C> SpanContext extract(Format<C> format, C carrier) {
        if (carrier instanceof TextMapExtract) {
            Context tagContext = this.tracer.extract((TextMapExtract)carrier, new DDTracer.TextMapExtractGetter((TextMapExtract)carrier));
            return this.converter.toSpanContext(tagContext);
        } else {
            log.debug("Unsupported format for propagation - {}", format.getClass().getName());
            return null;
        }
    }

    public void close() {
        this.tracer.close();
    }

    public static DDTracer.DDTracerBuilder builder() {
        return new DDTracer.DDTracerBuilder();
    }

    public class DDSpanBuilder implements SpanBuilder {
        private final AgentTracer.SpanBuilder delegate;

        public DDSpanBuilder(String operationName) {
            this.delegate = DDTracer.this.tracer.buildSpan(operationName);
        }

        public DDTracer.DDSpanBuilder asChildOf(SpanContext parent) {
            this.delegate.asChildOf(DDTracer.this.converter.toContext(parent));
            return this;
        }

        public DDTracer.DDSpanBuilder asChildOf(Span parent) {
            if (parent != null) {
                this.delegate.asChildOf(DDTracer.this.converter.toAgentSpan(parent).context());
            }

            return this;
        }

        public DDTracer.DDSpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            if (referencedContext == null) {
                return this;
            } else {
                Context context = DDTracer.this.converter.toContext(referencedContext);
                if (!(context instanceof ExtractedContext) && !(context instanceof DDSpanContext)) {
                    DDTracer.log.debug("Expected to have a DDSpanContext or ExtractedContext but got " + context.getClass().getName());
                    return this;
                } else {
                    if (!"child_of".equals(referenceType) && !"follows_from".equals(referenceType)) {
                        DDTracer.log.debug("Only support reference type of CHILD_OF and FOLLOWS_FROM");
                    } else {
                        this.delegate.asChildOf(context);
                    }

                    return this;
                }
            }
        }

        public DDTracer.DDSpanBuilder ignoreActiveSpan() {
            this.delegate.ignoreActiveSpan();
            return this;
        }

        public DDSpanBuilder withTag(String key, String value) {
            this.delegate.withTag(key, value);
            return this;
        }

        public DDSpanBuilder withTag(String key, boolean value) {
            this.delegate.withTag(key, value);
            return this;
        }

        public DDSpanBuilder withTag(String key, Number value) {
            this.delegate.withTag(key, value);
            return this;
        }

        public <T> DDSpanBuilder withTag(Tag<T> tag, T value) {
            this.delegate.withTag(tag.getKey(), value);
            return this;
        }

        public DDSpanBuilder withStartTimestamp(long microseconds) {
            this.delegate.withStartTimestamp(microseconds);
            return this;
        }

        public Span startManual() {
            return this.start();
        }

        public Span start() {
            AgentSpan agentSpan = this.delegate.start();
            return DDTracer.this.converter.toSpan(agentSpan);
        }

        /** @deprecated */
        @Deprecated
        public Scope startActive(boolean finishSpanOnClose) {
            return DDTracer.this.scopeManager.activate(this.start(), finishSpanOnClose);
        }

        public DDSpanBuilder withServiceName(String serviceName) {
            this.delegate.withServiceName(serviceName);
            return this;
        }

        public DDSpanBuilder withResourceName(String resourceName) {
            this.delegate.withResourceName(resourceName);
            return this;
        }

        public DDSpanBuilder withErrorFlag() {
            this.delegate.withErrorFlag();
            return this;
        }

        public DDSpanBuilder withSpanType(String spanType) {
            this.delegate.withSpanType(spanType);
            return this;
        }
    }

    private static class TextMapExtractGetter implements ContextVisitor<TextMapExtract> {
        private final TextMapExtract carrier;

        private TextMapExtractGetter(TextMapExtract carrier) {
            this.carrier = carrier;
        }

        public void forEachKey(TextMapExtract ignored, KeyClassifier classifier) {
            Iterator var3 = this.carrier.iterator();

            Map.Entry entry;
            do {
                if (!var3.hasNext()) {
                    return;
                }

                entry = (Map.Entry)var3.next();
            } while(classifier.accept((String)entry.getKey(), (String)entry.getValue()));

        }
    }

    private static class TextMapInjectSetter implements Setter<TextMapInject> {
        static final TextMapInjectSetter INSTANCE = new TextMapInjectSetter();

        private TextMapInjectSetter() {
        }

        public void set(TextMapInject carrier, String key, String value) {
            carrier.put(key, value);
        }
    }

    public static class DDTracerBuilder {
        private Config config;
        private String serviceName;
        private Writer writer;
        private Sampler sampler;
        private Injector injector;
        private Extractor extractor;
        private ScopeManager scopeManager;
        private Map<String, String> localRootSpanTags;
        private Map<String, String> defaultSpanTags;
        private Map<String, String> serviceNameMappings;
        private Map<String, String> taggedHeaders;
        private int partialFlushMinSpans;
        private LogHandler logHandler;
        private StatsDClient statsDClient;

        public DDTracerBuilder withProperties(Properties properties) {
            return this.config(Config.get(properties));
        }

        DDTracerBuilder() {
        }

        /** @deprecated */
        @Deprecated
        public DDTracerBuilder config(Config config) {
            this.config = config;
            return this;
        }

        public DDTracerBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public DDTracerBuilder writer(Writer writer) {
            this.writer = writer;
            return this;
        }

        public DDTracerBuilder sampler(Sampler sampler) {
            this.sampler = sampler;
            return this;
        }

        public DDTracerBuilder injector(Injector injector) {
            this.injector = injector;
            return this;
        }

        public DDTracerBuilder extractor(Extractor extractor) {
            this.extractor = extractor;
            return this;
        }

        public DDTracerBuilder scopeManager(ScopeManager scopeManager) {
            this.scopeManager = scopeManager;
            return this;
        }

        public DDTracerBuilder localRootSpanTags(Map<String, String> localRootSpanTags) {
            this.localRootSpanTags = localRootSpanTags;
            return this;
        }

        public DDTracerBuilder defaultSpanTags(Map<String, String> defaultSpanTags) {
            this.defaultSpanTags = defaultSpanTags;
            return this;
        }

        public DDTracerBuilder serviceNameMappings(Map<String, String> serviceNameMappings) {
            this.serviceNameMappings = serviceNameMappings;
            return this;
        }

        public DDTracerBuilder taggedHeaders(Map<String, String> taggedHeaders) {
            this.taggedHeaders = taggedHeaders;
            return this;
        }

        public DDTracerBuilder partialFlushMinSpans(int partialFlushMinSpans) {
            this.partialFlushMinSpans = partialFlushMinSpans;
            return this;
        }

        public DDTracerBuilder logHandler(LogHandler logHandler) {
            this.logHandler = logHandler;
            return this;
        }

        public DDTracerBuilder statsDClient(StatsDClient statsDClient) {
            this.statsDClient = statsDClient;
            return this;
        }

        public DDTracer build() {
            return new DDTracer(this.config, this.serviceName, this.writer, this.sampler, this.injector, this.extractor, this.scopeManager, this.localRootSpanTags, this.defaultSpanTags, this.serviceNameMappings, this.taggedHeaders, this.partialFlushMinSpans, this.logHandler, this.statsDClient);
        }

        public String toString() {
            return "DDTracer.DDTracerBuilder(config=" + this.config + ", serviceName=" + this.serviceName + ", writer=" + this.writer + ", sampler=" + this.sampler + ", injector=" + this.injector + ", extractor=" + this.extractor + ", scopeManager=" + this.scopeManager + ", localRootSpanTags=" + this.localRootSpanTags + ", defaultSpanTags=" + this.defaultSpanTags + ", serviceNameMappings=" + this.serviceNameMappings + ", taggedHeaders=" + this.taggedHeaders + ", partialFlushMinSpans=" + this.partialFlushMinSpans + ", logHandler=" + this.logHandler + ", statsDClient=" + this.statsDClient + ")";
        }
    }
}
