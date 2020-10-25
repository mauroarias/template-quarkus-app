package datadog.lib.datadog.trace.core.propagation;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.AgentPropagation.ContextVisitor;
import datadog.lib.datadog.trace.core.propagation.HttpCodec.Extractor;
import datadog.lib.datadog.trace.core.propagation.ContextInterpreter.Factory;
import java.util.Map;

public class TagContextExtractor implements Extractor {
    private final Map<String, String> taggedHeaders;
    private final ThreadLocal<ContextInterpreter> ctxInterpreter;

    public TagContextExtractor(final Map<String, String> taggedHeaders, final Factory factory) {
        this.taggedHeaders = taggedHeaders;
        this.ctxInterpreter = new ThreadLocal<ContextInterpreter>() {
            protected ContextInterpreter initialValue() {
                return factory.create(taggedHeaders);
            }
        };
    }

    public <C> TagContext extract(C carrier, ContextVisitor<C> getter) {
        ContextInterpreter interpreter = ((ContextInterpreter)this.ctxInterpreter.get()).reset();
        getter.forEachKey(carrier, interpreter);
        return interpreter.build();
    }
}
