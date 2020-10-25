package datadog.lib.datadog.trace.core.serialization.msgpack;

public interface MessageFormatter {
    <T> boolean format(T var1, Mapper<T> var2);

    void flush();
}
