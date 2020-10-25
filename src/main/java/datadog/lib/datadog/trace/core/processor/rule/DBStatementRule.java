package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;

public class DBStatementRule implements Rule {
    public DBStatementRule() {
    }

    public String[] aliases() {
        return new String[]{"DBStatementAsResourceName"};
    }

    public void processSpan(ExclusiveSpan span) {
        if (!"java-mongo".equals(span.getTag("component"))) {
            Object dbStatementValue = span.getAndRemoveTag("db.statement");
            if (dbStatementValue instanceof CharSequence) {
                CharSequence statement = (CharSequence)dbStatementValue;
                if (statement.length() != 0) {
                    span.setResourceName(statement);
                }
            }
        }

    }
}
