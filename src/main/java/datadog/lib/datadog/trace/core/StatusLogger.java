package datadog.lib.datadog.trace.core;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;
import com.squareup.moshi.Types;
import datadog.lib.datadog.trace.api.Config;
import datadog.trace.api.DDTraceApiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class StatusLogger {
    private static final Logger log = LoggerFactory.getLogger(StatusLogger.class);

    public StatusLogger() {
    }

    public static void logStatus(Config config) {
        if (log.isInfoEnabled()) {
            log.info("DATADOG TRACER CONFIGURATION {}", (new Builder()).add(ConfigAdapter.FACTORY).build().adapter(Config.class).toJson(config));
        }

        if (log.isDebugEnabled()) {
            log.debug("class path: {}", System.getProperty("java.class.path"));
        }

    }

    private static boolean agentServiceCheck(Config config) {
        try {
            Socket s = new Socket();
            Throwable var2 = null;

            boolean var3;
            try {
                s.setSoTimeout(500);
                s.connect(new InetSocketAddress(config.getAgentHost(), config.getAgentPort()));
                var3 = true;
            } catch (Throwable var13) {
                var2 = var13;
                throw var13;
            } finally {
                if (s != null) {
                    if (var2 != null) {
                        try {
                            s.close();
                        } catch (Throwable var12) {
                            var2.addSuppressed(var12);
                        }
                    } else {
                        s.close();
                    }
                }

            }

            return var3;
        } catch (IOException var15) {
            return false;
        }
    }

    private static void writeMap(JsonWriter writer, Map<String, String> map) throws IOException {
        writer.beginObject();
        Iterator var2 = map.entrySet().iterator();

        while(var2.hasNext()) {
            Entry<String, String> entry = (Entry)var2.next();
            writer.name((String)entry.getKey());
            writer.value((String)entry.getValue());
        }

        writer.endObject();
    }

    private static class ConfigAdapter extends JsonAdapter<Config> {
        public static final JsonAdapter.Factory FACTORY = new Factory() {
            public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
                Class<?> rawType = Types.getRawType(type);
                return rawType.isAssignableFrom(Config.class) ? new ConfigAdapter() : null;
            }
        };

        private ConfigAdapter() {
        }

        public Config fromJson(JsonReader reader) {
            throw new UnsupportedOperationException();
        }

        public void toJson(JsonWriter writer, Config config) throws IOException {
            writer.beginObject();
            writer.name("version");
            writer.value(DDTraceCoreInfo.VERSION);
            writer.name("os_name");
            writer.value(System.getProperty("os.name"));
            writer.name("os_version");
            writer.value(System.getProperty("os.version"));
            writer.name("architecture");
            writer.value(System.getProperty("os.arch"));
            writer.name("lang");
            writer.value("jvm");
            writer.name("lang_version");
            writer.value(System.getProperty("java.version"));
            writer.name("jvm_vendor");
            writer.value(System.getProperty("java.vendor"));
            writer.name("jvm_version");
            writer.value(System.getProperty("java.vm.version"));
            writer.name("java_class_version");
            writer.value(System.getProperty("java.class.version"));
            writer.name("http_nonProxyHosts");
            writer.value(String.valueOf(System.getProperty("http.nonProxyHosts")));
            writer.name("http_proxyHost");
            writer.value(String.valueOf(System.getProperty("http.proxyHost")));
            writer.name("enabled");
            writer.value(config.isTraceEnabled());
            writer.name("service");
            writer.value(config.getServiceName());
            writer.name("agent_url");
            writer.value("http://" + config.getAgentHost() + ":" + config.getAgentPort());
            writer.name("agent_error");
            writer.value(!StatusLogger.agentServiceCheck(config));
            writer.name("debug");
            writer.value(config.isDebugEnabled());
            writer.name("analytics_enabled");
            writer.value(config.isTraceAnalyticsEnabled());
            writer.name("sample_rate");
            writer.value(config.getTraceSampleRate());
            writer.name("sampling_rules");
            writer.beginArray();
            StatusLogger.writeMap(writer, config.getTraceSamplingServiceRules());
            StatusLogger.writeMap(writer, config.getTraceSamplingOperationRules());
            writer.endArray();
            writer.name("priority_sampling_enabled");
            writer.value(config.isPrioritySamplingEnabled());
            writer.name("logs_correlation_enabled");
            writer.value(config.isLogsInjectionEnabled());
            writer.name("profiling_enabled");
            writer.value(config.isProfilingEnabled());
            writer.name("dd_version");
            writer.value(DDTraceApiInfo.VERSION);
            writer.name("health_checks_enabled");
            writer.value(config.isHealthMetricsEnabled());
            writer.name("configuration_file");
            writer.value(config.getConfigFile());
            writer.name("runtime_id");
            writer.value(config.getRuntimeId());
            writer.endObject();
        }
    }
}
