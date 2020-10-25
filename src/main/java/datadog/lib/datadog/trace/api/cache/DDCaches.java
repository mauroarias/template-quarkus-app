package datadog.lib.datadog.trace.api.cache;

public final class DDCaches {
    private DDCaches() {
    }

    public static <K, V> DDCache<K, V> newFixedSizeCache(int capacity) {
        return new FixedSizeCache(capacity);
    }

    public static <K, V> DDCache<K, V> newUnboundedCache(int initialCapacity) {
        return new CHMCache(initialCapacity);
    }
}
