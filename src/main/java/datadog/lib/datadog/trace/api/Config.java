package datadog.lib.datadog.trace.api;

import datadog.trace.api.ConfigDefaults;
import datadog.trace.api.IdGenerationStrategy;
import datadog.trace.api.PropagationStyle;
import datadog.lib.datadog.trace.bootstrap.config.provider.ConfigProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @deprecated */
@Deprecated
public class Config {
    private static final String DEFAULT_PRIORITY_SAMPLING_FORCE = null;
    private static final BitSet DEFAULT_HTTP_SERVER_ERROR_STATUSES = new BitSet();
    private static final BitSet DEFAULT_HTTP_CLIENT_ERROR_STATUSES = new BitSet();
    private static final String DEFAULT_AGENT_UNIX_DOMAIN_SOCKET = null;
    private static final String DEFAULT_PROPAGATION_STYLE_EXTRACT = PropagationStyle.DATADOG.name();
    private static final String DEFAULT_PROPAGATION_STYLE_INJECT = PropagationStyle.DATADOG.name();
    private static final String DEFAULT_TRACE_ANNOTATIONS = null;
    private static final String DEFAULT_TRACE_METHODS = null;

    static {
        DEFAULT_HTTP_SERVER_ERROR_STATUSES.set(500, 600);
        DEFAULT_HTTP_CLIENT_ERROR_STATUSES.set(400, 500);
    }

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    public static final String CONFIGURATION_FILE = "trace.config";
    public static final String API_KEY = "api-key";
    public static final String API_KEY_FILE = "api-key-file";
    public static final String SITE = "site";
    public static final String SERVICE_NAME = "service.name";
    public static final String TRACE_ENABLED = "trace.enabled";
    public static final String INTEGRATIONS_ENABLED = "integrations.enabled";
    public static final String ID_GENERATION_STRATEGY = "id.generation.strategy";
    public static final String WRITER_TYPE = "writer.type";
    public static final String PRIORITIZATION_TYPE = "prioritization.type";
    public static final String TRACE_AGENT_URL = "trace.agent.url";
    public static final String AGENT_HOST = "agent.host";
    public static final String TRACE_AGENT_PORT = "trace.agent.port";
    public static final String AGENT_PORT_LEGACY = "agent.port";
    public static final String AGENT_UNIX_DOMAIN_SOCKET = "trace.agent.unix.domain.socket";
    public static final String AGENT_TIMEOUT = "trace.agent.timeout";
    public static final String PRIORITY_SAMPLING = "priority.sampling";
    public static final String PRIORITY_SAMPLING_FORCE = "priority.sampling.force";
    /** @deprecated */
    @Deprecated
    public static final String TRACE_RESOLVER_ENABLED = "trace.resolver.enabled";
    public static final String SERVICE_MAPPING = "service.mapping";
    private static final String ENV = "env";
    private static final String VERSION = "version";
    public static final String TAGS = "tags";
    /** @deprecated */
    @Deprecated
    public static final String GLOBAL_TAGS = "trace.global.tags";
    public static final String SPAN_TAGS = "trace.span.tags";
    public static final String JMX_TAGS = "trace.jmx.tags";
    public static final String TRACE_ANALYTICS_ENABLED = "trace.analytics.enabled";
    public static final String TRACE_ANNOTATIONS = "trace.annotations";
    public static final String TRACE_EXECUTORS_ALL = "trace.executors.all";
    public static final String TRACE_EXECUTORS = "trace.executors";
    public static final String TRACE_METHODS = "trace.methods";
    public static final String TRACE_CLASSES_EXCLUDE = "trace.classes.exclude";
    public static final String TRACE_SAMPLING_SERVICE_RULES = "trace.sampling.service.rules";
    public static final String TRACE_SAMPLING_OPERATION_RULES = "trace.sampling.operation.rules";
    public static final String TRACE_SAMPLE_RATE = "trace.sample.rate";
    public static final String TRACE_RATE_LIMIT = "trace.rate.limit";
    public static final String TRACE_REPORT_HOSTNAME = "trace.report-hostname";
    public static final String HEADER_TAGS = "trace.header.tags";
    public static final String HTTP_SERVER_ERROR_STATUSES = "http.server.error.statuses";
    public static final String HTTP_CLIENT_ERROR_STATUSES = "http.client.error.statuses";
    public static final String HTTP_SERVER_TAG_QUERY_STRING = "http.server.tag.query-string";
    public static final String HTTP_CLIENT_TAG_QUERY_STRING = "http.client.tag.query-string";
    public static final String HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN = "trace.http.client.split-by-domain";
    public static final String DB_CLIENT_HOST_SPLIT_BY_INSTANCE = "trace.db.client.split-by-instance";
    public static final String SPLIT_BY_TAGS = "trace.split-by-tags";
    public static final String SCOPE_DEPTH_LIMIT = "trace.scope.depth.limit";
    public static final String SCOPE_STRICT_MODE = "trace.scope.strict.mode";
    public static final String SCOPE_INHERIT_ASYNC_PROPAGATION = "trace.scope.inherit.async.propagation";
    public static final String PARTIAL_FLUSH_MIN_SPANS = "trace.partial.flush.min.spans";
    public static final String RUNTIME_CONTEXT_FIELD_INJECTION = "trace.runtime.context.field.injection";
    public static final String PROPAGATION_STYLE_EXTRACT = "propagation.style.extract";
    public static final String PROPAGATION_STYLE_INJECT = "propagation.style.inject";
    public static final String JMX_FETCH_ENABLED = "jmxfetch.enabled";
    public static final String JMX_FETCH_CONFIG_DIR = "jmxfetch.config.dir";
    public static final String JMX_FETCH_CONFIG = "jmxfetch.config";
    /** @deprecated */
    @Deprecated
    public static final String JMX_FETCH_METRICS_CONFIGS = "jmxfetch.metrics-configs";
    public static final String JMX_FETCH_CHECK_PERIOD = "jmxfetch.check-period";
    public static final String JMX_FETCH_REFRESH_BEANS_PERIOD = "jmxfetch.refresh-beans-period";
    public static final String JMX_FETCH_STATSD_HOST = "jmxfetch.statsd.host";
    public static final String JMX_FETCH_STATSD_PORT = "jmxfetch.statsd.port";
    public static final String HEALTH_METRICS_ENABLED = "trace.health.metrics.enabled";
    public static final String HEALTH_METRICS_STATSD_HOST = "trace.health.metrics.statsd.host";
    public static final String HEALTH_METRICS_STATSD_PORT = "trace.health.metrics.statsd.port";
    public static final String PERF_METRICS_ENABLED = "trace.perf.metrics.enabled";
    public static final String LOGS_INJECTION_ENABLED = "logs.injection";
    public static final String PROFILING_ENABLED = "profiling.enabled";
    /** @deprecated */
    @Deprecated
    public static final String PROFILING_URL = "profiling.url";
    /** @deprecated */
    @Deprecated
    public static final String PROFILING_API_KEY_OLD = "profiling.api-key";
    /** @deprecated */
    @Deprecated
    public static final String PROFILING_API_KEY_FILE_OLD = "profiling.api-key-file";
    /** @deprecated */
    @Deprecated
    public static final String PROFILING_API_KEY_VERY_OLD = "profiling.apikey";
    /** @deprecated */
    @Deprecated
    public static final String PROFILING_API_KEY_FILE_VERY_OLD = "profiling.apikey.file";
    public static final String PROFILING_TAGS = "profiling.tags";
    public static final String PROFILING_START_DELAY = "profiling.start-delay";
    public static final String PROFILING_START_FORCE_FIRST = "profiling.experimental.start-force-first";
    public static final String PROFILING_UPLOAD_PERIOD = "profiling.upload.period";
    public static final String PROFILING_TEMPLATE_OVERRIDE_FILE = "profiling.jfr-template-override-file";
    public static final String PROFILING_UPLOAD_TIMEOUT = "profiling.upload.timeout";
    public static final String PROFILING_UPLOAD_COMPRESSION = "profiling.upload.compression";
    public static final String PROFILING_PROXY_HOST = "profiling.proxy.host";
    public static final String PROFILING_PROXY_PORT = "profiling.proxy.port";
    public static final String PROFILING_PROXY_USERNAME = "profiling.proxy.username";
    public static final String PROFILING_PROXY_PASSWORD = "profiling.proxy.password";
    public static final String PROFILING_EXCEPTION_SAMPLE_LIMIT = "profiling.exception.sample.limit";
    public static final String PROFILING_EXCEPTION_HISTOGRAM_TOP_ITEMS = "profiling.exception.histogram.top-items";
    public static final String PROFILING_EXCEPTION_HISTOGRAM_MAX_COLLECTION_SIZE = "profiling.exception.histogram.max-collection-size";
    public static final String PROFILING_EXCLUDE_AGENT_THREADS = "profiling.exclude.agent-threads";
    public static final String KAFKA_CLIENT_PROPAGATION_ENABLED = "kafka.client.propagation.enabled";
    public static final String KAFKA_CLIENT_BASE64_DECODING_ENABLED = "kafka.client.base64.decoding.enabled";
    private static final String TRACE_AGENT_URL_TEMPLATE = "http://%s:%d";
    private static final String PROFILING_REMOTE_URL_TEMPLATE = "https://intake.profile.%s/v1/input";
    private static final String PROFILING_LOCAL_URL_TEMPLATE = "http://%s:%d/profiling/v1/input";
    private static final Pattern ENV_REPLACEMENT = Pattern.compile("[^a-zA-Z0-9_]");
    private static final String SPLIT_BY_SPACE_OR_COMMA_REGEX = "[,\\s]+";
    private final String runtimeId;
    private final String apiKey;
    private final String site;
    private final String serviceName;
    private final boolean traceEnabled;
    private final boolean integrationsEnabled;
    private final String writerType;
    private final String prioritizationType;
    private final boolean agentConfiguredUsingDefault;
    private final String agentUrl;
    private final String agentHost;
    private final int agentPort;
    private final String agentUnixDomainSocket;
    private final int agentTimeout;
    private final boolean prioritySamplingEnabled;
    private final String prioritySamplingForce;
    private final boolean traceResolverEnabled;
    private final Map<String, String> serviceMapping;
    @NonNull
    private final Map<String, String> tags;
    private final Map<String, String> spanTags;
    private final Map<String, String> jmxTags;
    private final List<String> excludedClasses;
    private final Map<String, String> headerTags;
    private final BitSet httpServerErrorStatuses;
    private final BitSet httpClientErrorStatuses;
    private final boolean httpServerTagQueryString;
    private final boolean httpClientTagQueryString;
    private final boolean httpClientSplitByDomain;
    private final boolean dbClientSplitByInstance;
    private final Set<String> splitByTags;
    private final int scopeDepthLimit;
    private final boolean scopeStrictMode;
    private final boolean scopeInheritAsyncPropagation;
    private final int partialFlushMinSpans;
    private final boolean runtimeContextFieldInjection;
    private final Set<PropagationStyle> propagationStylesToExtract;
    private final Set<PropagationStyle> propagationStylesToInject;
    private final boolean jmxFetchEnabled;
    private final String jmxFetchConfigDir;
    private final List<String> jmxFetchConfigs;
    /** @deprecated */
    @Deprecated
    private final List<String> jmxFetchMetricsConfigs;
    private final Integer jmxFetchCheckPeriod;
    private final Integer jmxFetchRefreshBeansPeriod;
    private final String jmxFetchStatsdHost;
    private final Integer jmxFetchStatsdPort;
    private final boolean healthMetricsEnabled;
    private final String healthMetricsStatsdHost;
    private final Integer healthMetricsStatsdPort;
    private final boolean perfMetricsEnabled;
    private final boolean logsInjectionEnabled;
    private final boolean logsMDCTagsInjectionEnabled;
    private final boolean reportHostName;
    private final String traceAnnotations;
    private final String traceMethods;
    private final boolean traceExecutorsAll;
    private final List<String> traceExecutors;
    private final boolean traceAnalyticsEnabled;
    private final Map<String, String> traceSamplingServiceRules;
    private final Map<String, String> traceSamplingOperationRules;
    private final Double traceSampleRate;
    private final int traceRateLimit;
    private final boolean profilingEnabled;
    /** @deprecated */
    @Deprecated
    private final String profilingUrl;
    private final Map<String, String> profilingTags;
    private final int profilingStartDelay;
    private final boolean profilingStartForceFirst;
    private final int profilingUploadPeriod;
    private final String profilingTemplateOverrideFile;
    private final int profilingUploadTimeout;
    private final String profilingUploadCompression;
    private final String profilingProxyHost;
    private final int profilingProxyPort;
    private final String profilingProxyUsername;
    private final String profilingProxyPassword;
    private final int profilingExceptionSampleLimit;
    private final int profilingExceptionHistogramTopItems;
    private final int profilingExceptionHistogramMaxCollectionSize;
    private final boolean profilingExcludeAgentThreads;
    private final boolean kafkaClientPropagationEnabled;
    private final boolean kafkaClientBase64DecodingEnabled;
    private final boolean hystrixTagsEnabled;
    private final boolean servletPrincipalEnabled;
    private final boolean servletAsyncTimeoutError;
    private final boolean traceAgentV05Enabled;
    private final boolean debugEnabled;
    private final String configFile;
    private final IdGenerationStrategy idGenerationStrategy;
    private final ConfigProvider configProvider;
    private static final String PREFIX = "dd.";
    private static final Config INSTANCE = new Config();

