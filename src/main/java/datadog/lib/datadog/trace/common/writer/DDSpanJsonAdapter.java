package datadog.lib.datadog.trace.common.writer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import datadog.lib.datadog.trace.core.DDSpan;
import datadog.trace.api.DDId;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class DDSpanJsonAdapter extends JsonAdapter<DDSpan> {
    private final boolean hexIds;

    DDSpanJsonAdapter(boolean hexIds) {
        this.hexIds = hexIds;
    }

    public static Factory buildFactory(final boolean hexIds) {
        return new Factory() {
            public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
                Class<?> rawType = Types.getRawType(type);
                return rawType.isAssignableFrom(DDSpan.class) ? new DDSpanJsonAdapter(hexIds) : null;
            }
        };
    }

    public DDSpan fromJson(JsonReader reader) {
        throw new UnsupportedOperationException();
    }

    public void toJson(JsonWriter writer, DDSpan span) throws IOException {
        writer.beginObject();
        writer.name("service");
        writer.value(span.getServiceName());
        writer.name("name");
        writer.value(span.getOperationName().toString());
        writer.name("resource");
        writer.value(span.getResourceName().toString());
        writer.name("trace_id");
        this.writeId(writer, span.getTraceId());
        writer.name("span_id");
        this.writeId(writer, span.getSpanId());
        writer.name("parent_id");
        this.writeId(writer, span.getParentId());
        writer.name("start");
        writer.value(span.getStartTime());
        writer.name("duration");
        writer.value(span.getDurationNano());
        writer.name("type");
        writer.value(span.getSpanType());
        writer.name("error");
        writer.value((long)span.getError());
        writer.name("metrics");
        writer.beginObject();
        Iterator var3 = span.getMetrics().entrySet().iterator();

        while(var3.hasNext()) {
            Entry<CharSequence, Number> entry = (Entry)var3.next();
            writer.name(((CharSequence)entry.getKey()).toString());
            writer.value((Number)entry.getValue());
        }

        writer.endObject();
        writer.name("meta");
        writer.beginObject();
        Map<String, Object> tags = span.getTags();
        Iterator var7 = span.context().getBaggageItems().entrySet().iterator();

        Entry entry;
        while(var7.hasNext()) {
            entry = (Entry)var7.next();
            if (!tags.containsKey(entry.getKey())) {
                writer.name((String)entry.getKey());
                writer.value((String)entry.getValue());
            }
        }

        var7 = tags.entrySet().iterator();

        while(var7.hasNext()) {
            entry = (Entry)var7.next();
            writer.name((String)entry.getKey());
            writer.value(String.valueOf(entry.getValue()));
        }

        writer.endObject();
        writer.endObject();
    }

    private void writeId(JsonWriter writer, DDId id) throws IOException {
        if (this.hexIds) {
            writer.value(id.toHexString());
        } else {
            writer.value(id.toLong());
        }

    }
}
