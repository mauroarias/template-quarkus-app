package datadog.lib.datadog.trace.core.serialization.msgpack;

import java.nio.ByteBuffer;

public interface ByteBufferConsumer {
    void accept(int var1, ByteBuffer var2);
}
