package datadog.lib.datadog.trace.core.taginterceptor;

import datadog.lib.datadog.trace.api.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TagInterceptorsFactory {
    private static final Logger log = LoggerFactory.getLogger(TagInterceptorsFactory.class);

    public TagInterceptorsFactory() {
    }

    public static List<AbstractTagInterceptor> createTagInterceptors() {
        List<AbstractTagInterceptor> interceptors = new ArrayList();
        Iterator var1 = Arrays.asList(new ForceManualDropTagInterceptor(), new ForceManualKeepTagInterceptor(), new PeerServiceTagInterceptor(), new ServiceNameTagInterceptor(), new ServiceNameTagInterceptor("service", false), new ServletContextTagInterceptor()).iterator();

        while(var1.hasNext()) {
            AbstractTagInterceptor interceptor = (AbstractTagInterceptor)var1.next();
            if (Config.get().isRuleEnabled(interceptor.getClass().getSimpleName())) {
                interceptors.add(interceptor);
            } else {
                log.debug("{} disabled", interceptor.getClass().getSimpleName());
            }
        }

        var1 = Config.get().getSplitByTags().iterator();

        while(var1.hasNext()) {
            String splitByTag = (String)var1.next();
            interceptors.add(new ServiceNameTagInterceptor(splitByTag, true));
        }

        return interceptors;
    }
}
