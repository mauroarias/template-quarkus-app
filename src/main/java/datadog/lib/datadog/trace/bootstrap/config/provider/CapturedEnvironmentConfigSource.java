package datadog.lib.datadog.trace.bootstrap.config.provider;

import datadog.lib.datadog.trace.api.env.CapturedEnvironment;
import datadog.lib.datadog.trace.bootstrap.config.provider.ConfigProvider.Source;

final class CapturedEnvironmentConfigSource extends Source {
    private final CapturedEnvironment env;

    public CapturedEnvironmentConfigSource() {
        this(CapturedEnvironment.get());
    }

    public CapturedEnvironmentConfigSource(CapturedEnvironment env) {
        this.env = env;
    }

    protected String get(String key) {
        return (String)this.env.getProperties().get(key);
    }
}
