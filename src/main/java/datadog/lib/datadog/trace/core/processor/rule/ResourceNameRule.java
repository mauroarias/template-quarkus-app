package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;

public class ResourceNameRule implements Rule {
    public ResourceNameRule() {
    }

    public String[] aliases() {
        return new String[]{"ResourceNameDecorator"};
    }

    public void processSpan(ExclusiveSpan span) {
        Object name = span.getAndRemoveTag("resource.name");
        if (name instanceof CharSequence) {
            span.setResourceName((CharSequence)name);
        } else if (name != null) {
            span.setResourceName(name.toString());
        }

    }
}
