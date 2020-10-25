package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.core.DDSpan;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface PrioritizationStrategy {
    boolean publish(int var1, List<DDSpan> var2);

    boolean flush(long var1, TimeUnit var3);
}
