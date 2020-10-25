package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.api.Config;
import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;
import java.util.BitSet;

public class HttpStatusErrorRule implements Rule {
    private final BitSet serverErrorStatuses = Config.get().getHttpServerErrorStatuses();
    private final BitSet clientErrorStatuses = Config.get().getHttpClientErrorStatuses();

    public HttpStatusErrorRule() {
    }

    public String[] aliases() {
        return new String[0];
    }

    public void processSpan(ExclusiveSpan span) {
        if (!span.isError()) {
            CharSequence spanType = span.getType();
            if (null != spanType) {
                String var3 = spanType.toString();
                byte var4 = -1;
                switch(var3.hashCode()) {
                    case 117588:
                        if (var3.equals("web")) {
                            var4 = 0;
                        }
                        break;
                    case 3213448:
                        if (var3.equals("http")) {
                            var4 = 1;
                        }
                }

                switch(var4) {
                    case 0:
                        span.setError(this.serverErrorStatuses.get(span.getHttpStatus()));
                        break;
                    case 1:
                        span.setError(this.clientErrorStatuses.get(span.getHttpStatus()));
                }
            }
        }

    }
}
