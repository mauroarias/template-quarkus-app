package datadog.lib.datadog.opentracing.resolver;

import datadog.lib.datadog.opentracing.DDTracer;
import datadog.lib.datadog.trace.api.Config;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DDTracerResolver extends TracerResolver {
    private static final Logger log = LoggerFactory.getLogger(DDTracerResolver.class);

    public DDTracerResolver() {
    }

    Tracer resolve(Config config) {
        if (config.isTraceResolverEnabled()) {
            log.info("Creating DDTracer with DDTracerResolver");
            return DDTracer.builder().config(config).build();
        } else {
            log.info("DDTracerResolver disabled");
            return null;
        }
    }

    protected Tracer resolve() {
        return this.resolve(Config.get());
    }
}
