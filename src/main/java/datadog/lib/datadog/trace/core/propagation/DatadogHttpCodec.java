package datadog.lib.datadog.trace.core.propagation;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.lib.datadog.trace.core.DDSpanContext;
import datadog.trace.api.DDId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

class DatadogHttpCodec {
    private static final Logger log = LoggerFactory.getLogger(DatadogHttpCodec.class);
    private static final String OT_BAGGAGE_PREFIX = "ot-baggage-";
    private static final String TRACE_ID_KEY = "x-datadog-trace-id";
    private static final String SPAN_ID_KEY = "x-datadog-parent-id";
    private static final String SAMPLING_PRIORITY_KEY = "x-datadog-sampling-priority";
    private static final String ORIGIN_KEY = "x-datadog-origin";

    private DatadogHttpCodec() {
    }

    public static HttpCodec.Extractor newExtractor(Map<String, String> tagMapping) {
        return new TagContextExtractor(tagMapping, new ContextInterpreter.Factory() {
            protected ContextInterpreter construct(Map<String, String> mapping) {
                return new DatadogContextInterpreter(mapping);
            }
        });
    }

    private static class DatadogContextInterpreter extends ContextInterpreter {
        private static final int TRACE_ID = 0;
        private static final int SPAN_ID = 1;
        private static final int ORIGIN = 2;
        private static final int SAMPLING_PRIORITY = 3;
        private static final int TAGS = 4;
        private static final int OT_BAGGAGE = 5;
        private static final int IGNORE = -1;

        private DatadogContextInterpreter(Map<String, String> taggedHeaders) {
            super(taggedHeaders);
        }

        public boolean accept(String key, String value) {
            String lowerCaseKey = null;
            int classification = -1;
            char first = Character.toLowerCase(key.charAt(0));
            switch(first) {
                case 'o':
                    lowerCaseKey = this.toLowerCase(key);
                    if (lowerCaseKey.startsWith("ot-baggage-")) {
                        classification = 5;
                    }
                    break;
                case 'x':
                    if ("x-datadog-trace-id".equalsIgnoreCase(key)) {
                        classification = 0;
                    } else if ("x-datadog-parent-id".equalsIgnoreCase(key)) {
                        classification = 1;
                    } else if ("x-datadog-sampling-priority".equalsIgnoreCase(key)) {
                        classification = 3;
                    } else if ("x-datadog-origin".equalsIgnoreCase(key)) {
                        classification = 2;
                    }
            }

            if (!this.taggedHeaders.isEmpty() && classification == -1) {
                lowerCaseKey = this.toLowerCase(key);
                if (this.taggedHeaders.containsKey(lowerCaseKey)) {
                    classification = 4;
                }
            }

            if (classification != -1) {
                try {
                    String firstValue = HttpCodec.firstHeaderValue(value);
                    if (null != firstValue) {
                        switch(classification) {
                            case 0:
                                this.traceId = DDId.from(firstValue);
                                break;
                            case 1:
                                this.spanId = DDId.from(firstValue);
                                break;
                            case 2:
                                this.origin = firstValue;
                                break;
                            case 3:
                                this.samplingPriority = Integer.parseInt(firstValue);
                                break;
                            case 4:
                                String mappedKey = (String)this.taggedHeaders.get(lowerCaseKey);
                                if (null != mappedKey) {
                                    if (this.tags.isEmpty()) {
                                        this.tags = new TreeMap();
                                    }

                                    this.tags.put(mappedKey, HttpCodec.decode(value));
                                }
                                break;
                            case 5:
                                if (this.baggage.isEmpty()) {
                                    this.baggage = new TreeMap();
                                }

                                this.baggage.put(lowerCaseKey.substring("ot-baggage-".length()), HttpCodec.decode(value));
                        }
                    }
                } catch (RuntimeException var8) {
                    this.invalidateContext();
                    log.error("Exception when extracting context", var8);
                    return false;
                }
            }

            return true;
        }
    }

    public static class Injector implements HttpCodec.Injector {
        public Injector() {
        }

        public <C> void inject(DDSpanContext context, C carrier, AgentPropagation.Setter<C> setter) {
            setter.set(carrier, "x-datadog-trace-id", context.getTraceId().toString());
            setter.set(carrier, "x-datadog-parent-id", context.getSpanId().toString());
            if (context.lockSamplingPriority()) {
                setter.set(carrier, "x-datadog-sampling-priority", String.valueOf(context.getSamplingPriority()));
            }

            String origin = context.getOrigin();
            if (origin != null) {
                setter.set(carrier, "x-datadog-origin", origin);
            }

            Iterator var5 = context.baggageItems().iterator();

            while(var5.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry)var5.next();
                setter.set(carrier, "ot-baggage-" + (String)entry.getKey(), HttpCodec.encode((String)entry.getValue()));
            }

            log.debug("{} - Datadog parent context injected", context.getTraceId());
        }
    }
}
