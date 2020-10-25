package datadog.lib.datadog.trace.bootstrap.config.provider;

import datadog.lib.datadog.trace.bootstrap.config.provider.ConfigProvider.Source;
import lombok.NonNull;

import java.util.regex.Pattern;

final class EnvironmentConfigSource extends Source {
    private static final Pattern ENV_REPLACEMENT = Pattern.compile("[^a-zA-Z0-9_]");

    EnvironmentConfigSource() {
    }

    protected String get(String key) {
        return System.getenv(propertyNameToEnvironmentVariableName(key));
    }

    @NonNull
    private static String propertyNameToEnvironmentVariableName(String setting) {
        return ENV_REPLACEMENT.matcher(SystemPropertiesConfigSource.propertyNameToSystemPropertyName(setting).toUpperCase()).replaceAll("_");
    }
}
