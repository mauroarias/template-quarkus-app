package datadog.lib.datadog.trace.common.sampling;

import datadog.lib.datadog.trace.core.DDSpan;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/** @deprecated */
@Deprecated
public abstract class AbstractSampler implements Sampler {
    protected Map<String, Pattern> skipTagsPatterns = new HashMap();

    public AbstractSampler() {
    }

    public boolean sample(DDSpan span) {
        Iterator var2 = this.skipTagsPatterns.entrySet().iterator();

        while(var2.hasNext()) {
            Entry<String, Pattern> entry = (Entry)var2.next();
            Object value = span.getTags().get(entry.getKey());
            if (value != null) {
                String strValue = String.valueOf(value);
                Pattern skipPattern = (Pattern)entry.getValue();
                if (skipPattern.matcher(strValue).matches()) {
                    return false;
                }
            }
        }

        return this.doSample(span);
    }

    /** @deprecated */
    @Deprecated
    public void addSkipTagPattern(String tag, Pattern skipPattern) {
        this.skipTagsPatterns.put(tag, skipPattern);
    }

    protected abstract boolean doSample(DDSpan var1);
}
