package datadog.lib.datadog.trace.core.propagation;

import datadog.trace.api.DDId;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.lib.datadog.trace.core.DDSpanContext;
import datadog.lib.datadog.trace.core.propagation.HttpCodec.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

class B3HttpCodec {
    private static final Logger log = LoggerFactory.getLogger(B3HttpCodec.class);
    private static final String TRACE_ID_KEY = "X-B3-TraceId";
    private static final String SPAN_ID_KEY = "X-B3-SpanId";
    private static final String SAMPLING_PRIORITY_KEY = "X-B3-Sampled";
    private static final String SAMPLING_PRIORITY_ACCEPT = String.valueOf(1);
    private static final String SAMPLING_PRIORITY_DROP = String.valueOf(0);

    private B3HttpCodec() {
    }

    public static Extractor newExtractor(Map<String, String> tagMapping) {
        return new TagContextExtractor(tagMapping, new ContextInterpreter.Factory() {
            protected ContextInterpreter construct(Map<String, String> mapping) {
                return new B3ContextInterpreter(mapping);
            }
        });
    }

    private static class B3ContextInterpreter extends ContextInterpreter {
        private static final int TRACE_ID = 0;
        private static final int SPAN_ID = 1;
        private static final int TAGS = 2;
        private static final int SAMPLING_PRIORITY = 3;
        private static final int IGNORE = -1;

        private B3ContextInterpreter(Map<String, String> taggedHeaders) {
            super(taggedHeaders);
        }

        public boolean accept(String key, String value) {
            String lowerCaseKey = null;
            int classification = -1;
            if (Character.toLowerCase(key.charAt(0)) == 'x') {
                if ("X-B3-TraceId".equalsIgnoreCase(key)) {
                    classification = 0;
                } else if ("X-B3-SpanId".equalsIgnoreCase(key)) {
                    classification = 1;
                } else if ("X-B3-Sampled".equalsIgnoreCase(key)) {
                    classification = 3;
                }
            }

            if (!this.taggedHeaders.isEmpty() && classification == -1) {
                lowerCaseKey = this.toLowerCase(key);
                if (this.taggedHeaders.containsKey(lowerCaseKey)) {
                    classification = 2;
                }
            }

            if (classification != -1) {
                try {
                    String firstValue = HttpCodec.firstHeaderValue(value);
                    if (null != firstValue) {
                        String trimmedValue;
                        switch(classification) {
                            case 0:
                                int length = firstValue.length();
                                if (length > 32) {
                                    log.debug("Header {} exceeded max length of 32: {}", "X-B3-TraceId", value);
                                    this.traceId = DDId.ZERO;
                                    return true;
                                }

                                if (length > 16) {
                                    trimmedValue = value.substring(length - 16);
                                } else {
                                    trimmedValue = value;
                                }

                                this.traceId = DDId.fromHex(trimmedValue);
                                break;
                            case 1:
                                this.spanId = DDId.fromHex(firstValue);
                                break;
                            case 2:
                                trimmedValue = (String)this.taggedHeaders.get(lowerCaseKey);
                                if (null != trimmedValue) {
                                    if (this.tags.isEmpty()) {
                                        this.tags = new TreeMap();
                                    }

                                    this.tags.put(trimmedValue, HttpCodec.decode(firstValue));
                                }
                                break;
                            case 3:
                                this.samplingPriority = this.convertSamplingPriority(firstValue);
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

        private int convertSamplingPriority(String samplingPriority) {
            return "1".equals(samplingPriority) ? 1 : 0;
        }
    }

    public static class Injector implements HttpCodec.Injector {
        public Injector() {
        }

        public <C> void inject(DDSpanContext context, C carrier, AgentPropagation.Setter<C> setter) {
            try {
                String injectedTraceId = context.getTraceId().toHexString().toLowerCase();
                setter.set(carrier, "X-B3-TraceId", injectedTraceId);
                setter.set(carrier, "X-B3-SpanId", context.getSpanId().toHexString().toLowerCase());
                if (context.lockSamplingPriority()) {
                    setter.set(carrier, "X-B3-Sampled", this.convertSamplingPriority(context.getSamplingPriority()));
                }

                log.debug("{} - B3 parent context injected - {}", context.getTraceId(), injectedTraceId);
            } catch (NumberFormatException var5) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot parse context id(s): {} {}", new Object[]{context.getTraceId(), context.getSpanId(), var5});
                }
            }

        }

        private String convertSamplingPriority(int samplingPriority) {
            return samplingPriority > 0 ? SAMPLING_PRIORITY_ACCEPT : SAMPLING_PRIORITY_DROP;
        }
    }
}
