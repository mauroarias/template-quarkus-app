package org.mauro.config;

import io.opentracing.util.GlobalTracer;
import io.quarkus.runtime.Startup;
import org.jboss.logging.Logger;
import quarkus.datadog.opentracing.QuarkusDDTracer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@Startup
@ApplicationScoped
public class DatadogConfig {

    private static final Logger log = Logger.getLogger(DatadogConfig.class);

    @PostConstruct
    void startup() {
        log.info("post constructor");
        configDD();
    }

    public void configDD() {
        QuarkusDDTracer tracer = QuarkusDDTracer.build();
        GlobalTracer.register(tracer);
        datadog.trace.api.GlobalTracer.registerIfAbsent(tracer);
    }
}
