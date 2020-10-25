package datadog.lib.datadog.trace.bootstrap.config.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigProvider {
    private static final Logger log = LoggerFactory.getLogger(ConfigProvider.class);
    protected final ConfigProvider.Source[] sources;

    private ConfigProvider(ConfigProvider.Source... sources) {
        this.sources = sources;
    }

    public final String getString(String key) {
        return this.getString(key, (String)null);
    }

    public final <T extends Enum<T>> T getEnum(String key, Class<T> enumType, T defaultValue) {
        String value = this.getString(key);
        if (null != value) {
            try {
                return Enum.valueOf(enumType, value);
            } catch (Exception var6) {
                log.debug("failed to parse {} for {}, defaulting to {}", new Object[]{value, key, defaultValue});
            }
        }

        return defaultValue;
    }

    public final String getString(String key, String defaultValue, String... aliases) {
        ConfigProvider.Source[] var4 = this.sources;
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            ConfigProvider.Source source = var4[var6];
            String value = source.get(key, aliases);
            if (value != null) {
                return value;
            }
        }

        return defaultValue;
    }

    public final String getStringBypassSysProps(String key, String defaultValue) {
        ConfigProvider.Source[] var3 = this.sources;
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            ConfigProvider.Source source = var3[var5];
            if (!(source instanceof SystemPropertiesConfigSource)) {
                String value = source.get(key);
                if (value != null) {
                    return value;
                }
            }
        }

        return defaultValue;
    }

    public final Boolean getBoolean(String key) {
        final String value = this.get(key);
        return value == null ? null : new Boolean(value);
    }

    public final boolean getBoolean(String key, boolean defaultValue, String... aliases) {
        final String value = this.get(key);
        return value == null ? defaultValue : new Boolean(value);
    }

    public final Integer getInteger(String key) {
        final String value = this.get(key);
        return value == null ? null : new Integer(value);
    }

    public final int getInteger(String key, int defaultValue, String... aliases) {
        final String value = this.get(key);
        return value == null ? defaultValue : new Integer(value);
    }

    public final Float getFloat(String key, String... aliases) {
        final String value = this.get(key);
        return value == null ? null : new Float(value);
    }

    public final float getFloat(String key, float defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : new Float(value);
    }

    public final Double getDouble(String key) {
        final String value = this.get(key);
        return value == null ? null : new Double(value);
    }

    public final double getDouble(String key, double defaultValue) {
        final String value = this.get(key);
        return value == null ? defaultValue : new Double(value);
    }

    private String get(String key, String... aliases) {
        for (Source source : sources) {
            String value = source.get(key, aliases);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public final List<String> getList(String key) {
        return ConfigConverter.parseList(this.getString(key));
    }

    public final Map<String, String> getMergedMap(String key) {
        Map<String, String> merged = new HashMap();

        for(int i = this.sources.length - 1; 0 <= i; --i) {
            String value = this.sources[i].get(key);
            merged.putAll(ConfigConverter.parseMap(value, key));
        }

        return merged;
    }

    public BitSet getIntegerRange(String key, BitSet defaultValue) {
        String value = this.getString(key);

        try {
            return value == null ? defaultValue : ConfigConverter.parseIntegerRangeSet(value, key);
        } catch (NumberFormatException var5) {
            log.warn("Invalid configuration for " + key, var5);
            return defaultValue;
        }
    }

    public static ConfigProvider createDefault() {
        Properties configProperties = loadConfigurationFile(new ConfigProvider(new ConfigProvider.Source[]{new SystemPropertiesConfigSource(), new EnvironmentConfigSource()}));
        return configProperties.isEmpty() ? new ConfigProvider(new ConfigProvider.Source[]{new SystemPropertiesConfigSource(), new EnvironmentConfigSource(), new CapturedEnvironmentConfigSource()}) : new ConfigProvider(new ConfigProvider.Source[]{new SystemPropertiesConfigSource(), new EnvironmentConfigSource(), new PropertiesConfigSource(configProperties, true), new CapturedEnvironmentConfigSource()});
    }

    public static ConfigProvider withPropertiesOverride(Properties properties) {
        PropertiesConfigSource providedConfigSource = new PropertiesConfigSource(properties, false);
        Properties configProperties = loadConfigurationFile(new ConfigProvider(new ConfigProvider.Source[]{new SystemPropertiesConfigSource(), new EnvironmentConfigSource(), providedConfigSource}));
        return configProperties.isEmpty() ? new ConfigProvider(new ConfigProvider.Source[]{new SystemPropertiesConfigSource(), new EnvironmentConfigSource(), providedConfigSource, new CapturedEnvironmentConfigSource()}) : new ConfigProvider(new ConfigProvider.Source[]{providedConfigSource, new SystemPropertiesConfigSource(), new EnvironmentConfigSource(), new PropertiesConfigSource(configProperties, true), new CapturedEnvironmentConfigSource()});
    }

    private static Properties loadConfigurationFile(ConfigProvider configProvider) {
        Properties properties = new Properties();
        String configurationFilePath = configProvider.getString("trace.config");
        if (null == configurationFilePath) {
            return properties;
        } else {
            configurationFilePath = configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));
            File configurationFile = new File(configurationFilePath);
            if (!configurationFile.exists()) {
                log.error("Configuration file '{}' not found.", configurationFilePath);
                return properties;
            } else {
                try {
                    FileReader fileReader = new FileReader(configurationFile);
                    Throwable var5 = null;

                    try {
                        properties.load(fileReader);
                    } catch (Throwable var16) {
                        var5 = var16;
                        throw var16;
                    } finally {
                        if (fileReader != null) {
                            if (var5 != null) {
                                try {
                                    fileReader.close();
                                } catch (Throwable var15) {
                                    var5.addSuppressed(var15);
                                }
                            } else {
                                fileReader.close();
                            }
                        }

                    }
                } catch (FileNotFoundException var18) {
                    log.error("Configuration file '{}' not found.", configurationFilePath);
                } catch (IOException var19) {
                    log.error("Configuration file '{}' cannot be accessed or correctly parsed.", configurationFilePath);
                }

                return properties;
            }
        }
    }

    public abstract static class Source {
        public Source() {
        }

        public final String get(String key, String... aliases) {
            String value = this.get(key);
            if (value != null) {
                return value;
            } else {
                String[] var4 = aliases;
                int var5 = aliases.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    String alias = var4[var6];
                    value = this.get(alias);
                    if (value != null) {
                        return value;
                    }
                }

                return null;
            }
        }

        protected abstract String get(String var1);
    }
}
