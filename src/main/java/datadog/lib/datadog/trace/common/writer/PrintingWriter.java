package datadog.lib.datadog.trace.common.writer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi.Builder;
import com.squareup.moshi.Types;
import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor;
import okio.BufferedSink;
import okio.Okio;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PrintingWriter implements Writer {
    private final TraceProcessor processor = new TraceProcessor();
    private final BufferedSink sink;
    private final JsonAdapter<Map<String, List<DDSpan>>> jsonAdapter;

    public PrintingWriter(OutputStream outputStream, boolean hexIds) {
        this.sink = Okio.buffer(Okio.sink(outputStream));
        this.jsonAdapter = (new Builder()).add(DDSpanJsonAdapter.buildFactory(hexIds)).build().adapter(Types.newParameterizedType(Map.class, new Type[]{String.class, Types.newParameterizedType(List.class, new Type[]{DDSpan.class})}));
    }

    public void write(List<DDSpan> trace) {
        List processedTrace = this.processor.onTraceComplete(trace);

        try {
            synchronized(this.sink) {
                this.jsonAdapter.toJson(this.sink, Collections.singletonMap("traces", processedTrace));
                this.sink.flush();
            }
        } catch (IOException var6) {
        }

    }

    public void start() {
    }

    public boolean flush() {
        return true;
    }

    public void close() {
    }

    public void incrementTraceCount() {
    }
}
