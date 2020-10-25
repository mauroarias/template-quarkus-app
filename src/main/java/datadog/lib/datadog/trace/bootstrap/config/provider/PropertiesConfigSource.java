package datadog.lib.datadog.trace.bootstrap.config.provider;

import datadog.lib.datadog.trace.bootstrap.config.provider.ConfigProvider.Source;

import java.util.Properties;

final class PropertiesConfigSource extends Source {
    private final Properties props;
    private final boolean useSystemPropertyFormat;

    public PropertiesConfigSource(Properties props, boolean useSystemPropertyFormat) {
        assert props != null;

        this.props = props;
        this.useSystemPropertyFormat = useSystemPropertyFormat;
    }

    protected String get(String key) {
        return this.props.getProperty(this.useSystemPropertyFormat ? SystemPropertiesConfigSource.propertyNameToSystemPropertyName(key) : key);
    }
}
