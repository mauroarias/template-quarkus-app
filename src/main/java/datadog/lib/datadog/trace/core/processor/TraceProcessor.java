package datadog.lib.datadog.trace.core.processor;

import datadog.lib.datadog.trace.api.Config;
import datadog.lib.datadog.trace.core.DDSpan;
import datadog.lib.datadog.trace.core.ExclusiveSpan;
import datadog.lib.datadog.trace.core.processor.rule.AnalyticsSampleRateRule;
import datadog.lib.datadog.trace.core.processor.rule.DBStatementRule;
import datadog.lib.datadog.trace.core.processor.rule.ErrorRule;
import datadog.lib.datadog.trace.core.processor.rule.HttpStatusErrorRule;
import datadog.lib.datadog.trace.core.processor.rule.MarkSpanForMetricCalculationRule;
import datadog.lib.datadog.trace.core.processor.rule.ResourceNameRule;
import datadog.lib.datadog.trace.core.processor.rule.URLAsResourceNameRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TraceProcessor {
    private static final Logger log = LoggerFactory.getLogger(TraceProcessor.class);
    final Rule[] DEFAULT_RULES = new Rule[]{new DBStatementRule(), new ResourceNameRule(), new ErrorRule(), new HttpStatusErrorRule(), new URLAsResourceNameRule(), new AnalyticsSampleRateRule(), new MarkSpanForMetricCalculationRule()};
    private final List<Rule> rules;

    public TraceProcessor() {
        this.rules = new ArrayList(this.DEFAULT_RULES.length);
        Rule[] var1 = this.DEFAULT_RULES;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Rule rule = var1[var3];
            if (isEnabled(rule)) {
                this.rules.add(rule);
            }
        }

    }

    private static boolean isEnabled(Rule rule) {
        boolean enabled = Config.get().isRuleEnabled(rule.getClass().getSimpleName());
        String[] var2 = rule.aliases();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String alias = var2[var4];
            enabled &= Config.get().isRuleEnabled(alias);
        }

        if (!enabled) {
            log.debug("{} disabled", rule.getClass().getSimpleName());
        }

        return enabled;
    }

    public List<DDSpan> onTraceComplete(List<DDSpan> trace) {
        Iterator var2 = trace.iterator();

        while(var2.hasNext()) {
            DDSpan span = (DDSpan)var2.next();
            this.applyRules(span);
        }

        return trace;
    }

    private void applyRules(DDSpan span) {
        if (this.rules.size() > 0) {
            span.context().processExclusiveSpan(new ExclusiveSpan.Consumer() {
                public void accept(ExclusiveSpan span) {
                    Iterator var2 = rules.iterator();

                    while(var2.hasNext()) {
                        Rule rule = (Rule)var2.next();
                        rule.processSpan(span);
                    }

                }
            });
        }

    }

    public interface Rule {
        String[] aliases();

        void processSpan(ExclusiveSpan var1);
    }
}
