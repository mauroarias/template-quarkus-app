package datadog.lib.datadog.trace.core;

import java.util.Map;

public abstract class TagsAndBaggageConsumer {
    public TagsAndBaggageConsumer() {
    }

    public abstract void accept(Map<String, Object> var1, Map<String, String> var2);
}
