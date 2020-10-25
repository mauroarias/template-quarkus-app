package datadog.lib.datadog.trace.core.propagation;

import datadog.lib.datadog.trace.api.Config;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.lib.datadog.trace.core.DDSpanContext;
import datadog.trace.api.PropagationStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpCodec {
    private static final Logger log = LoggerFactory.getLogger(HttpCodec.class);

    public HttpCodec() {
    }

    public static Injector createInjector(Config config) {
        List<Injector> injectors = new ArrayList();
        Iterator var2 = config.getPropagationStylesToInject().iterator();

        while(var2.hasNext()) {
            PropagationStyle style = (PropagationStyle)var2.next();
            if (style == PropagationStyle.DATADOG) {
                injectors.add(new DatadogHttpCodec.Injector());
            } else if (style == PropagationStyle.B3) {
                injectors.add(new B3HttpCodec.Injector());
            } else if (style == PropagationStyle.HAYSTACK) {
                injectors.add(new HaystackHttpCodec.Injector());
            } else {
                log.debug("No implementation found to inject propagation style: {}", style);
            }
        }

        return new CompoundInjector(injectors);
    }

    public static Extractor createExtractor(Config config, Map<String, String> taggedHeaders) {
        List<Extractor> extractors = new ArrayList();
        Iterator var3 = config.getPropagationStylesToExtract().iterator();

        while(var3.hasNext()) {
            PropagationStyle style = (PropagationStyle)var3.next();
            switch(style) {
                case DATADOG:
                    extractors.add(DatadogHttpCodec.newExtractor(taggedHeaders));
                    break;
                case HAYSTACK:
                    extractors.add(HaystackHttpCodec.newExtractor(taggedHeaders));
                    break;
                case B3:
                    extractors.add(B3HttpCodec.newExtractor(taggedHeaders));
                    break;
                default:
                    log.debug("No implementation found to extract propagation style: {}", style);
            }
        }

        return new CompoundExtractor(extractors);
    }

    static String encode(String value) {
        String encoded = value;

        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            log.debug("Failed to encode value - {}", value);
        }

        return encoded;
    }

    static String decode(String value) {
        String decoded = value;

        try {
            decoded = URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            log.debug("Failed to decode value - {}", value);
        }

        return decoded;
    }

    static String firstHeaderValue(String value) {
        if (value == null) {
            return null;
        } else {
            int firstComma = value.indexOf(44);
            return firstComma == -1 ? value : value.substring(0, firstComma).trim();
        }
    }

    public static class CompoundExtractor implements Extractor {
        private final List<Extractor> extractors;

        public CompoundExtractor(List<Extractor> extractors) {
            this.extractors = extractors;
        }

        public <C> TagContext extract(C carrier, AgentPropagation.ContextVisitor<C> setter) {
            TagContext context = null;
            Iterator var4 = this.extractors.iterator();

            do {
                if (!var4.hasNext()) {
                    return context;
                }

                Extractor extractor = (Extractor)var4.next();
                context = extractor.extract(carrier, setter);
            } while(!(context instanceof ExtractedContext));

            return context;
        }
    }

    public static class CompoundInjector implements Injector {
        private final List<Injector> injectors;

        public CompoundInjector(List<Injector> injectors) {
            this.injectors = injectors;
        }

        public <C> void inject(DDSpanContext context, C carrier, AgentPropagation.Setter<C> setter) {
            Iterator var4 = this.injectors.iterator();

            while(var4.hasNext()) {
                Injector injector = (Injector)var4.next();
                injector.inject(context, carrier, setter);
            }

        }
    }

    public interface Extractor {
        <C> TagContext extract(C var1, AgentPropagation.ContextVisitor<C> var2);
    }

    public interface Injector {
        <C> void inject(DDSpanContext var1, C var2, AgentPropagation.Setter<C> var3);
    }
}
