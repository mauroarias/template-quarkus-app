package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.api.env.CapturedEnvironment;
import datadog.lib.datadog.trace.core.ExclusiveSpan;

class ServletContextTagInterceptor extends AbstractTagInterceptor {
    public ServletContextTagInterceptor() {
        super("servlet.context");
    }

    public boolean shouldSetTag(ExclusiveSpan span, String tag, Object value) {
        String contextName = String.valueOf(value).trim();
        if (contextName.equals("/") || !span.getServiceName().equals("unnamed-java-app") && !span.getServiceName().equals(CapturedEnvironment.get().getProperties().get("service.name")) && !span.getServiceName().isEmpty()) {
            return true;
        } else {
            if (contextName.startsWith("/") && contextName.length() > 1) {
                contextName = contextName.substring(1);
            }

            if (!contextName.isEmpty()) {
                span.setServiceName(contextName);
            }

            return true;
        }
    }
}
