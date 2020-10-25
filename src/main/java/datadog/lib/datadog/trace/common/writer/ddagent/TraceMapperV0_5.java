package datadog.lib.datadog.trace.common.writer.ddagent;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.lib.datadog.trace.core.DDSpanData;
import datadog.lib.datadog.trace.core.StringTables;
import datadog.lib.datadog.trace.core.TagsAndBaggageConsumer;
import datadog.lib.datadog.trace.core.serialization.msgpack.ByteBufferConsumer;
import datadog.lib.datadog.trace.core.serialization.msgpack.EncodingCachingStrategies;
import datadog.lib.datadog.trace.core.serialization.msgpack.Mapper;
import datadog.lib.datadog.trace.core.serialization.msgpack.Packer;
import datadog.lib.datadog.trace.core.serialization.msgpack.Util;
import datadog.lib.datadog.trace.core.serialization.msgpack.Writable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class TraceMapperV0_5 implements TraceMapper {
    static final byte[] EMPTY = ByteBuffer.allocate(3).put((byte)-110).put((byte)-112).put((byte)-112).array();
    private static final DictionaryFull DICTIONARY_FULL = new DictionaryFull();
    private final ByteBuffer[] dictionary;
    private final Packer dictionaryWriter;
    private final DictionaryMapper dictionaryMapper;
    private final Map<Object, Integer> encoding;
    private final MetaWriter metaWriter;

    public TraceMapperV0_5() {
        this(2097152);
    }

    public TraceMapperV0_5(int bufferSize) {
        this.dictionary = new ByteBuffer[1];
        this.dictionaryMapper = new DictionaryMapper();
        this.encoding = new HashMap();
        this.metaWriter = new MetaWriter();
        this.dictionaryWriter = new Packer(new ByteBufferConsumer() {
            public void accept(int messageCount, ByteBuffer buffer) {
                dictionary[0] = buffer;
            }
        }, ByteBuffer.allocate(bufferSize), true);
        this.reset();
    }

    public void map(List<? extends DDSpanData> trace, Writable writable) {
        writable.startArray(trace.size());
        Iterator var3 = trace.iterator();

        while(var3.hasNext()) {
            DDSpanData span = (DDSpanData)var3.next();
            writable.startArray(12);
            this.writeDictionaryEncoded(writable, span.getServiceName());
            this.writeDictionaryEncoded(writable, span.getOperationName());
            this.writeDictionaryEncoded(writable, span.getResourceName());
            writable.writeLong(span.getTraceId().toLong());
            writable.writeLong(span.getSpanId().toLong());
            writable.writeLong(span.getParentId().toLong());
            writable.writeLong(span.getStartTime());
            writable.writeLong(span.getDurationNano());
            writable.writeInt(span.getError());
            span.processTagsAndBaggage(this.metaWriter.withWritable(writable));
            writable.startMap(span.getMetrics().size());
            Iterator var5 = span.getMetrics().entrySet().iterator();

            while(var5.hasNext()) {
                Entry<CharSequence, Number> entry = (Entry)var5.next();
                this.writeDictionaryEncoded(writable, entry.getKey());
                writable.writeObject(entry.getValue(), EncodingCachingStrategies.NO_CACHING);
            }

            this.writeDictionaryEncoded(writable, span.getType());
        }

    }

    private void writeDictionaryEncoded(Writable writable, Object value) {
        Object target = null == value ? "" : value;
        Integer encoded = (Integer)this.encoding.get(target);
        if (null == encoded) {
            if (!this.dictionaryWriter.format(target, this.dictionaryMapper)) {
                this.dictionaryWriter.flush();
                throw DICTIONARY_FULL;
            }

            int dictionaryCode = this.dictionaryWriter.messageCount() - 1;
            this.encoding.put(target, dictionaryCode);
            writable.writeInt(dictionaryCode);
        } else {
            writable.writeInt(encoded);
        }

    }

    public Payload newPayload() {
        return new PayloadV0_5(this.getDictionary());
    }

    public int messageBufferSize() {
        return 2097152;
    }

    private ByteBuffer getDictionary() {
        if (this.dictionary[0] == null) {
            this.dictionaryWriter.flush();
        }

        return this.dictionary[0];
    }

    public void reset() {
        this.dictionaryWriter.reset();
        this.dictionary[0] = null;
        this.encoding.clear();
    }

    public String endpoint() {
        return "v0.5";
    }

    private final class MetaWriter extends TagsAndBaggageConsumer {
        private Writable writable;

        private MetaWriter() {
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
            Entry entry;
            if (!baggage.isEmpty()) {
                i = 0;

                for(var7 = baggage.entrySet().iterator(); var7.hasNext(); ++i) {
                    entry = (Entry)var7.next();
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
                entry = (Entry)var7.next();
                if ((overlaps & 1L << i) == 0L) {
                    writeDictionaryEncoded(this.writable, entry.getKey());
                    writeDictionaryEncoded(this.writable, entry.getValue());
                }
            }

            var7 = tags.entrySet().iterator();

            while(var7.hasNext()) {
                entry = (Entry)var7.next();
                writeDictionaryEncoded(this.writable, entry.getKey());
                writeDictionaryEncoded(this.writable, entry.getValue());
            }

        }
    }

    private static final class DictionaryFull extends BufferOverflowException {
        private DictionaryFull() {
        }

        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static class PayloadV0_5 extends Payload {
        private final ByteBuffer header;
        private final ByteBuffer dictionary;

        private PayloadV0_5(ByteBuffer dictionary) {
            this.header = ByteBuffer.allocate(1).put(0, (byte)-110);
            this.dictionary = dictionary;
        }

        int sizeInBytes() {
            return sizeInBytes(this.header) + sizeInBytes(this.dictionary) + sizeInBytes(this.body);
        }

        public void writeTo(WritableByteChannel channel) throws IOException {
            writeBufferToChannel(this.header, channel);
            writeBufferToChannel(this.dictionary, channel);
            writeBufferToChannel(this.body, channel);
        }
    }

    private static class DictionaryMapper implements Mapper<Object> {
        private final byte[] numberByteArray;

        private DictionaryMapper() {
            this.numberByteArray = Util.integerToStringBuffer();
        }

        public void map(Object data, Writable packer) {
            if (data instanceof UTF8BytesString) {
                packer.writeObject(data, EncodingCachingStrategies.NO_CACHING);
            } else if (!(data instanceof Long) && !(data instanceof Integer)) {
                assert null != data : "enclosing mapper should not provide null values";

                String string = String.valueOf(data);
                byte[] utf8 = StringTables.getKeyBytesUTF8(string);
                if (null == utf8) {
                    packer.writeString(string, EncodingCachingStrategies.NO_CACHING);
                    return;
                }

                packer.writeUTF8(utf8);
            } else {
                Util.writeLongAsString(((Number)data).longValue(), packer, this.numberByteArray);
            }

        }
    }
}
