package datadog.lib.datadog.trace.api.cache;

import datadog.lib.datadog.trace.api.Function;

import java.util.concurrent.ConcurrentHashMap;

final class CHMCache<K, V> implements DDCache<K, V> {
    private final ConcurrentHashMap<K, V> chm;

    public CHMCache(int initialCapacity) {
        this.chm = new ConcurrentHashMap(initialCapacity);
    }

    public V computeIfAbsent(K key, Function<K, ? extends V> func) {
        if (null == key) {
            return null;
        } else {
            V value = this.chm.get(key);
            if (null == value) {
                value = func.apply(key);
                V winner = this.chm.putIfAbsent(key, value);
                if (null != winner) {
                    value = winner;
                }
            }

            return value;
        }
    }
}
