package datadog.lib.datadog.trace.common.writer;

import datadog.lib.datadog.trace.core.DDSpan;
import java.io.Closeable;
import java.util.List;

public interface Writer extends Closeable {
    void write(List<DDSpan> var1);

    void start();

    boolean flush();

    void close();

    void incrementTraceCount();
}
