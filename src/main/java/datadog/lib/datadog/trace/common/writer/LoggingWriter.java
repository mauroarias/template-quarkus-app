package datadog.lib.datadog.trace.common.writer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi.Builder;
import com.squareup.moshi.Types;
import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class LoggingWriter implements Writer {
    private static final Logger log = LoggerFactory.getLogger(LoggingWriter.class);
    private static final JsonAdapter<List<DDSpan>> TRACE_ADAPTER = (new Builder()).add(DDSpanJsonAdapter.buildFactory(false)).build().adapter(Types.newParameterizedType(List.class, new Type[]{DDSpan.class}));
    private final TraceProcessor processor = new TraceProcessor();

    public LoggingWriter() {
    }

    public void write(List<DDSpan> trace) {
        List processedTrace = this.processor.onTraceComplete(trace);

        try {
            log.info("write(trace): {}", TRACE_ADAPTER.toJson(processedTrace));
        } catch (Exception var4) {
            log.error("error writing(trace): {}", processedTrace, var4);
        }

    }

    public void incrementTraceCount() {
        log.info("incrementTraceCount()");
    }

    public void start() {
        log.info("start()");
    }

    public boolean flush() {
        log.info("flush()");
        return true;
    }

    public void close() {
        log.info("close()");
    }

    public String toString() {
        return "LoggingWriter { }";
    }
}
