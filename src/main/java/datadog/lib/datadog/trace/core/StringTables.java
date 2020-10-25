package datadog.lib.datadog.trace.core;

import datadog.trace.api.DDTags;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.InstrumentationTags;
import datadog.lib.datadog.trace.bootstrap.instrumentation.api.Tags;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StringTables {
    public static final byte[] SERVICE;
    public static final byte[] NAME;
    public static final byte[] RESOURCE;
    public static final byte[] TRACE_ID;
    public static final byte[] SPAN_ID;
    public static final byte[] PARENT_ID;
    public static final byte[] START;
    public static final byte[] DURATION;
    public static final byte[] TYPE;
    public static final byte[] ERROR;
    public static final byte[] METRICS;
    public static final byte[] META;
    private static final Map<CharSequence, byte[]> UTF8_INTERN_KEYS_TABLE;

    public StringTables() {
    }

    public static byte[] getKeyBytesUTF8(CharSequence value) {
        return (byte[])UTF8_INTERN_KEYS_TABLE.get(value);
    }

    private static void internConstantsUTF8(Class<?> clazz, Map<CharSequence, byte[]> map) {
        Field[] var2 = clazz.getDeclaredFields();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Field field = var2[var4];
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && field.getType() == String.class) {
                try {
                    intern(map, (String)field.get((Object)null), StandardCharsets.UTF_8);
                } catch (IllegalAccessException var7) {
                }
            }
        }

    }

    private static void intern(Map<CharSequence, byte[]> table, String value, Charset encoding) {
        byte[] bytes = value.getBytes(encoding);
        table.put(value, bytes);
    }

    static {
        SERVICE = "service".getBytes(StandardCharsets.UTF_8);
        NAME = "name".getBytes(StandardCharsets.UTF_8);
        RESOURCE = "resource".getBytes(StandardCharsets.UTF_8);
        TRACE_ID = "trace_id".getBytes(StandardCharsets.UTF_8);
        SPAN_ID = "span_id".getBytes(StandardCharsets.UTF_8);
        PARENT_ID = "parent_id".getBytes(StandardCharsets.UTF_8);
        START = "start".getBytes(StandardCharsets.UTF_8);
        DURATION = "duration".getBytes(StandardCharsets.UTF_8);
        TYPE = "type".getBytes(StandardCharsets.UTF_8);
        ERROR = "error".getBytes(StandardCharsets.UTF_8);
        METRICS = "metrics".getBytes(StandardCharsets.UTF_8);
        META = "meta".getBytes(StandardCharsets.UTF_8);
        UTF8_INTERN_KEYS_TABLE = new HashMap(256);
        internConstantsUTF8(DDSpanContext.class, UTF8_INTERN_KEYS_TABLE);
        internConstantsUTF8(DDTags.class, UTF8_INTERN_KEYS_TABLE);
        internConstantsUTF8(Tags.class, UTF8_INTERN_KEYS_TABLE);
        internConstantsUTF8(InstrumentationTags.class, UTF8_INTERN_KEYS_TABLE);
        intern(UTF8_INTERN_KEYS_TABLE, "_dd.agent_psr", StandardCharsets.UTF_8);
    }
}
