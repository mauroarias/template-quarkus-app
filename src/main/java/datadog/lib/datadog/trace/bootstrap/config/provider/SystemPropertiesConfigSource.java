package datadog.lib.datadog.trace.bootstrap.config.provider;

import datadog.lib.datadog.trace.bootstrap.config.provider.ConfigProvider.Source;
import lombok.NonNull;

final class SystemPropertiesConfigSource extends Source {
    private static final String PREFIX = "dd.";

    SystemPropertiesConfigSource() {
    }

    protected String get(String key) {
        return System.getProperty(propertyNameToSystemPropertyName(key));
    }

    @NonNull
    static String propertyNameToSystemPropertyName(String setting) {
        return "dd." + setting;
    }
}
