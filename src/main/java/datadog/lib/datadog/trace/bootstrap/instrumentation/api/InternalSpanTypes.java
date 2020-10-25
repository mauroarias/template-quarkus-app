package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

public class InternalSpanTypes {
    public static final CharSequence HTTP_CLIENT = UTF8BytesString.createConstant("http");
    public static final CharSequence HTTP_SERVER = UTF8BytesString.createConstant("web");
    public static final CharSequence RPC = UTF8BytesString.createConstant("rpc");
    public static final CharSequence SQL = UTF8BytesString.createConstant("sql");
    public static final CharSequence MONGO = UTF8BytesString.createConstant("mongodb");
    public static final CharSequence CASSANDRA = UTF8BytesString.createConstant("cassandra");
    public static final CharSequence COUCHBASE = UTF8BytesString.createConstant("db");
    public static final CharSequence REDIS = UTF8BytesString.createConstant("redis");
    public static final CharSequence MEMCACHED = UTF8BytesString.createConstant("memcached");
    public static final CharSequence ELASTICSEARCH = UTF8BytesString.createConstant("elasticsearch");
    public static final CharSequence HIBERNATE = UTF8BytesString.createConstant("hibernate");
    public static final CharSequence MESSAGE_CONSUMER = UTF8BytesString.createConstant("queue");
    public static final CharSequence MESSAGE_PRODUCER = UTF8BytesString.createConstant("queue");

    public InternalSpanTypes() {
    }
}
