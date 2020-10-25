package datadog.lib.datadog.trace.core.serialization.msgpack;

public interface Mapper<T> {
    void map(T var1, Writable var2);
}
