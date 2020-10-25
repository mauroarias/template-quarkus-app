package datadog.lib.datadog.trace.api;

public interface TwoArgFunction<T, U, V> {
    V apply(T var1, U var2);

    Function<T, V> curry(U var1);
}