    private String profilingApiKeyMasker() {
        return this.apiKey != null ? "****" : null;
    }

    private String profilingProxyPasswordMasker() {
        return this.profilingProxyPassword != null ? "****" : null;
    }

    private Config() {
        this(INSTANCE != null ? INSTANCE.runtimeId : UUID.randomUUID().toString(), ConfigProvider.createDefault());
    }

    private Config(String runtimeId, ConfigProvider configProvider) {
        this.configProvider = configProvider;
        this.configFile = findConfigurationFile();
        this.runtimeId = runtimeId;
        String apiKeyFile = configProvider.getString("api-key-file");
        String tmpApiKey = configProvider.getStringBypassSysProps("api-key", (String)null);
        if (apiKeyFile != null) {
            try {
                tmpApiKey = (new String(Files.readAllBytes(Paths.get(apiKeyFile)), StandardCharsets.UTF_8)).trim();
            } catch (IOException var18) {
                log.error("Cannot read API key from file {}, skipping", apiKeyFile, var18);
            }
        }

        this.site = configProvider.getString("site", "datadoghq.com", new String[0]);
        this.serviceName = configProvider.getString("service", "unnamed-java-app", new String[]{"service.name"});
        this.traceEnabled = configProvider.getBoolean("trace.enabled", true, new String[0]);
        this.integrationsEnabled = configProvider.getBoolean("integrations.enabled", true, new String[0]);
        this.writerType = configProvider.getString("writer.type", "DDAgentWriter", new String[0]);
        this.prioritizationType = configProvider.getString("prioritization.type", "FastLane", new String[0]);
        this.idGenerationStrategy = (IdGenerationStrategy)configProvider.getEnum("id.generation.strategy", IdGenerationStrategy.class, IdGenerationStrategy.RANDOM);
        if (this.idGenerationStrategy != IdGenerationStrategy.RANDOM) {
            log.warn("*** you are using an unsupported id generation strategy {} - this can impact correctness of traces", this.idGenerationStrategy);
        }

        String agentHostFromEnvironment = null;
        int agentPortFromEnvironment = -1;
        String unixDomainFromEnvironment = null;
        boolean rebuildAgentUrl = false;
        String agentUrlFromEnvironment = configProvider.getString("trace.agent.url");
        if (agentUrlFromEnvironment != null) {
            try {
                URI parsedAgentUrl = new URI(agentUrlFromEnvironment);
                agentHostFromEnvironment = parsedAgentUrl.getHost();
                agentPortFromEnvironment = parsedAgentUrl.getPort();
                if ("unix".equals(parsedAgentUrl.getScheme())) {
                    unixDomainFromEnvironment = parsedAgentUrl.getPath();
                }
            } catch (URISyntaxException var17) {
                log.warn("{} not configured correctly: {}. Ignoring", "trace.agent.url", var17.getMessage());
            }
        }

        if (agentHostFromEnvironment == null) {
            agentHostFromEnvironment = configProvider.getString("agent.host");
            rebuildAgentUrl = true;
        }

        boolean agentHostConfiguredUsingDefault;
        if (agentHostFromEnvironment == null) {
            this.agentHost = "localhost";
            agentHostConfiguredUsingDefault = true;
        } else {
            this.agentHost = agentHostFromEnvironment;
            agentHostConfiguredUsingDefault = false;
        }

        if (agentPortFromEnvironment < 0) {
            this.agentPort = configProvider.getInteger("trace.agent.port", 8126, new String[]{"agent.port"});
            rebuildAgentUrl = true;
        } else {
            this.agentPort = agentPortFromEnvironment;
        }

        if (rebuildAgentUrl) {
            this.agentUrl = String.format("http://%s:%d", this.agentHost, this.agentPort);
        } else {
            this.agentUrl = agentUrlFromEnvironment;
        }

        if (unixDomainFromEnvironment == null) {
            unixDomainFromEnvironment = configProvider.getString("trace.agent.unix.domain.socket");
        }

        boolean socketConfiguredUsingDefault;
        if (unixDomainFromEnvironment == null) {
            this.agentUnixDomainSocket = ConfigDefaults.DEFAULT_AGENT_UNIX_DOMAIN_SOCKET;
            socketConfiguredUsingDefault = true;
        } else {
            this.agentUnixDomainSocket = unixDomainFromEnvironment;
            socketConfiguredUsingDefault = false;
        }

        this.agentConfiguredUsingDefault = agentHostConfiguredUsingDefault && socketConfiguredUsingDefault && this.agentPort == 8126;
        this.agentTimeout = configProvider.getInteger("trace.agent.timeout", 10, new String[0]);
        this.prioritySamplingEnabled = configProvider.getBoolean("priority.sampling", true, new String[0]);
        this.prioritySamplingForce = configProvider.getString("priority.sampling.force", DEFAULT_PRIORITY_SAMPLING_FORCE, new String[0]);
        this.traceResolverEnabled = configProvider.getBoolean("trace.resolver.enabled", true, new String[0]);
        this.serviceMapping = configProvider.getMergedMap("service.mapping");
        Map<String, String> tags = new HashMap(configProvider.getMergedMap("trace.global.tags"));
        tags.putAll(configProvider.getMergedMap("tags"));
        this.tags = this.getMapWithPropertiesDefinedByEnvironment(tags, "env", "version");
        this.spanTags = configProvider.getMergedMap("trace.span.tags");
        this.jmxTags = configProvider.getMergedMap("trace.jmx.tags");
        this.excludedClasses = configProvider.getList("trace.classes.exclude");
        this.headerTags = configProvider.getMergedMap("trace.header.tags");
        this.httpServerErrorStatuses = configProvider.getIntegerRange("http.server.error.statuses", DEFAULT_HTTP_SERVER_ERROR_STATUSES);
        this.httpClientErrorStatuses = configProvider.getIntegerRange("http.client.error.statuses", DEFAULT_HTTP_CLIENT_ERROR_STATUSES);
        this.httpServerTagQueryString = configProvider.getBoolean("http.server.tag.query-string", false, new String[0]);
        this.httpClientTagQueryString = configProvider.getBoolean("http.client.tag.query-string", false, new String[0]);
        this.httpClientSplitByDomain = configProvider.getBoolean("trace.http.client.split-by-domain", false, new String[0]);
        this.dbClientSplitByInstance = configProvider.getBoolean("trace.db.client.split-by-instance", false, new String[0]);
        this.splitByTags = Collections.unmodifiableSet(new LinkedHashSet(configProvider.getList("trace.split-by-tags")));
        this.scopeDepthLimit = configProvider.getInteger("trace.scope.depth.limit", 100, new String[0]);
        this.scopeStrictMode = configProvider.getBoolean("trace.scope.strict.mode", false, new String[0]);
        this.scopeInheritAsyncPropagation = configProvider.getBoolean("trace.scope.inherit.async.propagation", true, new String[0]);
        this.partialFlushMinSpans = configProvider.getInteger("trace.partial.flush.min.spans", 1000, new String[0]);
        this.runtimeContextFieldInjection = configProvider.getBoolean("trace.runtime.context.field.injection", true, new String[0]);
        this.propagationStylesToExtract = this.getPropagationStyleSetSettingFromEnvironmentOrDefault("propagation.style.extract", DEFAULT_PROPAGATION_STYLE_EXTRACT);
        this.propagationStylesToInject = this.getPropagationStyleSetSettingFromEnvironmentOrDefault("propagation.style.inject", DEFAULT_PROPAGATION_STYLE_INJECT);
        boolean runtimeMetricsEnabled = configProvider.getBoolean("runtime.metrics.enabled", true, new String[0]);
        this.jmxFetchEnabled = runtimeMetricsEnabled && configProvider.getBoolean("jmxfetch.enabled", true, new String[0]);
        this.jmxFetchConfigDir = configProvider.getString("jmxfetch.config.dir");
        this.jmxFetchConfigs = configProvider.getList("jmxfetch.config");
        this.jmxFetchMetricsConfigs = configProvider.getList("jmxfetch.metrics-configs");
        this.jmxFetchCheckPeriod = configProvider.getInteger("jmxfetch.check-period");
        this.jmxFetchRefreshBeansPeriod = configProvider.getInteger("jmxfetch.refresh-beans-period");
        this.jmxFetchStatsdHost = configProvider.getString("jmxfetch.statsd.host");
        this.jmxFetchStatsdPort = configProvider.getInteger("jmxfetch.statsd.port", 8125, new String[0]);
        this.healthMetricsEnabled = runtimeMetricsEnabled && configProvider.getBoolean("trace.health.metrics.enabled", true, new String[0]);
        this.healthMetricsStatsdHost = configProvider.getString("trace.health.metrics.statsd.host");
        this.healthMetricsStatsdPort = configProvider.getInteger("trace.health.metrics.statsd.port");
        this.perfMetricsEnabled = runtimeMetricsEnabled && configProvider.getBoolean("trace.perf.metrics.enabled", false, new String[0]);
        this.logsInjectionEnabled = configProvider.getBoolean("logs.injection", false, new String[0]);
        this.logsMDCTagsInjectionEnabled = configProvider.getBoolean("logs.mdc.tags.injection", false, new String[0]);
        this.reportHostName = configProvider.getBoolean("trace.report-hostname", false, new String[0]);
        this.traceAgentV05Enabled = configProvider.getBoolean("trace.agent.v0.5.enabled", false, new String[0]);
        this.traceAnnotations = configProvider.getString("trace.annotations", DEFAULT_TRACE_ANNOTATIONS, new String[0]);
        this.traceMethods = configProvider.getString("trace.methods", DEFAULT_TRACE_METHODS, new String[0]);
        this.traceExecutorsAll = configProvider.getBoolean("trace.executors.all", false, new String[0]);
        this.traceExecutors = configProvider.getList("trace.executors");
        this.traceAnalyticsEnabled = configProvider.getBoolean("trace.analytics.enabled", false, new String[0]);
        this.traceSamplingServiceRules = configProvider.getMergedMap("trace.sampling.service.rules");
        this.traceSamplingOperationRules = configProvider.getMergedMap("trace.sampling.operation.rules");
        this.traceSampleRate = configProvider.getDouble("trace.sample.rate");
        this.traceRateLimit = configProvider.getInteger("trace.rate.limit", 100, new String[0]);
        this.profilingEnabled = configProvider.getBoolean("profiling.enabled", false, new String[0]);
        this.profilingUrl = configProvider.getString("profiling.url");
        String veryOldProfilingApiKeyFile;
        if (tmpApiKey == null) {
            veryOldProfilingApiKeyFile = configProvider.getString("profiling.api-key-file");
            tmpApiKey = System.getenv(propertyNameToEnvironmentVariableName("profiling.api-key"));
            if (veryOldProfilingApiKeyFile != null) {
                try {
                    tmpApiKey = (new String(Files.readAllBytes(Paths.get(veryOldProfilingApiKeyFile)), StandardCharsets.UTF_8)).trim();
                } catch (IOException var16) {
                    log.error("Cannot read API key from file {}, skipping", veryOldProfilingApiKeyFile, var16);
                }
            }
        }

        if (tmpApiKey == null) {
            veryOldProfilingApiKeyFile = configProvider.getString("profiling.apikey.file");
            tmpApiKey = System.getenv(propertyNameToEnvironmentVariableName("profiling.apikey"));
            if (veryOldProfilingApiKeyFile != null) {
                try {
                    tmpApiKey = (new String(Files.readAllBytes(Paths.get(veryOldProfilingApiKeyFile)), StandardCharsets.UTF_8)).trim();
                } catch (IOException var15) {
                    log.error("Cannot read API key from file {}, skipping", veryOldProfilingApiKeyFile, var15);
                }
            }
        }

        this.profilingTags = configProvider.getMergedMap("profiling.tags");
        this.profilingStartDelay = configProvider.getInteger("profiling.start-delay", 10, new String[0]);
        this.profilingStartForceFirst = configProvider.getBoolean("profiling.experimental.start-force-first", false, new String[0]);
        this.profilingUploadPeriod = configProvider.getInteger("profiling.upload.period", 60, new String[0]);
        this.profilingTemplateOverrideFile = configProvider.getString("profiling.jfr-template-override-file");
        this.profilingUploadTimeout = configProvider.getInteger("profiling.upload.timeout", 30, new String[0]);
        this.profilingUploadCompression = configProvider.getString("profiling.upload.compression", "on", new String[0]);
        this.profilingProxyHost = configProvider.getString("profiling.proxy.host");
        this.profilingProxyPort = configProvider.getInteger("profiling.proxy.port", 8080, new String[0]);
        this.profilingProxyUsername = configProvider.getString("profiling.proxy.username");
        this.profilingProxyPassword = configProvider.getString("profiling.proxy.password");
        this.profilingExceptionSampleLimit = configProvider.getInteger("profiling.exception.sample.limit", 10000, new String[0]);
        this.profilingExceptionHistogramTopItems = configProvider.getInteger("profiling.exception.histogram.top-items", 50, new String[0]);
        this.profilingExceptionHistogramMaxCollectionSize = configProvider.getInteger("profiling.exception.histogram.max-collection-size", 10000, new String[0]);
        this.profilingExcludeAgentThreads = configProvider.getBoolean("profiling.exclude.agent-threads", true, new String[0]);
        this.kafkaClientPropagationEnabled = configProvider.getBoolean("kafka.client.propagation.enabled", true, new String[0]);
        this.kafkaClientBase64DecodingEnabled = configProvider.getBoolean("kafka.client.base64.decoding.enabled", false, new String[0]);
        this.hystrixTagsEnabled = configProvider.getBoolean("hystrix.tags.enabled", false, new String[0]);
        this.servletPrincipalEnabled = configProvider.getBoolean("trace.servlet.principal.enabled", false, new String[0]);
        this.servletAsyncTimeoutError = configProvider.getBoolean("trace.servlet.async-timeout.error", true, new String[0]);
        this.debugEnabled = isDebugMode();
        this.apiKey = tmpApiKey;
        log.debug("New instance: {}", this);
    }

