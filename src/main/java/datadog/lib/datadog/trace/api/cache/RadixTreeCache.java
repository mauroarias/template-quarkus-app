package datadog.lib.datadog.trace.api.cache;

import datadog.lib.datadog.trace.api.IntFunction;

import java.util.concurrent.atomic.AtomicReferenceArray;

public final class RadixTreeCache<T> {
    private static final IntFunction<Integer> AUTOBOX = new IntFunction<Integer>() {
        public Integer apply(int value) {
            return value;
        }
    };
    public static final RadixTreeCache<Integer> HTTP_STATUSES;
    public static final RadixTreeCache<Integer> PORTS;
    private final int level1;
    private final int level2;
    private final int shift;
    private final int mask;
    private final AtomicReferenceArray<Object[]> tree;
    private final IntFunction<T> mapper;

    public RadixTreeCache(int level1, int level2, IntFunction<T> mapper, int... commonValues) {
        this.tree = new AtomicReferenceArray(level1);
        this.mapper = mapper;
        this.level1 = level1;
        this.level2 = level2;
        this.mask = level2 - 1;
        this.shift = Integer.bitCount(this.mask);
        int[] var5 = commonValues;
        int var6 = commonValues.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            int commonValue = var5[var7];
            this.get(commonValue);
        }

    }

    public T get(int primitive) {
        int prefix = primitive >>> this.shift;
        return prefix >= this.level1 ? this.mapper.apply(primitive) : this.computeIfAbsent(prefix, primitive);
    }

    private T computeIfAbsent(int prefix, int primitive) {
        Object[] page = (Object[])this.tree.get(prefix);
        if (null == page) {
            page = new Object[this.level2];
            if (!this.tree.compareAndSet(prefix, null, page)) {
                page = (Object[])this.tree.get(prefix);
            }
        }

        int suffix = primitive & this.mask;
        Object cached = page[suffix];
        if (cached == null) {
            cached = page[suffix] = this.mapper.apply(primitive);
        }

        return (T)cached;
    }

    static {
        HTTP_STATUSES = new RadixTreeCache(16, 32, AUTOBOX, new int[]{200, 201, 301, 307, 400, 401, 403, 404, 500, 502, 503});
        PORTS = new RadixTreeCache(256, 256, AUTOBOX, new int[]{80, 443, 8080});
    }
}
