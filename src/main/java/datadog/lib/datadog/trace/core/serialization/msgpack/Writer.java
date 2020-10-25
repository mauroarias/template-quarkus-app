package datadog.lib.datadog.trace.core.serialization.msgpack;

public interface Writer<T> {
    void write(T var1, Packer var2, EncodingCache var3);
}
