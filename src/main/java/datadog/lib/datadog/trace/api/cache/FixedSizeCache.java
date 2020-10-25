package datadog.lib.datadog.trace.api.cache;

import datadog.lib.datadog.trace.api.Function;

final class FixedSizeCache<K, V> implements DDCache<K, V> {
    static final int MAXIMUM_CAPACITY = 1073741824;
    private final int mask;
    private final Node<K, V>[] elements;

    public FixedSizeCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cache capacity must be > 0");
        } else {
            if (capacity > 1073741824) {
                capacity = 1073741824;
            }

            int n = -1 >>> Integer.numberOfLeadingZeros(capacity - 1);
            n = n < 0 ? 1 : (n >= 1073741824 ? 1073741824 : n + 1);
            Node<K, V>[] lmnts = (Node[])(new Node[n]);
            this.elements = lmnts;
            this.mask = n - 1;
        }
    }

    public V computeIfAbsent(K key, Function<K, ? extends V> creator) {
        if (key == null) {
            return null;
        } else {
            int h = key.hashCode();
            int firstPos = h & this.mask;
            int i = 1;

            V value;
            while(true) {
                int pos = h & this.mask;
                Node<K, V> current = this.elements[pos];
                if (current == null) {
                    value = this.createAndStoreValue(key, creator, pos);
                    break;
                }

                if (key.equals(current.key)) {
                    value = current.value;
                    break;
                }

                if (i == 3) {
                    value = this.createAndStoreValue(key, creator, firstPos);
                    break;
                }

                h = this.rehash(h);
                ++i;
            }

            return value;
        }
    }

    private V createAndStoreValue(K key, Function<K, ? extends V> creator, int pos) {
        V value = creator.apply(key);
        Node<K, V> node = new Node(key, value);
        this.elements[pos] = node;
        return value;
    }

    private int rehash(int v) {
        int h = v * -1640532531;
        h = Integer.reverseBytes(h);
        return h * -1640532531;
    }

    private static final class Node<K, V> {
        private final K key;
        private final V value;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
