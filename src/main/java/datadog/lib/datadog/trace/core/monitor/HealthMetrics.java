package datadog.lib.datadog.trace.core.monitor;

import com.timgroup.statsd.StatsDClient;
import datadog.lib.datadog.trace.api.IntFunction;
import datadog.lib.datadog.trace.api.cache.RadixTreeCache;
import datadog.lib.datadog.trace.common.writer.ddagent.DDAgentApi.Response;
import datadog.lib.datadog.trace.core.DDSpan;
import java.util.List;

public class HealthMetrics {
    private static final IntFunction<String[]> STATUS_TAGS = new IntFunction<String[]>() {
        public String[] apply(int httpStatus) {
            return new String[]{"status:" + httpStatus};
        }
    };
    private static final String[] NO_TAGS = new String[0];
    private final RadixTreeCache<String[]> statusTagsCache;
    private static final String[] USER_DROP_TAG = new String[]{"priority:user_drop"};
    private static final String[] USER_KEEP_TAG = new String[]{"priority:user_keep"};
    private static final String[] SAMPLER_DROP_TAG = new String[]{"priority:sampler_drop"};
    private static final String[] SAMPLER_KEEP_TAG = new String[]{"priority:sampler_keep"};
    private static final String[] UNSET_TAG = new String[]{"priority:unset"};
    private final StatsDClient statsd;

    private static String[] samplingPriorityTag(int samplingPriority) {
        switch(samplingPriority) {
            case -1:
                return USER_DROP_TAG;
            case 0:
                return SAMPLER_DROP_TAG;
            case 1:
                return SAMPLER_KEEP_TAG;
            case 2:
                return USER_KEEP_TAG;
            default:
                return UNSET_TAG;
        }
    }

    public HealthMetrics(StatsDClient statsd) {
        this.statusTagsCache = new RadixTreeCache(16, 32, STATUS_TAGS, new int[]{200, 400});
        this.statsd = statsd;
    }

    public void onStart(int queueCapacity) {
        this.statsd.recordGaugeValue("queue.max_length", (long)queueCapacity, NO_TAGS);
    }

    public void onShutdown(boolean flushSuccess) {
    }

    public void onPublish(List<DDSpan> trace, int samplingPriority) {
        this.statsd.incrementCounter("queue.enqueued.traces", samplingPriorityTag(samplingPriority));
        this.statsd.count("queue.enqueued.spans", (long)trace.size(), NO_TAGS);
    }

    public void onFailedPublish(int samplingPriority) {
        this.statsd.incrementCounter("queue.dropped.traces", samplingPriorityTag(samplingPriority));
    }

    public void onScheduleFlush(boolean previousIncomplete) {
    }

    public void onFlush(boolean early) {
    }

    public void onSerialize(int serializedSizeInBytes) {
        this.statsd.count("queue.enqueued.bytes", (long)serializedSizeInBytes, NO_TAGS);
    }

    public void onFailedSerialize(List<DDSpan> trace, Throwable optionalCause) {
    }

    public void onSend(int representativeCount, int sizeInBytes, Response response) {
        this.onSendAttempt(representativeCount, sizeInBytes, response);
    }

    public void onFailedSend(int representativeCount, int sizeInBytes, Response response) {
        this.onSendAttempt(representativeCount, sizeInBytes, response);
    }

    private void onSendAttempt(int representativeCount, int sizeInBytes, Response response) {
        this.statsd.incrementCounter("api.requests.total", NO_TAGS);
        this.statsd.count("flush.traces.total", (long)representativeCount, NO_TAGS);
        this.statsd.count("flush.bytes.total", (long)sizeInBytes, NO_TAGS);
        if (response.exception() != null) {
            this.statsd.incrementCounter("api.errors.total", NO_TAGS);
        }

        if (response.status() != null) {
            this.statsd.incrementCounter("api.responses.total", (String[])this.statusTagsCache.get(response.status()));
        }

    }
}
