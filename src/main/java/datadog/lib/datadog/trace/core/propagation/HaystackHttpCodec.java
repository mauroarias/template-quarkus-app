package datadog.lib.datadog.trace.core.propagation;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.lib.datadog.trace.core.DDSpanContext;
import datadog.trace.api.DDId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class HaystackHttpCodec {
    private static final Logger log = LoggerFactory.getLogger(HaystackHttpCodec.class);
    private static final String OT_BAGGAGE_PREFIX = "Baggage-";
    private static final String TRACE_ID_KEY = "Trace-ID";
    private static final String SPAN_ID_KEY = "Span-ID";
    private static final String PARENT_ID_KEY = "Parent-ID";
    private static final String DD_TRACE_ID_BAGGAGE_KEY = "Baggage-Datadog-Trace-Id";
    private static final String DD_SPAN_ID_BAGGAGE_KEY = "Baggage-Datadog-Span-Id";
    private static final String DD_PARENT_ID_BAGGAGE_KEY = "Baggage-Datadog-Parent-Id";
    private static final String HAYSTACK_TRACE_ID_BAGGAGE_KEY = "Haystack-Trace-ID";
    private static final String HAYSTACK_SPAN_ID_BAGGAGE_KEY = "Haystack-Span-ID";
    private static final String HAYSTACK_PARENT_ID_BAGGAGE_KEY = "Haystack-Parent-ID";
    public static final String DATADOG = "44617461-646f-6721";

    private HaystackHttpCodec() {
    }

    public static HttpCodec.Extractor newExtractor(Map<String, String> tagMapping) {
        return new TagContextExtractor(tagMapping, new ContextInterpreter.Factory() {
            protected ContextInterpreter construct(Map<String, String> mapping) {
                return new HaystackContextInterpreter(mapping);
            }
        });
    }

    private static String convertBigIntToUUID(DDId id) {
        String idHex = String.format("%016x", id.toLong());
        return "44617461-646f-6721-" + idHex.substring(0, 4) + "-" + idHex.substring(4);
    }

    private static DDId convertUUIDToBigInt(String value) {
        try {
            if (value.contains("-")) {
                String[] strings = value.split("-");
                if (strings.length == 5) {
                    String idHex = strings[3] + strings[4];
                    return DDId.fromHex(idHex);
                } else {
                    throw new NumberFormatException("Invalid UUID format: " + value);
                }
            } else {
                int length = value.length();
                return length == 32 ? DDId.fromHex(value.substring(16)) : DDId.fromHex(value);
            }
        } catch (Exception var3) {
            throw new IllegalArgumentException("Exception when converting UUID to BigInteger: " + value, var3);
        }
    }

    private static class HaystackContextInterpreter extends ContextInterpreter {
        private static final String BAGGAGE_PREFIX_LC = "baggage-";
        private static final int TRACE_ID = 0;
        private static final int SPAN_ID = 1;
        private static final int PARENT_ID = 2;
        private static final int TAGS = 3;
        private static final int BAGGAGE = 4;
        private static final int IGNORE = -1;

        private HaystackContextInterpreter(Map<String, String> taggedHeaders) {
            super(taggedHeaders);
        }

        public boolean accept(String key, String value) {
            char first = Character.toLowerCase(key.charAt(0));
            String lowerCaseKey = null;
            int classification = -1;
            switch(first) {
                case 'b':
                    lowerCaseKey = this.toLowerCase(key);
                    if (lowerCaseKey.startsWith("baggage-")) {
                        classification = 4;
                    }
                    break;
                case 'p':
                    if ("Parent-ID".equalsIgnoreCase(key)) {
                        classification = 2;
                    }
                    break;
                case 's':
                    if ("Span-ID".equalsIgnoreCase(key)) {
                        classification = 1;
                    }
                    break;
                case 't':
                    if ("Trace-ID".equalsIgnoreCase(key)) {
                        classification = 0;
                    }
            }

            if (!this.taggedHeaders.isEmpty() && classification == -1) {
                lowerCaseKey = this.toLowerCase(key);
                if (this.taggedHeaders.containsKey(lowerCaseKey)) {
                    classification = 3;
                }
            }

            if (-1 != classification) {
                try {
                    String firstValue = HttpCodec.firstHeaderValue(value);
                    if (null != firstValue) {
                        switch(classification) {
                            case 0:
                                this.traceId = convertUUIDToBigInt(value);
                                this.addBaggageItem("Haystack-Trace-ID", HttpCodec.decode(value));
                                break;
                            case 1:
                                this.spanId = convertUUIDToBigInt(value);
                                this.addBaggageItem("Haystack-Span-ID", HttpCodec.decode(value));
                                break;
                            case 2:
                                this.addBaggageItem("Haystack-Parent-ID", HttpCodec.decode(value));
                                break;
                            case 3:
                                String mappedKey = (String)this.taggedHeaders.get(lowerCaseKey);
                                if (null != mappedKey) {
                                    if (this.tags.isEmpty()) {
                                        this.tags = new TreeMap();
                                    }

                                    this.tags.put(mappedKey, HttpCodec.decode(value));
                                }
                                break;
                            case 4:
                                this.addBaggageItem(lowerCaseKey.substring("baggage-".length()), HttpCodec.decode(value));
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

        private void addBaggageItem(String key, String value) {
            if (this.baggage.isEmpty()) {
                this.baggage = new TreeMap();
            }

            this.baggage.put(key, HttpCodec.decode(value));
        }

        protected int defaultSamplingPriority() {
            return 1;
        }
    }

    public static class Injector implements HttpCodec.Injector {
        public Injector() {
        }

        public <C> void inject(DDSpanContext context, C carrier, AgentPropagation.Setter<C> setter) {
            try {
                String originalHaystackTraceId = this.getBaggageItemIgnoreCase(context.getBaggageItems(), "Haystack-Trace-ID");
                String injectedTraceId;
                if (originalHaystackTraceId != null && convertUUIDToBigInt(originalHaystackTraceId).equals(context.getTraceId())) {
                    injectedTraceId = originalHaystackTraceId;
                } else {
                    injectedTraceId = convertBigIntToUUID(context.getTraceId());
                }

                setter.set(carrier, "Trace-ID", injectedTraceId);
                context.setTag("Haystack-Trace-ID", injectedTraceId);
                setter.set(carrier, "Baggage-Datadog-Trace-Id", HttpCodec.encode(context.getTraceId().toString()));
                setter.set(carrier, "Span-ID", convertBigIntToUUID(context.getSpanId()));
                setter.set(carrier, "Baggage-Datadog-Span-Id", HttpCodec.encode(context.getSpanId().toString()));
                setter.set(carrier, "Parent-ID", convertBigIntToUUID(context.getParentId()));
                setter.set(carrier, "Baggage-Datadog-Parent-Id", HttpCodec.encode(context.getParentId().toString()));
                Iterator var6 = context.baggageItems().iterator();

                while(var6.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var6.next();
                    setter.set(carrier, "Baggage-" + (String)entry.getKey(), HttpCodec.encode((String)entry.getValue()));
                }

                log.debug("{} - Haystack parent context injected - {}", context.getTraceId(), injectedTraceId);
            } catch (NumberFormatException var8) {
                log.debug("Cannot parse context id(s): {} {}", new Object[]{context.getTraceId(), context.getSpanId(), var8});
            }

        }

        private String getBaggageItemIgnoreCase(Map<String, String> baggage, String key) {
            Iterator var3 = baggage.entrySet().iterator();

            Map.Entry mapping;
            do {
                if (!var3.hasNext()) {
                    return null;
                }

                mapping = (Map.Entry)var3.next();
            } while(!key.equalsIgnoreCase((String)mapping.getKey()));

            return (String)mapping.getValue();
        }
    }
}