    public Map<String, String> getLocalRootSpanTags() {
        Map<String, String> runtimeTags = this.getRuntimeTags();
        Map<String, String> result = new HashMap(runtimeTags);
        result.put("language", "jvm");
        if (this.reportHostName) {
            String hostName = getHostName();
            if (null != hostName && !hostName.isEmpty()) {
                result.put("_dd.hostname", hostName);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    public Map<String, String> getMergedSpanTags() {
        Map<String, String> result = newHashMap(this.getGlobalTags().size() + this.spanTags.size());
        result.putAll(this.getGlobalTags());
        result.putAll(this.spanTags);
        return Collections.unmodifiableMap(result);
    }

    public Map<String, String> getMergedJmxTags() {
        Map<String, String> runtimeTags = this.getRuntimeTags();
        Map<String, String> result = newHashMap(this.getGlobalTags().size() + this.jmxTags.size() + runtimeTags.size() + 1);
        result.putAll(this.getGlobalTags());
        result.putAll(this.jmxTags);
        result.putAll(runtimeTags);
        result.put("service", this.serviceName);
        return Collections.unmodifiableMap(result);
    }

    public Map<String, String> getMergedProfilingTags() {
        Map<String, String> runtimeTags = this.getRuntimeTags();
        String host = getHostName();
        Map<String, String> result = newHashMap(this.getGlobalTags().size() + this.profilingTags.size() + runtimeTags.size() + 3);
        result.put("host", host);
        result.putAll(this.getGlobalTags());
        result.putAll(this.profilingTags);
        result.putAll(runtimeTags);
        result.put("service", this.serviceName);
        result.put("language", "jvm");
        return Collections.unmodifiableMap(result);
    }

    public float getInstrumentationAnalyticsSampleRate(String... aliases) {
        String[] var2 = aliases;
        int var3 = aliases.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String alias = var2[var4];
            String configKey = alias + ".analytics.sample-rate";
            Float rate = this.configProvider.getFloat("trace." + configKey, new String[]{configKey});
            if (null != rate) {
                return rate;
            }
        }

        return 1.0F;
    }

    private Map<String, String> getGlobalTags() {
        return this.tags;
    }

    private Map<String, String> getRuntimeTags() {
        Map<String, String> result = newHashMap(2);
        result.put("runtime-id", this.runtimeId);
        return Collections.unmodifiableMap(result);
    }

    public String getFinalProfilingUrl() {
        if (this.profilingUrl != null) {
            return this.profilingUrl;
        } else {
            return this.apiKey != null ? String.format("https://intake.profile.%s/v1/input", this.site) : String.format("http://%s:%d/profiling/v1/input", this.agentHost, this.agentPort);
        }
    }

    public boolean isIntegrationEnabled(SortedSet<String> integrationNames, boolean defaultEnabled) {
        return this.isEnabled(integrationNames, "integration.", ".enabled", defaultEnabled);
    }

    public boolean isJmxFetchIntegrationEnabled(SortedSet<String> integrationNames, boolean defaultEnabled) {
        return this.isEnabled(integrationNames, "jmxfetch.", ".enabled", defaultEnabled);
    }

    public boolean isRuleEnabled(String name) {
        return this.configProvider.getBoolean("trace." + name + ".enabled", true, new String[0]) && this.configProvider.getBoolean("trace." + name.toLowerCase() + ".enabled", true, new String[0]);
    }

    /** @deprecated */
    public static boolean jmxFetchIntegrationEnabled(SortedSet<String> integrationNames, boolean defaultEnabled) {
        return get().isJmxFetchIntegrationEnabled(integrationNames, defaultEnabled);
    }

    public boolean isEndToEndDurationEnabled(boolean defaultEnabled, String... integrationNames) {
        return this.isEnabled(Arrays.asList(integrationNames), "", ".e2e.duration.enabled", defaultEnabled);
    }

    public boolean isTraceAnalyticsIntegrationEnabled(SortedSet<String> integrationNames, boolean defaultEnabled) {
        return this.isEnabled(integrationNames, "", ".analytics.enabled", defaultEnabled);
    }

    public boolean isTraceAnalyticsIntegrationEnabled(boolean defaultEnabled, String... integrationNames) {
        return this.isEnabled(Arrays.asList(integrationNames), "", ".analytics.enabled", defaultEnabled);
    }

    private static boolean isDebugMode() {
        String tracerDebugLevelSysprop = "dd.trace.debug";
        String tracerDebugLevelProp = System.getProperty("dd.trace.debug");
        if (tracerDebugLevelProp != null) {
            return Boolean.parseBoolean(tracerDebugLevelProp);
        } else {
            String tracerDebugLevelEnv = System.getenv("dd.trace.debug".replace('.', '_').toUpperCase());
            return tracerDebugLevelEnv != null ? Boolean.parseBoolean(tracerDebugLevelEnv) : false;
        }
    }

    /** @deprecated */
    public static boolean traceAnalyticsIntegrationEnabled(SortedSet<String> integrationNames, boolean defaultEnabled) {
        return get().isTraceAnalyticsIntegrationEnabled(integrationNames, defaultEnabled);
    }

    private boolean isEnabled(Iterable<String> integrationNames, String settingPrefix, String settingSuffix, boolean defaultEnabled) {
        boolean anyEnabled = defaultEnabled;
        Iterator var6 = integrationNames.iterator();

        while(var6.hasNext()) {
            String name = (String)var6.next();
            String configKey = settingPrefix + name + settingSuffix;
            boolean configEnabled = this.configProvider.getBoolean("trace." + configKey, defaultEnabled, new String[]{configKey});
            if (defaultEnabled) {
                anyEnabled &= configEnabled;
            } else {
                anyEnabled |= configEnabled;
            }
        }

        return anyEnabled;
    }

    private Set<PropagationStyle> getPropagationStyleSetSettingFromEnvironmentOrDefault(String name, String defaultValue) {
        String value = this.configProvider.getString(name, defaultValue, new String[0]);
        Set<PropagationStyle> result = convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(value));
        if (result.isEmpty()) {
            result = convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(defaultValue));
        }

        return result;
    }

