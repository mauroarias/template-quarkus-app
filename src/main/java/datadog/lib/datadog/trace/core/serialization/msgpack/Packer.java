package datadog.lib.datadog.trace.core.serialization.msgpack;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Packer implements Writable, MessageFormatter {
    private static final boolean IS_JVM_9_OR_LATER = !System.getProperty("java.version").startsWith("1.");
    private static final int MAX_ARRAY_HEADER_SIZE = 5;
    private static final byte NULL = -64;
    private static final byte FALSE = -62;
    private static final byte TRUE = -61;
    private static final byte UINT8 = -52;
    private static final byte UINT16 = -51;
    private static final byte UINT32 = -50;
    private static final byte UINT64 = -49;
    private static final byte INT8 = -48;
    private static final byte INT16 = -47;
    private static final byte INT32 = -46;
    private static final byte INT64 = -45;
    private static final byte FLOAT32 = -54;
    private static final byte FLOAT64 = -53;
    private static final byte STR8 = -39;
    private static final byte STR16 = -38;
    private static final byte STR32 = -37;
    private static final byte BIN8 = -60;
    private static final byte BIN16 = -59;
    private static final byte BIN32 = -58;
    private static final byte ARRAY16 = -36;
    private static final byte ARRAY32 = -35;
    private static final byte MAP16 = -34;
    private static final byte MAP32 = -33;
    private static final int NEGFIXNUM = 224;
    private static final int FIXSTR = 160;
    private static final int FIXARRAY = 144;
    private static final int FIXMAP = 128;
    private final Codec codec;
    private final ByteBufferConsumer sink;
    private final ByteBuffer buffer;
    private final boolean manualReset;
    private int messageCount;

    public Packer(Codec codec, ByteBufferConsumer sink, ByteBuffer buffer, boolean manualReset) {
        this.messageCount = 0;
        this.codec = codec;
        this.sink = sink;
        this.buffer = buffer;
        this.initBuffer();
        this.manualReset = manualReset;
    }

    public Packer(Codec codec, ByteBufferConsumer sink, ByteBuffer buffer) {
        this(codec, sink, buffer, false);
    }

    public Packer(ByteBufferConsumer sink, ByteBuffer buffer) {
        this(Codec.INSTANCE, sink, buffer);
    }

    public Packer(ByteBufferConsumer sink, ByteBuffer buffer, boolean manualReset) {
        this(Codec.INSTANCE, sink, buffer, manualReset);
    }

    private void initBuffer() {
        this.buffer.position(5);
        this.buffer.mark();
    }

    public <T> boolean format(T message, Mapper<T> mapper) {
        try {
            mapper.map(message, this);
            this.buffer.mark();
            ++this.messageCount;
            return true;
        } catch (BufferOverflowException var4) {
            this.buffer.reset();
            if (!this.manualReset) {
                if (this.buffer.position() == 5) {
                    throw var4;
                } else {
                    this.flush();
                    return this.format(message, mapper);
                }
            } else {
                return false;
            }
        }
    }

    public int messageCount() {
        return this.messageCount;
    }

    public void reset() {
        this.buffer.position(5);
        this.initBuffer();
        this.buffer.limit(this.buffer.capacity());
        this.messageCount = 0;
    }

    public void flush() {
        this.buffer.flip();
        int pos = 0;
        if (this.messageCount < 16) {
            pos = 4;
        } else if (this.messageCount < 65536) {
            pos = 2;
        }

        this.buffer.position(pos);
        this.writeArrayHeader(this.messageCount);
        this.buffer.position(pos);
        this.sink.accept(this.messageCount, this.buffer.slice());
        if (!this.manualReset) {
            this.reset();
        }

    }

    public void writeNull() {
        this.buffer.put((byte)-64);
    }

    public void writeBoolean(boolean value) {
        this.buffer.put((byte)(value ? -61 : -62));
    }

    public void writeObject(Object value, EncodingCache encodingCache) {
        if (value instanceof UTF8BytesString) {
            this.writeUTF8((UTF8BytesString)value);
        } else if (null == value) {
            this.writeNull();
        } else {
            Writer writer = (Writer)this.codec.get(value.getClass());
            writer.write(value, this, encodingCache);
        }

    }

    public void writeMap(Map<? extends CharSequence, ? extends Object> map, EncodingCache encodingCache) {
        this.writeMapHeader(map.size());
        Iterator var3 = map.entrySet().iterator();

        while(var3.hasNext()) {
            Entry<? extends CharSequence, ? extends Object> entry = (Entry)var3.next();
            this.writeString((CharSequence)entry.getKey(), encodingCache);
            this.writeObject(entry.getValue(), encodingCache);
        }

    }

    public void writeString(CharSequence s, EncodingCache encodingCache) {
        if (null == s) {
            this.writeNull();
        } else {
            if (null != encodingCache) {
                byte[] utf8 = encodingCache.encode(s);
                if (null != utf8) {
                    this.writeUTF8(utf8);
                    return;
                }
            }

            this.writeUTF8String(s);
        }

    }

    private void writeUTF8String(CharSequence s) {
        int mark = this.buffer.position();
        this.writeStringHeader(s.length());
        int actualLength = this.utf8Encode(s);
        if (actualLength > s.length()) {
            int lengthWritten = stringLength(s.length());
            int lengthRequired = stringLength(actualLength);
            if (lengthRequired != lengthWritten) {
                this.buffer.position(mark);
                this.writeStringHeader(actualLength);
                this.utf8Encode(s);
            } else {
                this.fixStringHeaderInPlace(mark, lengthRequired, actualLength);
            }
        }

    }

    private int utf8Encode(CharSequence s) {
        if (IS_JVM_9_OR_LATER && s.length() < 64 && s instanceof String) {
            byte[] utf8 = ((String)s).getBytes(StandardCharsets.UTF_8);
            this.buffer.put(utf8);
            return utf8.length;
        } else {
            return this.allocationFreeUTF8Encode(s);
        }
    }

    private int allocationFreeUTF8Encode(CharSequence s) {
        int written = 0;

        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c < 128) {
                this.buffer.put((byte)c);
                ++written;
            } else if (c < 2048) {
                this.buffer.putChar((char)((192 | c >> 6) << 8 | 128 | c & 63));
                written += 2;
            } else if (Character.isSurrogate(c)) {
                if (!Character.isHighSurrogate(c)) {
                    this.buffer.put((byte)63);
                    ++written;
                } else {
                    ++i;
                    if (i == s.length()) {
                        this.buffer.put((byte)63);
                        ++written;
                    } else {
                        char next = s.charAt(i);
                        if (!Character.isLowSurrogate(next)) {
                            this.buffer.put((byte)63);
                            this.buffer.put(Character.isHighSurrogate(next) ? 63 : (byte)next);
                            written += 2;
                        } else {
                            int codePoint = Character.toCodePoint(c, next);
                            this.buffer.putInt((240 | codePoint >> 18) << 24 | (128 | codePoint >> 12 & 63) << 16 | (128 | codePoint >> 6 & 63) << 8 | 128 | codePoint & 63);
                            written += 4;
                        }
                    }
                }
            } else {
                this.buffer.putChar((char)((224 | c >> 12) << 8 | 128 | c >> 6 & 63));
                this.buffer.put((byte)(128 | c & 63));
                written += 3;
            }
        }

        return written;
    }

    public void writeUTF8(byte[] string, int offset, int length) {
        this.writeStringHeader(length);
        this.buffer.put(string, offset, length);
    }

    public void writeUTF8(byte[] string) {
        this.writeUTF8(string, 0, string.length);
    }

    public void writeUTF8(UTF8BytesString string) {
        this.writeStringHeader(string.encodedLength());
        string.transferTo(this.buffer);
    }

    public void writeBinary(byte[] binary, int offset, int length) {
        this.writeBinaryHeader(length);
        this.buffer.put(binary, offset, length);
    }

    public void writeBinary(ByteBuffer binary) {
        ByteBuffer slice = binary.slice();
        this.writeBinaryHeader(slice.limit() - slice.position());
        this.buffer.put(slice);
    }

    public void writeInt(int value) {
        if (value < 0) {
            switch(Integer.numberOfLeadingZeros(~value)) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    this.buffer.put((byte)-46);
                    this.buffer.putInt(value);
                    break;
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                    this.buffer.put((byte)-47);
                    this.buffer.putChar((char)value);
                    break;
                case 25:
                case 26:
                    this.buffer.put((byte)-48);
                    this.buffer.put((byte)value);
                    break;
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                default:
                    this.buffer.put((byte)(224 | value));
            }
        } else {
            switch(Integer.numberOfLeadingZeros(value)) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                    this.buffer.put((byte)-50);
                    this.buffer.putInt(value);
                    break;
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                    this.buffer.put((byte)-51);
                    this.buffer.putChar((char)value);
                    break;
                case 24:
                    this.buffer.put((byte)-52);
                    this.buffer.put((byte)value);
                    break;
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                default:
                    this.buffer.put((byte)value);
            }
        }

    }

    public void writeLong(long value) {
        if (value < 0L) {
            switch(Long.numberOfLeadingZeros(~value)) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                    this.buffer.put((byte)-45);
                    this.buffer.putLong(value);
                    break;
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                    this.buffer.put((byte)-46);
                    this.buffer.putInt((int)value);
                    break;
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                    this.buffer.put((byte)-47);
                    this.buffer.putChar((char)((int)value));
                    break;
                case 57:
                case 58:
                    this.buffer.put((byte)-48);
                    this.buffer.put((byte)((int)value));
                    break;
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                default:
                    this.buffer.put((byte)((int)(224L | value)));
            }
        } else {
            switch(Long.numberOfLeadingZeros(value)) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                    this.buffer.put((byte)-49);
                    this.buffer.putLong(value);
                    break;
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                    this.buffer.put((byte)-50);
                    this.buffer.putInt((int)value);
                    break;
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                    this.buffer.put((byte)-51);
                    this.buffer.putChar((char)((int)value));
                    break;
                case 56:
                    this.buffer.put((byte)-52);
                    this.buffer.put((byte)((int)value));
                    break;
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                default:
                    this.buffer.put((byte)((int)value));
            }
        }

    }

    public void writeFloat(float value) {
        this.buffer.put((byte)-54);
        this.buffer.putFloat(value);
    }

    public void writeDouble(double value) {
        this.buffer.put((byte)-53);
        this.buffer.putDouble(value);
    }

    public void startMap(int elementCount) {
        this.writeMapHeader(elementCount);
    }

    public void startArray(int elementCount) {
        this.writeArrayHeader(elementCount);
    }

    void writeStringHeader(int length) {
        if (length < 16) {
            this.buffer.put((byte)(160 | length));
        } else if (length < 256) {
            this.buffer.put((byte)-39);
            this.buffer.put((byte)length);
        } else if (length < 65536) {
            this.buffer.put((byte)-38);
            this.buffer.putChar((char)length);
        } else {
            this.buffer.put((byte)-37);
            this.buffer.putInt(length);
        }

    }

    void writeArrayHeader(int length) {
        if (length < 16) {
            this.buffer.put((byte)(144 | length));
        } else if (length < 65536) {
            this.buffer.put((byte)-36);
            this.buffer.putChar((char)length);
        } else {
            this.buffer.put((byte)-35);
            this.buffer.putInt(length);
        }

    }

    void writeMapHeader(int length) {
        if (length < 16) {
            this.buffer.put((byte)(128 | length));
        } else if (length < 65536) {
            this.buffer.put((byte)-34);
            this.buffer.putChar((char)length);
        } else {
            this.buffer.put((byte)-33);
            this.buffer.putInt(length);
        }

    }

    ByteBuffer getBuffer() {
        return this.buffer;
    }

    void writeBinaryHeader(int length) {
        if (length < 256) {
            this.buffer.put((byte)-60);
            this.buffer.put((byte)length);
        } else if (length < 65536) {
            this.buffer.put((byte)-59);
            this.buffer.putChar((char)length);
        } else {
            this.buffer.put((byte)-58);
            this.buffer.putInt(length);
        }

    }

    private static int stringLength(int length) {
        if (length < 16) {
            return 160;
        } else if (length < 256) {
            return -39;
        } else {
            return length < 65536 ? -38 : -37;
        }
    }

    private void fixStringHeaderInPlace(int mark, int lengthType, int actualLength) {
        switch(lengthType) {
            case -39:
                this.buffer.put(mark + 1, (byte)actualLength);
                break;
            case -38:
                this.buffer.putChar(mark + 1, (char)actualLength);
                break;
            case -37:
                this.buffer.putInt(mark + 1, actualLength);
                break;
            case 160:
                this.buffer.put(mark, (byte)(160 | actualLength));
        }

    }
}
