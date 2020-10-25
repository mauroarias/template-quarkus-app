package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;

public class AnalyticsSampleRateRule implements Rule {
    public AnalyticsSampleRateRule() {
    }

    public String[] aliases() {
        return new String[]{"AnalyticsSampleRateDecorator"};
    }

    public void processSpan(ExclusiveSpan span) {
        Object sampleRateValue = span.getAndRemoveTag("_dd1.sr.eausr");
        if (sampleRateValue instanceof Number) {
            span.setMetric("_dd1.sr.eausr", (Number)sampleRateValue);
        } else if (sampleRateValue instanceof String) {
            try {
                span.setMetric("_dd1.sr.eausr", Double.parseDouble((String)sampleRateValue));
            } catch (NumberFormatException var4) {
            }
        }

    }
}