    @NonNull
    private static String propertyNameToEnvironmentVariableName(String setting) {
        return ENV_REPLACEMENT.matcher(propertyNameToSystemPropertyName(setting).toUpperCase()).replaceAll("_");
    }

    @NonNull
    private static String propertyNameToSystemPropertyName(String setting) {
        return "dd." + setting;
    }

    @NonNull
    private static Map<String, String> newHashMap(int size) {
        return new HashMap(size + 1, 1.0F);
    }

    @NonNull
    private Map<String, String> getMapWithPropertiesDefinedByEnvironment(@NonNull Map<String, String> map, @NonNull String... propNames) {
        if (map == null) {
            throw new NullPointerException("map is marked non-null but is null");
        } else if (propNames == null) {
            throw new NullPointerException("propNames is marked non-null but is null");
        } else {
            Map<String, String> res = new HashMap(map);
            String[] var4 = propNames;
            int var5 = propNames.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String propName = var4[var6];
                String val = this.configProvider.getString(propName);
                if (val != null) {
                    res.put(propName, val);
                }
            }

            return Collections.unmodifiableMap(res);
        }
    }

    @NonNull
    private static Set<String> parseStringIntoSetOfNonEmptyStrings(String str) {
        Set<String> result = new LinkedHashSet();
        String[] var2 = str.split("[,\\s]+");
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String value = var2[var4];
            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    @NonNull
    private static Set<PropagationStyle> convertStringSetToPropagationStyleSet(Set<String> input) {
        Set<PropagationStyle> result = new LinkedHashSet();
        Iterator var2 = input.iterator();

        while(var2.hasNext()) {
            String value = (String)var2.next();

            try {
                result.add(PropagationStyle.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException var5) {
                log.debug("Cannot recognize config string value: {}, {}", value, PropagationStyle.class);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    private static String findConfigurationFile() {
        String configurationFilePath = System.getProperty(propertyNameToSystemPropertyName("trace.config"));
        if (null == configurationFilePath) {
            configurationFilePath = System.getenv(propertyNameToEnvironmentVariableName("trace.config"));
        }

        if (null != configurationFilePath) {
            configurationFilePath = configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));
            File configurationFile = new File(configurationFilePath);
            if (!configurationFile.exists()) {
                return configurationFilePath;
            }
        }

        return "no config file present";
    }

    private static String getHostName() {
        String possibleHostname;
        if (System.getProperty("os.name").startsWith("Windows")) {
            possibleHostname = System.getenv("COMPUTERNAME");
        } else {
            possibleHostname = System.getenv("HOSTNAME");
        }

        if (possibleHostname != null && !possibleHostname.isEmpty()) {
            log.debug("Determined hostname from environment variable");
            return possibleHostname.trim();
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()));
                Throwable var2 = null;

                try {
                    possibleHostname = reader.readLine();
                } catch (Throwable var14) {
                    var2 = var14;
                    throw var14;
                } finally {
                    if (reader != null) {
                        if (var2 != null) {
                            try {
                                reader.close();
                            } catch (Throwable var13) {
                                var2.addSuppressed(var13);
                            }
                        } else {
                            reader.close();
                        }
                    }

                }
            } catch (Exception var16) {
            }

            if (possibleHostname != null && !possibleHostname.isEmpty()) {
                log.debug("Determined hostname from hostname command");
                return possibleHostname.trim();
            } else {
                try {
                    return InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException var12) {
                    return null;
                }
            }
        }
    }

    public static Config get() {
        return INSTANCE;
    }

    /** @deprecated */
    @Deprecated
    public static Config get(Properties properties) {
        return properties != null && !properties.isEmpty() ? new Config(INSTANCE.runtimeId, ConfigProvider.withPropertiesOverride(properties)) : INSTANCE;
    }

    public String toString() {
        return "Config(apiKey=" + this.profilingApiKeyMasker() + ", profilingProxyPassword=" + this.profilingProxyPasswordMasker() + ", runtimeId=" + this.getRuntimeId() + ", site=" + this.getSite() + ", serviceName=" + this.getServiceName() + ", traceEnabled=" + this.isTraceEnabled() + ", integrationsEnabled=" + this.isIntegrationsEnabled() + ", writerType=" + this.getWriterType() + ", prioritizationType=" + this.getPrioritizationType() + ", agentConfiguredUsingDefault=" + this.isAgentConfiguredUsingDefault() + ", agentUrl=" + this.getAgentUrl() + ", agentHost=" + this.getAgentHost() + ", agentPort=" + this.getAgentPort() + ", agentUnixDomainSocket=" + this.getAgentUnixDomainSocket() + ", agentTimeout=" + this.getAgentTimeout() + ", prioritySamplingEnabled=" + this.isPrioritySamplingEnabled() + ", prioritySamplingForce=" + this.getPrioritySamplingForce() + ", traceResolverEnabled=" + this.isTraceResolverEnabled() + ", serviceMapping=" + this.getServiceMapping() + ", tags=" + this.tags + ", spanTags=" + this.spanTags + ", jmxTags=" + this.jmxTags + ", excludedClasses=" + this.getExcludedClasses() + ", headerTags=" + this.getHeaderTags() + ", httpServerErrorStatuses=" + this.getHttpServerErrorStatuses() + ", httpClientErrorStatuses=" + this.getHttpClientErrorStatuses() + ", httpServerTagQueryString=" + this.isHttpServerTagQueryString() + ", httpClientTagQueryString=" + this.isHttpClientTagQueryString() + ", httpClientSplitByDomain=" + this.isHttpClientSplitByDomain() + ", dbClientSplitByInstance=" + this.isDbClientSplitByInstance() + ", splitByTags=" + this.getSplitByTags() + ", scopeDepthLimit=" + this.getScopeDepthLimit() + ", scopeStrictMode=" + this.isScopeStrictMode() + ", scopeInheritAsyncPropagation=" + this.isScopeInheritAsyncPropagation() + ", partialFlushMinSpans=" + this.getPartialFlushMinSpans() + ", runtimeContextFieldInjection=" + this.isRuntimeContextFieldInjection() + ", propagationStylesToExtract=" + this.getPropagationStylesToExtract() + ", propagationStylesToInject=" + this.getPropagationStylesToInject() + ", jmxFetchEnabled=" + this.isJmxFetchEnabled() + ", jmxFetchConfigDir=" + this.getJmxFetchConfigDir() + ", jmxFetchConfigs=" + this.getJmxFetchConfigs() + ", jmxFetchMetricsConfigs=" + this.getJmxFetchMetricsConfigs() + ", jmxFetchCheckPeriod=" + this.getJmxFetchCheckPeriod() + ", jmxFetchRefreshBeansPeriod=" + this.getJmxFetchRefreshBeansPeriod() + ", jmxFetchStatsdHost=" + this.getJmxFetchStatsdHost() + ", jmxFetchStatsdPort=" + this.getJmxFetchStatsdPort() + ", healthMetricsEnabled=" + this.isHealthMetricsEnabled() + ", healthMetricsStatsdHost=" + this.getHealthMetricsStatsdHost() + ", healthMetricsStatsdPort=" + this.getHealthMetricsStatsdPort() + ", perfMetricsEnabled=" + this.isPerfMetricsEnabled() + ", logsInjectionEnabled=" + this.isLogsInjectionEnabled() + ", logsMDCTagsInjectionEnabled=" + this.isLogsMDCTagsInjectionEnabled() + ", reportHostName=" + this.isReportHostName() + ", traceAnnotations=" + this.getTraceAnnotations() + ", traceMethods=" + this.getTraceMethods() + ", traceExecutorsAll=" + this.isTraceExecutorsAll() + ", traceExecutors=" + this.getTraceExecutors() + ", traceAnalyticsEnabled=" + this.isTraceAnalyticsEnabled() + ", traceSamplingServiceRules=" + this.getTraceSamplingServiceRules() + ", traceSamplingOperationRules=" + this.getTraceSamplingOperationRules() + ", traceSampleRate=" + this.getTraceSampleRate() + ", traceRateLimit=" + this.getTraceRateLimit() + ", profilingEnabled=" + this.isProfilingEnabled() + ", profilingUrl=" + this.profilingUrl + ", profilingTags=" + this.profilingTags + ", profilingStartDelay=" + this.getProfilingStartDelay() + ", profilingStartForceFirst=" + this.isProfilingStartForceFirst() + ", profilingUploadPeriod=" + this.getProfilingUploadPeriod() + ", profilingTemplateOverrideFile=" + this.getProfilingTemplateOverrideFile() + ", profilingUploadTimeout=" + this.getProfilingUploadTimeout() + ", profilingUploadCompression=" + this.getProfilingUploadCompression() + ", profilingProxyHost=" + this.getProfilingProxyHost() + ", profilingProxyPort=" + this.getProfilingProxyPort() + ", profilingProxyUsername=" + this.getProfilingProxyUsername() + ", profilingExceptionSampleLimit=" + this.getProfilingExceptionSampleLimit() + ", profilingExceptionHistogramTopItems=" + this.getProfilingExceptionHistogramTopItems() + ", profilingExceptionHistogramMaxCollectionSize=" + this.getProfilingExceptionHistogramMaxCollectionSize() + ", profilingExcludeAgentThreads=" + this.isProfilingExcludeAgentThreads() + ", kafkaClientPropagationEnabled=" + this.isKafkaClientPropagationEnabled() + ", kafkaClientBase64DecodingEnabled=" + this.isKafkaClientBase64DecodingEnabled() + ", hystrixTagsEnabled=" + this.isHystrixTagsEnabled() + ", servletPrincipalEnabled=" + this.isServletPrincipalEnabled() + ", servletAsyncTimeoutError=" + this.isServletAsyncTimeoutError() + ", traceAgentV05Enabled=" + this.isTraceAgentV05Enabled() + ", debugEnabled=" + this.isDebugEnabled() + ", configFile=" + this.getConfigFile() + ", idGenerationStrategy=" + this.getIdGenerationStrategy() + ", configProvider=" + this.configProvider + ")";
    }

    public String getRuntimeId() {
        return this.runtimeId;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public String getSite() {
        return this.site;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public boolean isTraceEnabled() {
        return this.traceEnabled;
    }

    public boolean isIntegrationsEnabled() {
        return this.integrationsEnabled;
    }

    public String getWriterType() {
        return this.writerType;
    }

    public String getPrioritizationType() {
        return this.prioritizationType;
    }

    public boolean isAgentConfiguredUsingDefault() {
        return this.agentConfiguredUsingDefault;
    }

    public String getAgentUrl() {
        return this.agentUrl;
    }

    public String getAgentHost() {
        return this.agentHost;
    }

    public int getAgentPort() {
        return this.agentPort;
    }

    public String getAgentUnixDomainSocket() {
        return this.agentUnixDomainSocket;
    }

    public int getAgentTimeout() {
        return this.agentTimeout;
    }

    public boolean isPrioritySamplingEnabled() {
        return this.prioritySamplingEnabled;
    }

    public String getPrioritySamplingForce() {
        return this.prioritySamplingForce;
    }

    public boolean isTraceResolverEnabled() {
        return this.traceResolverEnabled;
    }

    public Map<String, String> getServiceMapping() {
        return this.serviceMapping;
    }

    public List<String> getExcludedClasses() {
        return this.excludedClasses;
    }

    public Map<String, String> getHeaderTags() {
        return this.headerTags;
    }

    public BitSet getHttpServerErrorStatuses() {
        return this.httpServerErrorStatuses;
    }

    public BitSet getHttpClientErrorStatuses() {
        return this.httpClientErrorStatuses;
    }

    public boolean isHttpServerTagQueryString() {
        return this.httpServerTagQueryString;
    }

    public boolean isHttpClientTagQueryString() {
        return this.httpClientTagQueryString;
    }

    public boolean isHttpClientSplitByDomain() {
        return this.httpClientSplitByDomain;
    }

    public boolean isDbClientSplitByInstance() {
        return this.dbClientSplitByInstance;
    }

    public Set<String> getSplitByTags() {
        return this.splitByTags;
    }

    public int getScopeDepthLimit() {
        return this.scopeDepthLimit;
    }

    public boolean isScopeStrictMode() {
        return this.scopeStrictMode;
    }

    public boolean isScopeInheritAsyncPropagation() {
        return this.scopeInheritAsyncPropagation;
    }

    public int getPartialFlushMinSpans() {
        return this.partialFlushMinSpans;
    }

    public boolean isRuntimeContextFieldInjection() {
        return this.runtimeContextFieldInjection;
    }

    public Set<PropagationStyle> getPropagationStylesToExtract() {
        return this.propagationStylesToExtract;
    }

    public Set<PropagationStyle> getPropagationStylesToInject() {
        return this.propagationStylesToInject;
    }

    public boolean isJmxFetchEnabled() {
        return this.jmxFetchEnabled;
    }

    public String getJmxFetchConfigDir() {
        return this.jmxFetchConfigDir;
    }

    public List<String> getJmxFetchConfigs() {
        return this.jmxFetchConfigs;
    }

    /** @deprecated */
    @Deprecated
    public List<String> getJmxFetchMetricsConfigs() {
        return this.jmxFetchMetricsConfigs;
    }

    public Integer getJmxFetchCheckPeriod() {
        return this.jmxFetchCheckPeriod;
    }

    public Integer getJmxFetchRefreshBeansPeriod() {
        return this.jmxFetchRefreshBeansPeriod;
    }

    public String getJmxFetchStatsdHost() {
        return this.jmxFetchStatsdHost;
    }

    public Integer getJmxFetchStatsdPort() {
        return this.jmxFetchStatsdPort;
    }

    public boolean isHealthMetricsEnabled() {
        return this.healthMetricsEnabled;
    }

    public String getHealthMetricsStatsdHost() {
        return this.healthMetricsStatsdHost;
    }

    public Integer getHealthMetricsStatsdPort() {
        return this.healthMetricsStatsdPort;
    }

    public boolean isPerfMetricsEnabled() {
        return this.perfMetricsEnabled;
    }

    public boolean isLogsInjectionEnabled() {
        return this.logsInjectionEnabled;
    }

    public boolean isLogsMDCTagsInjectionEnabled() {
        return this.logsMDCTagsInjectionEnabled;
    }

    public boolean isReportHostName() {
        return this.reportHostName;
    }

    public String getTraceAnnotations() {
        return this.traceAnnotations;
    }

    public String getTraceMethods() {
        return this.traceMethods;
    }

    public boolean isTraceExecutorsAll() {
        return this.traceExecutorsAll;
    }

    public List<String> getTraceExecutors() {
        return this.traceExecutors;
    }

    public boolean isTraceAnalyticsEnabled() {
        return this.traceAnalyticsEnabled;
    }

    public Map<String, String> getTraceSamplingServiceRules() {
        return this.traceSamplingServiceRules;
    }

    public Map<String, String> getTraceSamplingOperationRules() {
        return this.traceSamplingOperationRules;
    }

    public Double getTraceSampleRate() {
        return this.traceSampleRate;
    }

    public int getTraceRateLimit() {
        return this.traceRateLimit;
    }

    public boolean isProfilingEnabled() {
        return this.profilingEnabled;
    }

    public int getProfilingStartDelay() {
        return this.profilingStartDelay;
    }

    public boolean isProfilingStartForceFirst() {
        return this.profilingStartForceFirst;
    }

    public int getProfilingUploadPeriod() {
        return this.profilingUploadPeriod;
    }

    public String getProfilingTemplateOverrideFile() {
        return this.profilingTemplateOverrideFile;
    }

    public int getProfilingUploadTimeout() {
        return this.profilingUploadTimeout;
    }

    public String getProfilingUploadCompression() {
        return this.profilingUploadCompression;
    }

    public String getProfilingProxyHost() {
        return this.profilingProxyHost;
    }

    public int getProfilingProxyPort() {
        return this.profilingProxyPort;
    }

    public String getProfilingProxyUsername() {
        return this.profilingProxyUsername;
    }

    public String getProfilingProxyPassword() {
        return this.profilingProxyPassword;
    }

    public int getProfilingExceptionSampleLimit() {
        return this.profilingExceptionSampleLimit;
    }

    public int getProfilingExceptionHistogramTopItems() {
        return this.profilingExceptionHistogramTopItems;
    }

    public int getProfilingExceptionHistogramMaxCollectionSize() {
        return this.profilingExceptionHistogramMaxCollectionSize;
    }

    public boolean isProfilingExcludeAgentThreads() {
        return this.profilingExcludeAgentThreads;
    }

    public boolean isKafkaClientPropagationEnabled() {
        return this.kafkaClientPropagationEnabled;
    }

    public boolean isKafkaClientBase64DecodingEnabled() {
        return this.kafkaClientBase64DecodingEnabled;
    }

    public boolean isHystrixTagsEnabled() {
        return this.hystrixTagsEnabled;
    }

    public boolean isServletPrincipalEnabled() {
        return this.servletPrincipalEnabled;
    }

    public boolean isServletAsyncTimeoutError() {
        return this.servletAsyncTimeoutError;
    }

    public boolean isTraceAgentV05Enabled() {
        return this.traceAgentV05Enabled;
    }

    public boolean isDebugEnabled() {
        return this.debugEnabled;
    }

    public String getConfigFile() {
        return this.configFile;
    }

    public IdGenerationStrategy getIdGenerationStrategy() {
        return this.idGenerationStrategy;
    }
}
