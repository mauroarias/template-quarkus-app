package datadog.lib.datadog.trace.core.serialization.msgpack;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public final class Codec extends ClassValue<Writer<?>> {
    public static final Codec INSTANCE = new Codec();
    private final Map<Class<?>, Writer<?>> config;

    public Codec(Map<Class<?>, Writer<?>> config) {
        this.config = config;
    }

    public Codec() {
        this(Collections.emptyMap());
    }

    protected Writer<?> computeValue(Class<?> clazz) {
        Writer<?> writer = (Writer)this.config.get(clazz);
        if (null != writer) {
            return writer;
        } else {
            if (Number.class.isAssignableFrom(clazz)) {
                if (Double.class == clazz) {
                    return new Codec.DoubleWriter();
                }

                if (Float.class == clazz) {
                    return new Codec.FloatWriter();
                }

                if (Integer.class == clazz) {
                    return new Codec.IntWriter();
                }

                if (Long.class == clazz) {
                    return new Codec.LongWriter();
                }

                if (Short.class == clazz) {
                    return new Codec.ShortWriter();
                }
            }

            if (clazz.isArray()) {
                if (byte[].class == clazz) {
                    return new Codec.ByteArrayWriter();
                } else if (int[].class == clazz) {
                    return new Codec.IntArrayWriter();
                } else if (long[].class == clazz) {
                    return new Codec.LongArrayWriter();
                } else if (double[].class == clazz) {
                    return new Codec.DoubleArrayWriter();
                } else if (float[].class == clazz) {
                    return new Codec.FloatArrayWriter();
                } else if (short[].class == clazz) {
                    return new Codec.ShortArrayWriter();
                } else if (char[].class == clazz) {
                    return new Codec.CharArrayWriter();
                } else {
                    return (Writer)(boolean[].class == clazz ? new Codec.BooleanArrayWriter() : new Codec.ObjectArrayWriter());
                }
            } else if (Boolean.class == clazz) {
                return new Codec.BooleanWriter();
            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                return Codec.CharSequenceWriter.INSTANCE;
            } else if (Map.class.isAssignableFrom(clazz)) {
                return new Codec.MapWriter();
            } else if (Collection.class.isAssignableFrom(clazz)) {
                return new Codec.CollectionWriter();
            } else {
                return (Writer)(ByteBuffer.class.isAssignableFrom(clazz) ? new Codec.ByteBufferWriter() : Codec.DefaultWriter.INSTANCE);
            }
        }
    }

    private static final class DefaultWriter implements Writer<Object> {
        public static final Codec.DefaultWriter INSTANCE = new Codec.DefaultWriter();

        private DefaultWriter() {
        }

        public void write(Object value, Packer packer, EncodingCache encodingCache) {
            Codec.CharSequenceWriter.INSTANCE.write((CharSequence)String.valueOf(value), packer, EncodingCachingStrategies.NO_CACHING);
        }
    }

    private static final class CharArrayWriter implements Writer<char[]> {
        private CharArrayWriter() {
        }

        public void write(char[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeString(CharBuffer.wrap(value), EncodingCachingStrategies.NO_CACHING);
        }
    }

    private static final class CharSequenceWriter implements Writer<CharSequence> {
        public static final Codec.CharSequenceWriter INSTANCE = new Codec.CharSequenceWriter();

        private CharSequenceWriter() {
        }

        public void write(CharSequence value, Packer packer, EncodingCache encodingCache) {
            packer.writeString(value, encodingCache);
        }
    }

    private static final class LongWriter implements Writer<Long> {
        private LongWriter() {
        }

        public void write(Long value, Packer packer, EncodingCache encodingCache) {
            packer.writeLong(value);
        }
    }

    private static final class ShortWriter implements Writer<Short> {
        private ShortWriter() {
        }

        public void write(Short value, Packer packer, EncodingCache encodingCache) {
            packer.writeInt(value);
        }
    }

    private static final class IntWriter implements Writer<Integer> {
        private IntWriter() {
        }

        public void write(Integer value, Packer packer, EncodingCache encodingCache) {
            packer.writeInt(value);
        }
    }

    private static final class FloatWriter implements Writer<Float> {
        private FloatWriter() {
        }

        public void write(Float value, Packer packer, EncodingCache encodingCache) {
            packer.writeFloat(value);
        }
    }

    private static final class BooleanWriter implements Writer<Boolean> {
        private BooleanWriter() {
        }

        public void write(Boolean value, Packer packer, EncodingCache encodingCache) {
            packer.writeBoolean(value);
        }
    }

    private static final class DoubleWriter implements Writer<Double> {
        private DoubleWriter() {
        }

        public void write(Double value, Packer packer, EncodingCache encodingCache) {
            packer.writeDouble(value);
        }
    }

    private static final class MapWriter implements Writer<Map<? extends CharSequence, Object>> {
        private MapWriter() {
        }

        public void write(Map<? extends CharSequence, Object> value, Packer packer, EncodingCache encodingCache) {
            packer.writeMap(value, encodingCache);
        }
    }

    private static final class ObjectArrayWriter implements Writer<Object[]> {
        private ObjectArrayWriter() {
        }

        public void write(Object[] array, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(array.length);
            Object[] var4 = array;
            int var5 = array.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Object value = var4[var6];
                packer.writeObject(value, encodingCache);
            }

        }
    }

    private static final class CollectionWriter implements Writer<Collection<?>> {
        private CollectionWriter() {
        }

        public void write(Collection<?> collection, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(collection.size());
            Iterator var4 = collection.iterator();

            while(var4.hasNext()) {
                Object value = var4.next();
                packer.writeObject(value, encodingCache);
            }

        }
    }

    private static final class LongArrayWriter implements Writer<long[]> {
        private LongArrayWriter() {
        }

        public void write(long[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(value.length);
            long[] var4 = value;
            int var5 = value.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                long i = var4[var6];
                packer.writeLong(i);
            }

        }
    }

    private static final class FloatArrayWriter implements Writer<float[]> {
        private FloatArrayWriter() {
        }

        public void write(float[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(value.length);
            float[] var4 = value;
            int var5 = value.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                float i = var4[var6];
                packer.writeFloat(i);
            }

        }
    }

    private static final class DoubleArrayWriter implements Writer<double[]> {
        private DoubleArrayWriter() {
        }

        public void write(double[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(value.length);
            double[] var4 = value;
            int var5 = value.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                double i = var4[var6];
                packer.writeDouble(i);
            }

        }
    }

    private static final class BooleanArrayWriter implements Writer<boolean[]> {
        private BooleanArrayWriter() {
        }

        public void write(boolean[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(value.length);
            boolean[] var4 = value;
            int var5 = value.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                boolean i = var4[var6];
                packer.writeBoolean(i);
            }

        }
    }

    private static final class ByteBufferWriter implements Writer<ByteBuffer> {
        private ByteBufferWriter() {
        }

        public void write(ByteBuffer buffer, Packer packer, EncodingCache encodingCache) {
            packer.writeBinary(buffer);
        }
    }

    private static final class ByteArrayWriter implements Writer<byte[]> {
        private ByteArrayWriter() {
        }

        public void write(byte[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeBinary(value, 0, value.length);
        }
    }

    private static final class ShortArrayWriter implements Writer<short[]> {
        private ShortArrayWriter() {
        }

        public void write(short[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(value.length);
            short[] var4 = value;
            int var5 = value.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                short i = var4[var6];
                packer.writeInt(i);
            }

        }
    }

    private static final class IntArrayWriter implements Writer<int[]> {
        private IntArrayWriter() {
        }

        public void write(int[] value, Packer packer, EncodingCache encodingCache) {
            packer.writeArrayHeader(value.length);
            int[] var4 = value;
            int var5 = value.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                int i = var4[var6];
                packer.writeInt(i);
            }

        }
    }
}
