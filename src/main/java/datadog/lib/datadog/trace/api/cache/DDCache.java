package datadog.lib.datadog.trace.api.cache;

import datadog.lib.datadog.trace.api.Function;

public interface DDCache<K, V> {
    V computeIfAbsent(K var1, Function<K, ? extends V> var2);
}
