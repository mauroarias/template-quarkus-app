package datadog.lib.datadog.trace.common.writer.ddagent;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;
import datadog.lib.datadog.common.container.ContainerInfo;
import datadog.lib.datadog.trace.api.RatelimitedLogger;
import datadog.lib.datadog.trace.common.writer.ddagent.unixdomainsockets.UnixDomainSocketFactory;
import datadog.lib.datadog.trace.core.DDTraceCoreInfo;
import datadog.lib.datadog.trace.core.monitor.Counter;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import datadog.lib.datadog.trace.core.monitor.Recording;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class DDAgentApi {
    private static final Logger log = LoggerFactory.getLogger(DDAgentApi.class);
    private static final String DATADOG_META_LANG = "Datadog-Meta-Lang";
    private static final String DATADOG_META_LANG_VERSION = "Datadog-Meta-Lang-Version";
    private static final String DATADOG_META_LANG_INTERPRETER = "Datadog-Meta-Lang-Interpreter";
    private static final String DATADOG_META_LANG_INTERPRETER_VENDOR = "Datadog-Meta-Lang-Interpreter-Vendor";
    private static final String DATADOG_META_TRACER_VERSION = "Datadog-Meta-Tracer-Version";
    private static final String DATADOG_CONTAINER_ID = "Datadog-Container-ID";
    private static final String X_DATADOG_TRACE_COUNT = "X-Datadog-Trace-Count";
    private static final String V3_ENDPOINT = "v0.3/traces";
    private static final String V4_ENDPOINT = "v0.4/traces";
    private static final String V5_ENDPOINT = "v0.5/traces";
    private static final long NANOSECONDS_BETWEEN_ERROR_LOG;
    private static final String WILL_NOT_LOG_FOR_MESSAGE = "(Will not log errors for 5 minutes)";
    private final List<DDAgentResponseListener> responseListeners;
    private final String[] endpoints;
    private long previousErrorLogNanos;
    private boolean logNextSuccess;
    private long totalTraces;
    private long receivedTraces;
    private long sentTraces;
    private long failedTraces;
    private final Recording discoveryTimer;
    private final Recording sendPayloadTimer;
    private final Counter agentErrorCounter;
    private static final JsonAdapter<Map<String, Map<String, Number>>> RESPONSE_ADAPTER;
    private static final MediaType MSGPACK;
    private static final Map<String, RequestBody> ENDPOINT_SNIFF_REQUESTS;
    private final String agentUrl;
    private final String unixDomainSocketPath;
    private final long timeoutMillis;
    private OkHttpClient httpClient;
    private HttpUrl tracesUrl;
    private String detectedVersion;
    private boolean agentRunning;
    private RatelimitedLogger ratelimitedLogger;

    public DDAgentApi(String agentUrl, String unixDomainSocketPath, long timeoutMillis, boolean enableV05Endpoint, Monitoring monitoring) {
        this.responseListeners = new ArrayList();
        this.previousErrorLogNanos = System.nanoTime() - NANOSECONDS_BETWEEN_ERROR_LOG;
        this.logNextSuccess = false;
        this.totalTraces = 0L;
        this.receivedTraces = 0L;
        this.sentTraces = 0L;
        this.failedTraces = 0L;
        this.detectedVersion = null;
        this.agentRunning = false;
        this.ratelimitedLogger = new RatelimitedLogger(log, NANOSECONDS_BETWEEN_ERROR_LOG);
        this.agentUrl = agentUrl;
        this.unixDomainSocketPath = unixDomainSocketPath;
        this.timeoutMillis = timeoutMillis;
        this.endpoints = enableV05Endpoint ? new String[]{"v0.5/traces", "v0.4/traces", "v0.3/traces"} : new String[]{"v0.4/traces", "v0.3/traces"};
        this.discoveryTimer = monitoring.newTimer("trace.agent.discovery.time");
        this.sendPayloadTimer = monitoring.newTimer("trace.agent.send.time");
        this.agentErrorCounter = monitoring.newCounter("trace.agent.error.counter");
    }

    public DDAgentApi(String agentUrl, String unixDomainSocketPath, long timeoutMillis, Monitoring monitoring) {
        this(agentUrl, unixDomainSocketPath, timeoutMillis, true, monitoring);
    }

    public void addResponseListener(DDAgentResponseListener listener) {
        if (!this.responseListeners.contains(listener)) {
            this.responseListeners.add(listener);
        }

    }

    TraceMapper selectTraceMapper() {
        String endpoint = this.detectEndpointAndBuildClient();
        if (null == endpoint) {
            return null;
        } else {
            return (TraceMapper)("v0.5/traces".equals(endpoint) ? new TraceMapperV0_5() : new TraceMapperV0_4());
        }
    }

    DDAgentApi.Response sendSerializedTraces(Payload payload) {
        int sizeInBytes = payload.sizeInBytes();
        if (null == this.httpClient) {
            this.detectEndpointAndBuildClient();
            if (null == this.httpClient) {
                log.error("No datadog agent detected");
                this.countAndLogFailedSend(payload.traceCount(), payload.representativeCount(), sizeInBytes, (okhttp3.Response)null, (IOException)null);
                return DDAgentApi.Response.failed(this.agentRunning ? 404 : 503);
            }
        }

        try {
            Request request = prepareRequest(this.tracesUrl).addHeader("X-Datadog-Trace-Count", Integer.toString(payload.representativeCount())).put(new DDAgentApi.MsgPackRequestBody(payload)).build();
            this.totalTraces += (long)payload.representativeCount();
            this.receivedTraces += (long)payload.traceCount();
            Recording recording = this.sendPayloadTimer.start();
            Throwable var5 = null;

            Object responseString;
            try {
                okhttp3.Response response = this.httpClient.newCall(request).execute();
                Throwable var7 = null;

                try {
                    if (response.code() == 200) {
                        this.countAndLogSuccessfulSend(payload.traceCount(), payload.representativeCount(), sizeInBytes);
                        responseString = null;

                        Object e;
                        Object endpoint;
                        try {
                            responseString = getResponseBody(response);
                            if (!"".equals(responseString) && !"OK".equalsIgnoreCase((String)responseString)) {
                                Map<String, Map<String, Number>> parsedResponse = (Map)RESPONSE_ADAPTER.fromJson((String)responseString);
                                endpoint = this.tracesUrl.toString();
                                Iterator var11 = this.responseListeners.iterator();

                                while(var11.hasNext()) {
                                    DDAgentResponseListener listener = (DDAgentResponseListener)var11.next();
                                    listener.onResponse((String)endpoint, parsedResponse);
                                }
                            }

                            e = DDAgentApi.Response.success(response.code(), (String)responseString);
                            return (DDAgentApi.Response)e;
                        } catch (IOException var44) {
                            e = var44;
                            log.debug("Failed to parse DD agent response: {}", responseString, var44);
                            endpoint = DDAgentApi.Response.success(response.code(), (Throwable)var44);
                            return (DDAgentApi.Response)endpoint;
                        }
                    }

                    this.agentErrorCounter.incrementErrorCount(response.message(), payload.traceCount());
                    this.countAndLogFailedSend(payload.traceCount(), payload.representativeCount(), sizeInBytes, response, (IOException)null);
                    responseString = DDAgentApi.Response.failed(response.code());
                } catch (Throwable var45) {
                    responseString = var45;
                    var7 = var45;
                    throw var45;
                } finally {
                    if (response != null) {
                        if (var7 != null) {
                            try {
                                response.close();
                            } catch (Throwable var43) {
                                var7.addSuppressed(var43);
                            }
                        } else {
                            response.close();
                        }
                    }

                }
            } catch (Throwable var47) {
                var5 = var47;
                throw var47;
            } finally {
                if (recording != null) {
                    if (var5 != null) {
                        try {
                            recording.close();
                        } catch (Throwable var42) {
                            var5.addSuppressed(var42);
                        }
                    } else {
                        recording.close();
                    }
                }

            }

            return (DDAgentApi.Response)responseString;
        } catch (IOException var49) {
            this.countAndLogFailedSend(payload.traceCount(), payload.representativeCount(), sizeInBytes, (okhttp3.Response)null, var49);
            return DDAgentApi.Response.failed(var49);
        }
    }

    private void countAndLogSuccessfulSend(int traceCount, int representativeCount, int sizeInBytes) {
        this.sentTraces += (long)traceCount;
        if (log.isDebugEnabled()) {
            log.debug(this.createSendLogMessage(traceCount, representativeCount, sizeInBytes, "Success"));
        } else if (this.logNextSuccess) {
            this.logNextSuccess = false;
            if (log.isInfoEnabled()) {
                log.info(this.createSendLogMessage(traceCount, representativeCount, sizeInBytes, "Success"));
            }
        }

    }

    private void countAndLogFailedSend(int traceCount, int representativeCount, int sizeInBytes, okhttp3.Response response, IOException outer) {
        this.failedTraces += (long)traceCount;
        String agentError = getResponseBody(response);
        String sendErrorString;
        if (log.isDebugEnabled()) {
            sendErrorString = this.createSendLogMessage(traceCount, representativeCount, sizeInBytes, agentError.isEmpty() ? "Error" : agentError);
            if (response != null) {
                log.debug("{} Status: {}, Response: {}, Body: {}", new Object[]{sendErrorString, response.code(), response.message(), agentError});
            } else if (outer != null) {
                log.debug(sendErrorString, outer);
            } else {
                log.debug(sendErrorString);
            }

        } else {
            sendErrorString = this.createSendLogMessage(traceCount, representativeCount, sizeInBytes, agentError.isEmpty() ? "Error" : agentError);
            boolean hasLogged;
            if (response != null) {
                hasLogged = this.ratelimitedLogger.warn("{} Status: {} {}", new Object[]{sendErrorString, response.code(), response.message()});
            } else if (outer != null) {
                hasLogged = this.ratelimitedLogger.warn("{} {}: {}", new Object[]{sendErrorString, outer.getClass().getName(), outer.getMessage()});
            } else {
                hasLogged = this.ratelimitedLogger.warn(sendErrorString, new Object[0]);
            }

            if (hasLogged) {
                this.logNextSuccess = true;
            }

        }
    }

    private static String getResponseBody(okhttp3.Response response) {
        if (response != null) {
            try {
                return response.body().string().trim();
            } catch (IOException | NullPointerException var2) {
            }
        }

        return "";
    }

    private String createSendLogMessage(int traceCount, int representativeCount, int sizeInBytes, String prefix) {
        String sizeString = sizeInBytes > 1024 ? sizeInBytes / 1024 + "KB" : sizeInBytes + "B";
        return prefix + " while sending " + traceCount + " of " + representativeCount + " (size=" + sizeString + ") traces to the DD agent. Total: " + this.totalTraces + ", Received: " + this.receivedTraces + ", Sent: " + this.sentTraces + ", Failed: " + this.failedTraces + ".";
    }

    private static OkHttpClient buildClientIfAvailable(String endpoint, HttpUrl url, String unixDomainSocketPath, long timeoutMillis) {
        OkHttpClient client = buildHttpClient(url.scheme(), unixDomainSocketPath, timeoutMillis);

        try {
            return validateClient(endpoint, client, url);
        } catch (IOException var9) {
            try {
                return validateClient(endpoint, client, url);
            } catch (IOException var8) {
                log.debug("No connectivity to {}: {}", url, var8.getMessage());
                return null;
            }
        }
    }

    private static OkHttpClient validateClient(String endpoint, OkHttpClient client, HttpUrl url) throws IOException {
        RequestBody body = (RequestBody)ENDPOINT_SNIFF_REQUESTS.get(endpoint);
        Request request = prepareRequest(url).header("X-Datadog-Trace-Count", "0").put(body).build();
        okhttp3.Response response = client.newCall(request).execute();
        Throwable var6 = null;

        OkHttpClient var7;
        try {
            if (response.code() != 200) {
                log.debug("connectivity to {} not validated, response code={}", url, response.code());
                return null;
            }

            log.debug("connectivity to {} validated", url);
            var7 = client;
        } catch (Throwable var17) {
            var6 = var17;
            throw var17;
        } finally {
            if (response != null) {
                if (var6 != null) {
                    try {
                        response.close();
                    } catch (Throwable var16) {
                        var6.addSuppressed(var16);
                    }
                } else {
                    response.close();
                }
            }

        }

        return var7;
    }

    private static OkHttpClient buildHttpClient(String scheme, String unixDomainSocketPath, long timeoutMillis) {
        Builder builder = new Builder();
        if (unixDomainSocketPath != null) {
            builder = builder.socketFactory(new UnixDomainSocketFactory(new File(unixDomainSocketPath)));
        }

        if (!"https".equals(scheme)) {
            builder = builder.connectionSpecs(Collections.singletonList(ConnectionSpec.CLEARTEXT));
        }

        return builder.connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS).writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS).readTimeout(timeoutMillis, TimeUnit.MILLISECONDS).dispatcher(new Dispatcher(DDAgentApi.RejectingExecutorService.INSTANCE)).build();
    }

    private static okhttp3.Request.Builder prepareRequest(HttpUrl url) {
        okhttp3.Request.Builder builder = (new okhttp3.Request.Builder()).url(url).addHeader("Datadog-Meta-Lang", "java").addHeader("Datadog-Meta-Lang-Version", DDTraceCoreInfo.JAVA_VERSION).addHeader("Datadog-Meta-Lang-Interpreter", DDTraceCoreInfo.JAVA_VM_NAME).addHeader("Datadog-Meta-Lang-Interpreter-Vendor", DDTraceCoreInfo.JAVA_VM_VENDOR).addHeader("Datadog-Meta-Tracer-Version", DDTraceCoreInfo.VERSION);
        String containerId = ContainerInfo.get().getContainerId();
        return containerId == null ? builder : builder.addHeader("Datadog-Container-ID", containerId);
    }

    String detectEndpointAndBuildClient() {
        if (this.httpClient == null) {
            try {
                Recording recording = this.discoveryTimer.start();
                Throwable var2 = null;

                try {
                    HttpUrl baseUrl = HttpUrl.get(this.agentUrl);
                    this.agentRunning = this.isAgentRunning(baseUrl.host(), baseUrl.port(), this.timeoutMillis);
                    String[] var4 = this.endpoints;
                    int var5 = var4.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        String candidate = var4[var6];
                        this.tracesUrl = baseUrl.newBuilder().addEncodedPathSegments(candidate).build();
                        this.httpClient = buildClientIfAvailable(candidate, this.tracesUrl, this.unixDomainSocketPath, this.timeoutMillis);
                        if (null != this.httpClient) {
                            this.detectedVersion = candidate;
                            log.debug("connected to agent {}", candidate);
                            String var8 = candidate;
                            return var8;
                        }

                        log.debug("API {} endpoints not available. Downgrading", candidate);
                    }

                    if (null == this.tracesUrl) {
                        log.error("no compatible agent detected");
                    }
                } catch (Throwable var26) {
                    var2 = var26;
                    throw var26;
                } finally {
                    if (recording != null) {
                        if (var2 != null) {
                            try {
                                recording.close();
                            } catch (Throwable var25) {
                                var2.addSuppressed(var25);
                            }
                        } else {
                            recording.close();
                        }
                    }

                }
            } finally {
                this.discoveryTimer.flush();
            }
        } else {
            log.warn("No connectivity to datadog agent");
        }

        if (null == this.detectedVersion && log.isDebugEnabled()) {
            log.debug("Tried all of {}, no connectivity to datadog agent", Arrays.asList(this.endpoints));
        }

        return this.detectedVersion;
    }

    private boolean isAgentRunning(String host, int port, long timeoutMillis) {
        try {
            Socket socket = new Socket();
            Throwable var6 = null;

            boolean var7;
            try {
                socket.connect(new InetSocketAddress(host, port), (int)timeoutMillis);
                log.debug("Agent connectivity ({}:{})", host, port);
                var7 = true;
            } catch (Throwable var17) {
                var6 = var17;
                throw var17;
            } finally {
                if (socket != null) {
                    if (var6 != null) {
                        try {
                            socket.close();
                        } catch (Throwable var16) {
                            var6.addSuppressed(var16);
                        }
                    } else {
                        socket.close();
                    }
                }

            }

            return var7;
        } catch (IOException var19) {
            log.debug("No agent connectivity ({}:{})", host, port);
            return false;
        }
    }

    static {
        NANOSECONDS_BETWEEN_ERROR_LOG = TimeUnit.MINUTES.toNanos(5L);
        RESPONSE_ADAPTER = (new com.squareup.moshi.Moshi.Builder()).build().adapter(Types.newParameterizedType(Map.class, new Type[]{String.class, Types.newParameterizedType(Map.class, new Type[]{String.class, Double.class})}));
        MSGPACK = MediaType.get("application/msgpack");
        Map<String, RequestBody> requests = new HashMap();
        requests.put("v0.5/traces", RequestBody.create(MSGPACK, TraceMapperV0_5.EMPTY));
        requests.put("v0.4/traces", RequestBody.create(MSGPACK, TraceMapperV0_4.EMPTY));
        requests.put("v0.3/traces", RequestBody.create(MSGPACK, TraceMapperV0_4.EMPTY));
        ENDPOINT_SNIFF_REQUESTS = Collections.unmodifiableMap(requests);
    }

    private static class MsgPackRequestBody extends RequestBody {
        private final Payload payload;

        private MsgPackRequestBody(Payload payload) {
            this.payload = payload;
        }

        public MediaType contentType() {
            return DDAgentApi.MSGPACK;
        }

        public long contentLength() {
            return (long)this.payload.sizeInBytes();
        }

        public void writeTo(BufferedSink sink) throws IOException {
            this.payload.writeTo(sink);
        }
    }

    public static final class Response {
        private final boolean success;
        private final Integer status;
        private final Throwable exception;
        private final String response;

        public static DDAgentApi.Response success(int status) {
            return new DDAgentApi.Response(true, status, (Throwable)null, (String)null);
        }

        public static DDAgentApi.Response success(int status, String response) {
            return new DDAgentApi.Response(true, status, (Throwable)null, response);
        }

        public static DDAgentApi.Response success(int status, Throwable exception) {
            return new DDAgentApi.Response(true, status, exception, (String)null);
        }

        public static DDAgentApi.Response failed(int status) {
            return new DDAgentApi.Response(false, status, (Throwable)null, (String)null);
        }

        public static DDAgentApi.Response failed(Throwable exception) {
            return new DDAgentApi.Response(false, (Integer)null, exception, (String)null);
        }

        private Response(boolean success, Integer status, Throwable exception, String response) {
            this.success = success;
            this.status = status;
            this.exception = exception;
            this.response = response;
        }

        public final boolean success() {
            return this.success;
        }

        public final Integer status() {
            return this.status;
        }

        public final Throwable exception() {
            return this.exception;
        }

        public final String response() {
            return this.response;
        }
    }

    private static final class RejectingExecutorService extends AbstractExecutorService {
        static final DDAgentApi.RejectingExecutorService INSTANCE = new DDAgentApi.RejectingExecutorService();

        private RejectingExecutorService() {
        }

        public void execute(Runnable command) {
            throw new RejectedExecutionException("Unexpected request to execute async task");
        }

        public void shutdown() {
        }

        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        public boolean isShutdown() {
            return true;
        }

        public boolean isTerminated() {
            return true;
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}
