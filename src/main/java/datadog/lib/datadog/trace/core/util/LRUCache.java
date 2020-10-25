package datadog.lib.datadog.trace.core.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public final class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75F;
    private final int maxEntries;

    public LRUCache(int maxEntries) {
        this(16, maxEntries);
    }

    public LRUCache(int initialCapacity, int maxEntries) {
        this(initialCapacity, 0.75F, maxEntries);
    }

    public LRUCache(int initialCapacity, float loadFactor, int maxEntries) {
        super(initialCapacity, loadFactor, true);
        this.maxEntries = maxEntries;
    }

    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return this.size() > this.maxEntries;
    }
}
