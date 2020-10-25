package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.core.DDSpanData;
import datadog.lib.datadog.trace.core.serialization.msgpack.Mapper;
import java.util.List;

public interface TraceMapper extends Mapper<List<? extends DDSpanData>> {
    Payload newPayload();

    int messageBufferSize();

    void reset();

    String endpoint();
}
