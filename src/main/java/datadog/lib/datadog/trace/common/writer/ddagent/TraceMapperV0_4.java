package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.lib.datadog.trace.core.DDSpanData;
import datadog.lib.datadog.trace.core.StringTables;
import datadog.lib.datadog.trace.core.TagsAndBaggageConsumer;
import datadog.lib.datadog.trace.core.serialization.msgpack.EncodingCachingStrategies;
import datadog.lib.datadog.trace.core.serialization.msgpack.Util;
import datadog.lib.datadog.trace.core.serialization.msgpack.Writable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class TraceMapperV0_4 implements TraceMapper {
    static final byte[] EMPTY = ByteBuffer.allocate(1).put((byte)-112).array();
    private final MetaWriter metaWriter = new MetaWriter();

    public TraceMapperV0_4() {
    }

    public void map(List<? extends DDSpanData> trace, Writable writable) {
        writable.startArray(trace.size());
        Iterator var3 = trace.iterator();

        while(var3.hasNext()) {
            DDSpanData span = (DDSpanData)var3.next();
            writable.startMap(12);
            writable.writeUTF8(StringTables.SERVICE);
            writable.writeString(span.getServiceName(), EncodingCachingStrategies.NO_CACHING);
            writable.writeUTF8(StringTables.NAME);
            writable.writeObject(span.getOperationName(), EncodingCachingStrategies.NO_CACHING);
            writable.writeUTF8(StringTables.RESOURCE);
            writable.writeObject(span.getResourceName(), EncodingCachingStrategies.NO_CACHING);
            writable.writeUTF8(StringTables.TRACE_ID);
            writable.writeLong(span.getTraceId().toLong());
            writable.writeUTF8(StringTables.SPAN_ID);
            writable.writeLong(span.getSpanId().toLong());
            writable.writeUTF8(StringTables.PARENT_ID);
            writable.writeLong(span.getParentId().toLong());
            writable.writeUTF8(StringTables.START);
            writable.writeLong(span.getStartTime());
            writable.writeUTF8(StringTables.DURATION);
            writable.writeLong(span.getDurationNano());
            writable.writeUTF8(StringTables.TYPE);
            writable.writeString(span.getType(), EncodingCachingStrategies.NO_CACHING);
            writable.writeUTF8(StringTables.ERROR);
            writable.writeInt(span.getError());
            writable.writeUTF8(StringTables.METRICS);
            writable.writeMap(span.getMetrics(), EncodingCachingStrategies.CONSTANT_KEYS);
            writable.writeUTF8(StringTables.META);
            span.processTagsAndBaggage(this.metaWriter.withWritable(writable));
        }

    }

    public Payload newPayload() {
        return new PayloadV0_4();
    }

    public int messageBufferSize() {
        return 5242880;
    }

    public void reset() {
    }

    public String endpoint() {
        return "v0.4";
    }

    private static class PayloadV0_4 extends Payload {
        private PayloadV0_4() {
        }

        int sizeInBytes() {
            return sizeInBytes(this.body);
        }

        public void writeTo(WritableByteChannel channel) throws IOException {
            writeBufferToChannel(this.body, channel);
        }
    }

    private static final class MetaWriter extends TagsAndBaggageConsumer {
        private final byte[] numberByteArray;
        private Writable writable;

        private MetaWriter() {
            this.numberByteArray = Util.integerToStringBuffer();
        }

        MetaWriter withWritable(Writable writable) {
            this.writable = writable;
            return this;
        }

        public void accept(Map<String, Object> tags, Map<String, String> baggage) {
            int size = tags.size();
            long overlaps = 0L;
            int i;
            Iterator var7;
            Map.Entry entry;
            if (!baggage.isEmpty()) {
                i = 0;

                for(var7 = baggage.entrySet().iterator(); var7.hasNext(); ++i) {
                    entry = (Map.Entry)var7.next();
                    if (!tags.containsKey(entry.getKey())) {
                        ++size;
                    } else {
                        overlaps |= 1L << i;
                    }
                }
            }

            this.writable.startMap(size);
            i = 0;

            for(var7 = baggage.entrySet().iterator(); var7.hasNext(); ++i) {
                entry = (Map.Entry)var7.next();
                if ((overlaps & 1L << i) == 0L) {
                    this.writable.writeString((CharSequence)entry.getKey(), EncodingCachingStrategies.CONSTANT_KEYS);
                    this.writable.writeString((CharSequence)entry.getValue(), EncodingCachingStrategies.NO_CACHING);
                }
            }

            var7 = tags.entrySet().iterator();

            while(true) {
                while(var7.hasNext()) {
                    entry = (Map.Entry)var7.next();
                    this.writable.writeString((CharSequence)entry.getKey(), EncodingCachingStrategies.CONSTANT_KEYS);
                    if (!(entry.getValue() instanceof Long) && !(entry.getValue() instanceof Integer)) {
                        if (entry.getValue() instanceof UTF8BytesString) {
                            this.writable.writeUTF8((UTF8BytesString)entry.getValue());
                        } else {
                            this.writable.writeString(String.valueOf(entry.getValue()), EncodingCachingStrategies.NO_CACHING);
                        }
                    } else {
                        Util.writeLongAsString(((Number)entry.getValue()).longValue(), this.writable, this.numberByteArray);
                    }
                }

                return;
            }
        }
    }
}
