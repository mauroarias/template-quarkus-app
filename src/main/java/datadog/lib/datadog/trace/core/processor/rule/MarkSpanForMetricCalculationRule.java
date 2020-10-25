package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;

public class MarkSpanForMetricCalculationRule implements Rule {
    public MarkSpanForMetricCalculationRule() {
    }

    public String[] aliases() {
        return new String[0];
    }

    public void processSpan(ExclusiveSpan span) {
        Object val = span.getAndRemoveTag("_dd.measured");
        if (val instanceof Boolean && (Boolean)val) {
            span.setMetric("_dd.measured", 1);
        }

    }
}
