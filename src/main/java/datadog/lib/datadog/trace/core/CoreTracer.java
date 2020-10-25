package datadog.lib.datadog.trace.core;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import datadog.lib.datadog.trace.api.Config;
import datadog.trace.api.DDId;
import datadog.trace.api.IdGenerationStrategy;
import datadog.trace.api.interceptor.MutableSpan;
import datadog.trace.api.interceptor.TraceInterceptor;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentScopeManager;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentSpan.Context;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.NoopAgentSpan;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentTracer.TracerAPI;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.ScopeSource;
import datadog.lib.datadog.trace.common.sampling.PrioritySampler;
import datadog.lib.datadog.trace.common.sampling.Sampler;
import datadog.lib.datadog.trace.common.writer.Writer;
import datadog.lib.datadog.trace.common.writer.WriterFactory;
import datadog.trace.context.ScopeListener;
import datadog.trace.context.TraceScope;
import datadog.trace.context.TraceScope.Continuation;
import datadog.lib.datadog.trace.core.PendingTrace.Factory;
import datadog.lib.datadog.trace.core.jfr.DDNoopScopeEventFactory;
import datadog.lib.datadog.trace.core.jfr.DDScopeEventFactory;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import datadog.lib.datadog.trace.core.monitor.Recording;
import datadog.lib.datadog.trace.core.propagation.ExtractedContext;
import datadog.lib.datadog.trace.core.propagation.HttpCodec;
import datadog.lib.datadog.trace.core.propagation.HttpCodec.Extractor;
import datadog.lib.datadog.trace.core.propagation.HttpCodec.Injector;
import datadog.lib.datadog.trace.core.propagation.TagContext;
import datadog.lib.datadog.trace.core.scopemanager.ContinuableScopeManager;
import datadog.lib.datadog.trace.core.taginterceptor.AbstractTagInterceptor;
import datadog.lib.datadog.trace.core.taginterceptor.TagInterceptorsFactory;
import datadog.lib.datadog.trace.util.AgentThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class CoreTracer implements TracerAPI {
    private static final Logger log = LoggerFactory.getLogger(CoreTracer.class);
    public static final BigInteger TRACE_ID_MAX = BigInteger.valueOf(2L).pow(64).subtract(BigInteger.ONE);
    private static final String LANG_STATSD_TAG = "lang";
    private static final String LANG_VERSION_STATSD_TAG = "lang_version";
    private static final String LANG_INTERPRETER_STATSD_TAG = "lang_interpreter";
    private static final String LANG_INTERPRETER_VENDOR_STATSD_TAG = "lang_interpreter_vendor";
    private static final String TRACER_VERSION_STATSD_TAG = "tracer_version";
    final PendingTraceBuffer pendingTraceBuffer;
    final String serviceName;
    final Writer writer;
    final Sampler sampler;
    final AgentScopeManager scopeManager;
    private final Map<String, String> localRootSpanTags;
    private final Map<String, String> defaultSpanTags;
    private final Map<String, String> serviceNameMappings;
    private final int partialFlushMinSpans;
    private final StatsDClient statsDClient;
    private final Monitoring monitoring;
    private final Monitoring performanceMonitoring;
    private final Recording traceWriteTimer;
    private final IdGenerationStrategy idGenerationStrategy;
    private final Factory pendingTraceFactory;
    private final Thread shutdownCallback;
    private final Map<String, List<AbstractTagInterceptor>> spanTagInterceptors;
    private final SortedSet<TraceInterceptor> interceptors;
    private final Injector injector;
    private final Extractor extractor;

    public Continuation capture() {
        TraceScope activeScope = this.activeScope();
        return activeScope == null ? null : activeScope.capture();
    }

    private CoreTracer(Config config, String serviceName, Writer writer, IdGenerationStrategy idGenerationStrategy, Sampler sampler, Injector injector, Extractor extractor, AgentScopeManager scopeManager, Map<String, String> localRootSpanTags, Map<String, String> defaultSpanTags, Map<String, String> serviceNameMappings, Map<String, String> taggedHeaders, int partialFlushMinSpans, StatsDClient statsDClient) {
        pendingTraceBuffer = new PendingTraceBuffer();
        pendingTraceBuffer.start();

        this.spanTagInterceptors = new HashMap();
        this.interceptors = new ConcurrentSkipListSet(new Comparator<TraceInterceptor>() {
            public int compare(TraceInterceptor o1, TraceInterceptor o2) {
                return Integer.compare(o1.priority(), o2.priority());
            }
        });

        assert localRootSpanTags != null;

        assert defaultSpanTags != null;

        assert serviceNameMappings != null;

        assert taggedHeaders != null;

        this.serviceName = serviceName;
        this.sampler = sampler;
        this.injector = injector;
        this.extractor = extractor;
        this.localRootSpanTags = localRootSpanTags;
        this.defaultSpanTags = defaultSpanTags;
        this.serviceNameMappings = serviceNameMappings;
        this.partialFlushMinSpans = partialFlushMinSpans;
        this.idGenerationStrategy = null == idGenerationStrategy ? Config.get().getIdGenerationStrategy() : idGenerationStrategy;
        if (statsDClient == null) {
            this.statsDClient = createStatsDClient(config);
        } else {
            this.statsDClient = statsDClient;
        }

        this.monitoring = config.isHealthMetricsEnabled() ? new Monitoring(this.statsDClient, 10L, TimeUnit.SECONDS) : Monitoring.DISABLED;
        this.performanceMonitoring = config.isPerfMetricsEnabled() ? new Monitoring(this.statsDClient, 10L, TimeUnit.SECONDS) : Monitoring.DISABLED;
        this.traceWriteTimer = this.performanceMonitoring.newThreadLocalTimer("trace.write");
        if (scopeManager == null) {
            this.scopeManager = new ContinuableScopeManager(config.getScopeDepthLimit(), createScopeEventFactory(), this.statsDClient, config.isScopeStrictMode(), config.isScopeInheritAsyncPropagation());
        } else {
            this.scopeManager = scopeManager;
        }

        if (writer == null) {
            this.writer = WriterFactory.createWriter(config, sampler, this.statsDClient, this.monitoring);
        } else {
            this.writer = writer;
        }

        this.pendingTraceFactory = new Factory(this, pendingTraceBuffer);
        this.writer.start();
        this.shutdownCallback = new ShutdownHook(this);

        try {
            Runtime.getRuntime().addShutdownHook(this.shutdownCallback);
        } catch (IllegalStateException var18) {
        }

        List<AbstractTagInterceptor> tagInterceptors = TagInterceptorsFactory.createTagInterceptors();
        Iterator var16 = tagInterceptors.iterator();

        while(var16.hasNext()) {
            AbstractTagInterceptor interceptor = (AbstractTagInterceptor)var16.next();
            this.addTagInterceptor(interceptor);
        }

        this.registerClassLoader(ClassLoader.getSystemClassLoader());
        StatusLogger.logStatus(config);
    }

    public void finalize() {
        try {
            this.shutdownCallback.run();
            Runtime.getRuntime().removeShutdownHook(this.shutdownCallback);
        } catch (IllegalStateException var2) {
        } catch (Exception var3) {
            log.error("Error while finalizing DDTracer.", var3);
        }

    }

    public List<AbstractTagInterceptor> getSpanTagInterceptors(String tag) {
        return (List)this.spanTagInterceptors.get(tag);
    }

    private void addTagInterceptor(AbstractTagInterceptor interceptor) {
        List<AbstractTagInterceptor> list = (List)this.spanTagInterceptors.get(interceptor.getMatchingTag());
        if (list == null) {
            list = new ArrayList();
        }

        ((List)list).add(interceptor);
        this.spanTagInterceptors.put(interceptor.getMatchingTag(), list);
        log.debug("Decorator added: '{}' -> {}", interceptor.getMatchingTag(), interceptor.getClass().getName());
    }

    private void registerClassLoader(ClassLoader classLoader) {
        try {
            Iterator var2 = ServiceLoader.load(TraceInterceptor.class, classLoader).iterator();

            while(var2.hasNext()) {
                TraceInterceptor interceptor = (TraceInterceptor)var2.next();
                this.addTraceInterceptor(interceptor);
            }
        } catch (ServiceConfigurationError var4) {
            log.warn("Problem loading TraceInterceptor for classLoader: " + classLoader, var4);
        }

    }

    public CoreSpanBuilder buildSpan(CharSequence operationName) {
        return new CoreSpanBuilder(operationName);
    }

    public AgentSpan startSpan(CharSequence spanName) {
        return this.buildSpan(spanName).start();
    }

    public AgentSpan startSpan(CharSequence spanName, long startTimeMicros) {
        return this.buildSpan(spanName).withStartTimestamp(startTimeMicros).start();
    }

    public AgentSpan startSpan(CharSequence spanName, Context parent) {
        return this.buildSpan(spanName).ignoreActiveSpan().asChildOf(parent).start();
    }

    public AgentSpan startSpan(CharSequence spanName, Context parent, long startTimeMicros) {
        return this.buildSpan(spanName).ignoreActiveSpan().asChildOf(parent).withStartTimestamp(startTimeMicros).start();
    }

    public AgentScope activateSpan(AgentSpan span) {
        return this.scopeManager.activate(span, ScopeSource.INSTRUMENTATION, false);
    }

    public AgentScope activateSpan(AgentSpan span, ScopeSource source) {
        return this.scopeManager.activate(span, source);
    }

    public AgentScope activateSpan(AgentSpan span, ScopeSource source, boolean isAsyncPropagating) {
        return this.scopeManager.activate(span, source, isAsyncPropagating);
    }

    public AgentSpan activeSpan() {
        return this.scopeManager.activeSpan();
    }

    public TraceScope activeScope() {
        return this.scopeManager.active();
    }

    public AgentPropagation propagate() {
        return this;
    }

    public AgentSpan noopSpan() {
        return NoopAgentSpan.INSTANCE;
    }

    public <C> void inject(AgentSpan span, C carrier, Setter<C> setter) {
        this.inject(span.context(), carrier, setter);
    }

    public <C> void inject(Context context, C carrier, Setter<C> setter) {
        if (context instanceof DDSpanContext) {
            DDSpanContext ddSpanContext = (DDSpanContext)context;
            DDSpan rootSpan = ddSpanContext.getTrace().getRootSpan();
            this.setSamplingPriorityIfNecessary(rootSpan);
            this.injector.inject(ddSpanContext, carrier, setter);
        }
    }

    public <C> Context extract(C carrier, ContextVisitor<C> getter) {
        return extractor.extract(carrier, getter);
    }

    void write(List<DDSpan> trace) {
        if (!trace.isEmpty()) {
            List<DDSpan> writtenTrace = trace;
            if (!this.interceptors.isEmpty()) {
                Collection<? extends MutableSpan> interceptedTrace = new ArrayList(trace);

                Iterator var4;
                TraceInterceptor interceptor;
                for(var4 = this.interceptors.iterator(); var4.hasNext(); interceptedTrace = interceptor.onTraceComplete((Collection)interceptedTrace)) {
                    interceptor = (TraceInterceptor)var4.next();
                }

                writtenTrace = new ArrayList(((Collection)interceptedTrace).size());
                var4 = ((Collection)interceptedTrace).iterator();

                while(var4.hasNext()) {
                    MutableSpan span = (MutableSpan)var4.next();
                    if (span instanceof DDSpan) {
                        ((List)writtenTrace).add((DDSpan)span);
                    }
                }
            }

            if (!((List)writtenTrace).isEmpty()) {
                DDSpan rootSpan = ((DDSpan)((List)writtenTrace).get(0)).getLocalRootSpan();
                this.setSamplingPriorityIfNecessary(rootSpan);
                DDSpan spanToSample = rootSpan == null ? (DDSpan)((List)writtenTrace).get(0) : rootSpan;
                if (this.sampler.sample(spanToSample)) {
                    this.writer.write((List)writtenTrace);
                } else {
                    this.incrementTraceCount();
                }
            }

        }
    }

    void setSamplingPriorityIfNecessary(DDSpan rootSpan) {
        if (this.sampler instanceof PrioritySampler && rootSpan != null && rootSpan.context().getSamplingPriority() == -2147483648) {
            ((PrioritySampler)this.sampler).setSamplingPriority(rootSpan);
        }

    }

    void incrementTraceCount() {
        this.writer.incrementTraceCount();
    }

    public String getTraceId() {
        AgentSpan activeSpan = this.activeSpan();
        return activeSpan instanceof DDSpan ? ((DDSpan)activeSpan).getTraceId().toString() : "0";
    }

    public String getSpanId() {
        AgentSpan activeSpan = this.activeSpan();
        return activeSpan instanceof DDSpan ? ((DDSpan)activeSpan).getSpanId().toString() : "0";
    }

    public boolean addTraceInterceptor(TraceInterceptor interceptor) {
        return this.interceptors.add(interceptor);
    }

    public void addScopeListener(ScopeListener listener) {
        if (this.scopeManager instanceof ContinuableScopeManager) {
            ((ContinuableScopeManager)this.scopeManager).addScopeListener(listener);
        }

    }

    public void close() {
        this.writer.close();
    }

    public void flush() {
        pendingTraceBuffer.flush();
        this.writer.flush();
    }

    private static DDScopeEventFactory createScopeEventFactory() {
        try {
            return (DDScopeEventFactory)Class.forName("datadog.lib.datadog.trace.core.jfr.openjdk.ScopeEventFactory").newInstance();
        } catch (ReflectiveOperationException | NoClassDefFoundError | ClassFormatError var1) {
            log.debug("Profiling of ScopeEvents is not available");
            return new DDNoopScopeEventFactory();
        }
    }

    private static StatsDClient createStatsDClient(Config config) {
        if (!config.isHealthMetricsEnabled()) {
            return new NoOpStatsDClient();
        } else {
            String host = config.getHealthMetricsStatsdHost();
            if (host == null) {
                host = config.getJmxFetchStatsdHost();
            }

            if (host == null) {
                host = config.getAgentHost();
            }

            Integer port = config.getHealthMetricsStatsdPort();
            if (port == null) {
                port = config.getJmxFetchStatsdPort();
            }

            try {
                return new NonBlockingStatsDClient("datadog.lib.datadog.tracer", host, port, generateConstantTags(config));
            } catch (StatsDClientException var4) {
                log.error("Unable to create StatsD client", var4);
                return new NoOpStatsDClient();
            }
        }
    }

    private static String[] generateConstantTags(Config config) {
        List<String> constantTags = new ArrayList();
        constantTags.add(statsdTag("lang", "java"));
        constantTags.add(statsdTag("lang_version", DDTraceCoreInfo.JAVA_VERSION));
        constantTags.add(statsdTag("lang_interpreter", DDTraceCoreInfo.JAVA_VM_NAME));
        constantTags.add(statsdTag("lang_interpreter_vendor", DDTraceCoreInfo.JAVA_VM_VENDOR));
        constantTags.add(statsdTag("tracer_version", DDTraceCoreInfo.VERSION));
        constantTags.add(statsdTag("service", config.getServiceName()));
        Map<String, String> mergedSpanTags = config.getMergedSpanTags();
        String version = (String)mergedSpanTags.get("version");
        if (version != null && !version.isEmpty()) {
            constantTags.add(statsdTag("version", version));
        }

        String env = (String)mergedSpanTags.get("env");
        if (env != null && !env.isEmpty()) {
            constantTags.add(statsdTag("env", env));
        }

        return (String[])constantTags.toArray(new String[0]);
    }

    Recording writeTimer() {
        return this.traceWriteTimer.start();
    }

    private static String statsdTag(String tagPrefix, String tagValue) {
        return tagPrefix + ":" + tagValue;
    }

    public static CoreTracerBuilder builder() {
        return new CoreTracerBuilder();
    }

    public int getPartialFlushMinSpans() {
        return this.partialFlushMinSpans;
    }

    private static class ShutdownHook extends Thread {
        private final WeakReference<CoreTracer> reference;

        private ShutdownHook(CoreTracer tracer) {
            super(AgentThreadFactory.AGENT_THREAD_GROUP, "dd-tracer-shutdown-hook");
            this.reference = new WeakReference(tracer);
        }

        public void run() {
            CoreTracer tracer = (CoreTracer)this.reference.get();
            if (tracer != null) {
                tracer.close();
            }

        }
    }

    public class CoreSpanBuilder implements AgentTracer.SpanBuilder {
        private final CharSequence operationName;
        private Map<String, Object> tags;
        private long timestampMicro;
        private Object parent;
        private String serviceName;
        private String resourceName;
        private boolean errorFlag;
        private CharSequence spanType;
        private boolean ignoreScope = false;

        public CoreSpanBuilder(CharSequence operationName) {
            this.operationName = operationName;
        }

        public CoreSpanBuilder ignoreActiveSpan() {
            this.ignoreScope = true;
            return this;
        }

        private DDSpan buildSpan() {
            return DDSpan.create(this.timestampMicro, this.buildSpanContext());
        }

        public AgentSpan start() {
            AgentSpan span = this.buildSpan();
            return span;
        }

        public CoreSpanBuilder withTag(String tag, Number number) {
            return this.withTag(tag, (Object)number);
        }

        public CoreSpanBuilder withTag(String tag, String string) {
            return this.withTag(tag, (Object)string);
        }

        public CoreSpanBuilder withTag(String tag, boolean bool) {
            return this.withTag(tag, (Object)bool);
        }

        public CoreSpanBuilder withStartTimestamp(long timestampMicroseconds) {
            this.timestampMicro = timestampMicroseconds;
            return this;
        }

        public CoreSpanBuilder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public CoreSpanBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public CoreSpanBuilder withErrorFlag() {
            this.errorFlag = true;
            return this;
        }

        public CoreSpanBuilder withSpanType(CharSequence spanType) {
            this.spanType = spanType;
            return this;
        }

        public CoreSpanBuilder asChildOf(Context spanContext) {
            this.parent = spanContext;
            return this;
        }

        public CoreSpanBuilder asChildOf(AgentSpan agentSpan) {
            this.parent = agentSpan.context();
            return this;
        }

        public CoreTracer.CoreSpanBuilder withTag(String tag, Object value) {
            byte var4 = -1;
            switch(tag.hashCode()) {
                case -688795810:
                    if (tag.equals("span.type")) {
                        var4 = 0;
                    }
            }

            switch(var4) {
                case 0:
                    if (value instanceof CharSequence) {
                        return this.withSpanType((CharSequence)value);
                    }
                default:
                    Map<String, Object> tagMap = this.tags;
                    if (tagMap == null) {
                        this.tags = (Map)(tagMap = new LinkedHashMap());
                    }

                    if (value != null && (!(value instanceof String) || !((String)value).isEmpty())) {
                        ((Map)tagMap).put(tag, value);
                    } else {
                        ((Map)tagMap).remove(tag);
                    }

                    return this;
            }
        }

        private DDSpanContext buildSpanContext() {
            DDId spanId = CoreTracer.this.idGenerationStrategy.generate();
            Object parentContext = this.parent;
            if (parentContext == null && !this.ignoreScope) {
                AgentSpan activeSpan = CoreTracer.this.scopeManager.activeSpan();
                if (activeSpan != null) {
                    parentContext = activeSpan.context();
                }
            }

            DDId traceId;
            DDId parentSpanId;
            Map baggage;
            PendingTrace parentTrace;
            int samplingPriority;
            String origin;
            Map coreTags;
            Map rootSpanTags;
            if (parentContext instanceof DDSpanContext) {
                DDSpanContext ddsc = (DDSpanContext)parentContext;
                traceId = ddsc.getTraceId();
                parentSpanId = ddsc.getSpanId();
                baggage = ddsc.getBaggageItems();
                parentTrace = ddsc.getTrace();
                samplingPriority = -2147483648;
                origin = null;
                coreTags = null;
                rootSpanTags = null;
                if (this.serviceName == null) {
                    this.serviceName = ddsc.getServiceName();
                }
            } else {
                if (parentContext instanceof ExtractedContext) {
                    ExtractedContext extractedContext = (ExtractedContext)parentContext;
                    traceId = extractedContext.getTraceId();
                    parentSpanId = extractedContext.getSpanId();
                    samplingPriority = extractedContext.getSamplingPriority();
                    baggage = extractedContext.getBaggage();
                } else {
                    traceId = IdGenerationStrategy.RANDOM.generate();
                    parentSpanId = DDId.ZERO;
                    samplingPriority = -2147483648;
                    baggage = null;
                }

                if (parentContext instanceof TagContext) {
                    coreTags = ((TagContext)parentContext).getTags();
                    origin = ((TagContext)parentContext).getOrigin();
                } else {
                    coreTags = null;
                    origin = null;
                }

                rootSpanTags = localRootSpanTags;
                parentTrace = pendingTraceFactory.create(traceId);
            }

            if (this.serviceName == null) {
                this.serviceName = this.serviceName;
            }

            CharSequence operationName = this.operationName != null ? this.operationName : this.resourceName;
            int tagsSize = (null == this.tags ? 0 : this.tags.size()) + defaultSpanTags.size() + (null == coreTags ? 0 : coreTags.size()) + (null == rootSpanTags ? 0 : rootSpanTags.size());
            DDSpanContext context = new DDSpanContext(traceId, spanId, parentSpanId, this.serviceName, (CharSequence)operationName, this.resourceName, samplingPriority, origin, baggage, this.errorFlag, this.spanType, tagsSize, parentTrace, CoreTracer.this, serviceNameMappings);
            context.setAllTags(defaultSpanTags);
            context.setAllTags(this.tags);
            context.setAllTags(coreTags);
            context.setAllTags(rootSpanTags);
            return context;
        }
    }

    public static class CoreTracerBuilder {
        private Config config;
        private String serviceName;
        private Writer writer;
        private IdGenerationStrategy idGenerationStrategy;
        private Sampler sampler;
        private Injector injector;
        private Extractor extractor;
        private AgentScopeManager scopeManager;
        private Map<String, String> localRootSpanTags;
        private Map<String, String> defaultSpanTags;
        private Map<String, String> serviceNameMappings;
        private Map<String, String> taggedHeaders;
        private int partialFlushMinSpans;
        private StatsDClient statsDClient;

        public CoreTracerBuilder() {
            this.config(Config.get());
        }

        public CoreTracerBuilder withProperties(Properties properties) {
            return this.config(Config.get(properties));
        }

        public CoreTracerBuilder config(Config config) {
            this.config = config;
            this.serviceName(config.getServiceName());
            this.sampler(Sampler.Builder.forConfig(config));
            this.injector(HttpCodec.createInjector(config));
            this.extractor(HttpCodec.createExtractor(config, config.getHeaderTags()));
            this.localRootSpanTags(config.getLocalRootSpanTags());
            this.defaultSpanTags(config.getMergedSpanTags());
            this.serviceNameMappings(config.getServiceMapping());
            this.taggedHeaders(config.getHeaderTags());
            this.partialFlushMinSpans(config.getPartialFlushMinSpans());
            return this;
        }

        public CoreTracerBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public CoreTracerBuilder writer(Writer writer) {
            this.writer = writer;
            return this;
        }

        public CoreTracerBuilder idGenerationStrategy(IdGenerationStrategy idGenerationStrategy) {
            this.idGenerationStrategy = idGenerationStrategy;
            return this;
        }

        public CoreTracerBuilder sampler(Sampler sampler) {
            this.sampler = sampler;
            return this;
        }

        public CoreTracerBuilder injector(Injector injector) {
            this.injector = injector;
            return this;
        }

        public CoreTracerBuilder extractor(Extractor extractor) {
            this.extractor = extractor;
            return this;
        }

        public CoreTracerBuilder scopeManager(AgentScopeManager scopeManager) {
            this.scopeManager = scopeManager;
            return this;
        }

        public CoreTracerBuilder localRootSpanTags(Map<String, String> localRootSpanTags) {
            this.localRootSpanTags = localRootSpanTags;
            return this;
        }

        public CoreTracerBuilder defaultSpanTags(Map<String, String> defaultSpanTags) {
            this.defaultSpanTags = defaultSpanTags;
            return this;
        }

        public CoreTracerBuilder serviceNameMappings(Map<String, String> serviceNameMappings) {
            this.serviceNameMappings = serviceNameMappings;
            return this;
        }

        public CoreTracerBuilder taggedHeaders(Map<String, String> taggedHeaders) {
            this.taggedHeaders = taggedHeaders;
            return this;
        }

        public CoreTracerBuilder partialFlushMinSpans(int partialFlushMinSpans) {
            this.partialFlushMinSpans = partialFlushMinSpans;
            return this;
        }

        public CoreTracerBuilder statsDClient(StatsDClient statsDClient) {
            this.statsDClient = statsDClient;
            return this;
        }

        public CoreTracer build() {
            return new CoreTracer(this.config, this.serviceName, this.writer, this.idGenerationStrategy, this.sampler, this.injector, this.extractor, this.scopeManager, this.localRootSpanTags, this.defaultSpanTags, this.serviceNameMappings, this.taggedHeaders, this.partialFlushMinSpans, this.statsDClient);
        }

        public String toString() {
            return "CoreTracer.CoreTracerBuilder(config=" + this.config + ", serviceName=" + this.serviceName + ", writer=" + this.writer + ", idGenerationStrategy=" + this.idGenerationStrategy + ", sampler=" + this.sampler + ", injector=" + this.injector + ", extractor=" + this.extractor + ", scopeManager=" + this.scopeManager + ", localRootSpanTags=" + this.localRootSpanTags + ", defaultSpanTags=" + this.defaultSpanTags + ", serviceNameMappings=" + this.serviceNameMappings + ", taggedHeaders=" + this.taggedHeaders + ", partialFlushMinSpans=" + this.partialFlushMinSpans + ", statsDClient=" + this.statsDClient + ")";
        }
    }
}
