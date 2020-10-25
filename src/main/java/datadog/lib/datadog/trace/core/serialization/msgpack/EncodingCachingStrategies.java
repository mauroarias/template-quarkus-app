package datadog.lib.datadog.trace.core.serialization.msgpack;

import datadog.lib.datadog.trace.core.StringTables;

public class EncodingCachingStrategies {
    public static final EncodingCache CONSTANT_KEYS = new EncodingCachingStrategies.ConstantKeys();
    public static final EncodingCache NO_CACHING = null;

    public EncodingCachingStrategies() {
    }

    private static final class ConstantKeys implements EncodingCache {
        private ConstantKeys() {
        }

        public byte[] encode(CharSequence s) {
            return StringTables.getKeyBytesUTF8(s);
        }
    }
}
