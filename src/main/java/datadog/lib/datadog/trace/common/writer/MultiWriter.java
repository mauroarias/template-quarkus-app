package datadog.lib.datadog.trace.common.writer;

import com.timgroup.statsd.StatsDClient;
import datadog.lib.datadog.trace.api.Config;
import datadog.lib.datadog.trace.common.sampling.Sampler;
import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.monitor.Monitoring;
import java.util.List;

public class MultiWriter implements Writer {
    private final Writer[] writers;

    public MultiWriter(Config config, Sampler sampler, StatsDClient statsDClient, Monitoring monitoring, String type) {
        String mwConfig = type.replace("MultiWriter:", "");
        String[] writerConfigs = mwConfig.split(",");
        this.writers = new Writer[writerConfigs.length];
        int i = 0;
        String[] var9 = writerConfigs;
        int var10 = writerConfigs.length;

        for(int var11 = 0; var11 < var10; ++var11) {
            String writerConfig = var9[var11];
            this.writers[i] = WriterFactory.createWriter(config, sampler, statsDClient, monitoring, writerConfig);
            ++i;
        }

    }

    public MultiWriter(Writer[] writers) {
        this.writers = (Writer[])writers.clone();
    }

    public void start() {
        Writer[] var1 = this.writers;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Writer writer = var1[var3];
            if (writer != null) {
                writer.start();
            }
        }

    }

    public void write(List<DDSpan> trace) {
        Writer[] var2 = this.writers;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Writer writer = var2[var4];
            if (writer != null) {
                writer.write(trace);
            }
        }

    }

    public boolean flush() {
        boolean flush = true;
        Writer[] var2 = this.writers;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Writer writer = var2[var4];
            if (writer != null) {
                flush &= writer.flush();
            }
        }

        return flush;
    }

    public void close() {
        Writer[] var1 = this.writers;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Writer writer = var1[var3];
            if (writer != null) {
                writer.close();
            }
        }

    }

    public void incrementTraceCount() {
        Writer[] var1 = this.writers;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Writer writer = var1[var3];
            if (writer != null) {
                writer.incrementTraceCount();
            }
        }

    }
}
