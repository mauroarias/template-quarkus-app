package datadog.lib.datadog.trace.core.serialization.msgpack;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;

import java.nio.ByteBuffer;
import java.util.Map;

public interface Writable {
    void writeNull();

    void writeBoolean(boolean var1);

    void writeObject(Object var1, EncodingCache var2);

    void writeMap(Map<? extends CharSequence, ?> var1, EncodingCache var2);

    void writeString(CharSequence var1, EncodingCache var2);

    void writeUTF8(byte[] var1, int var2, int var3);

    void writeUTF8(byte[] var1);

    void writeUTF8(UTF8BytesString var1);

    void writeBinary(byte[] var1, int var2, int var3);

    void startMap(int var1);

    void startArray(int var1);

    void writeBinary(ByteBuffer var1);

    void writeInt(int var1);

    void writeLong(long var1);

    void writeFloat(float var1);

    void writeDouble(double var1);
}
