package datadog.lib.datadog.trace.bootstrap.config.provider;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class ConfigConverter {
    private static final Logger log = LoggerFactory.getLogger(ConfigConverter.class);
    private static final Pattern COMMA_SEPARATED = Pattern.compile("(([^,:]+:[^,:]*,)*([^,:]+:[^,:]*),?)?");
    private static final Pattern SPACE_SEPARATED = Pattern.compile("((\\S+:\\S*)\\s+)*(\\S+:\\S*)?");
    private static final Pattern ILLEGAL_SPACE_SEPARATED = Pattern.compile("(:\\S+:)+");

    ConfigConverter() {
    }

    @NonNull
    static List<String> parseList(String str) {
        if (str != null && !str.trim().isEmpty()) {
            String[] tokens = str.split(",", -1);

            for(int i = 0; i < tokens.length; ++i) {
                tokens[i] = tokens[i].trim();
            }

            return Collections.unmodifiableList(Arrays.asList(tokens));
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull
    static Map<String, String> parseMap(String str, String settingName) {
        if (str == null) {
            return Collections.emptyMap();
        } else {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return Collections.emptyMap();
            } else if (COMMA_SEPARATED.matcher(trimmed).matches()) {
                return parseMap(str, settingName, ",");
            } else if (SPACE_SEPARATED.matcher(trimmed).matches() && !ILLEGAL_SPACE_SEPARATED.matcher(trimmed).find()) {
                return parseMap(str, settingName, "\\s+");
            } else {
                log.warn("Invalid config for {}: '{}'. Must match 'key1:value1,key2:value2' or 'key1:value1 key2:value2'.", settingName, str);
                return Collections.emptyMap();
            }
        }
    }

    private static Map<String, String> parseMap(String str, String settingName, String separator) {
        String[] tokens = str.split(separator);
        Map<String, String> map = newHashMap(tokens.length);
        String[] var5 = tokens;
        int var6 = tokens.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String token = var5[var7];
            String[] keyValue = token.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (value.length() <= 0) {
                    log.warn("Ignoring empty value for key '{}' in config for {}", key, settingName);
                } else {
                    map.put(key, value);
                }
            }
        }

        return Collections.unmodifiableMap(map);
    }

    @NonNull
    private static Map<String, String> newHashMap(int size) {
        return new HashMap(size + 1, 1.0F);
    }

    @NonNull
    static BitSet parseIntegerRangeSet(@NonNull String str, String settingName) throws NumberFormatException {
        if (str == null) {
            throw new NullPointerException("str is marked non-null but is null");
        } else {
            str = str.replaceAll("\\s", "");
            if (!str.matches("\\d{3}(?:-\\d{3})?(?:,\\d{3}(?:-\\d{3})?)*")) {
                log.warn("Invalid config for {}: '{}'. Must be formatted like '400-403,405,410-499'.", settingName, str);
                throw new NumberFormatException();
            } else {
                int lastSeparator = Math.max(str.lastIndexOf(44), str.lastIndexOf(45));
                int maxValue = Integer.parseInt(str.substring(lastSeparator + 1));
                BitSet set = new BitSet(maxValue);
                String[] tokens = str.split(",", -1);
                String[] var6 = tokens;
                int var7 = tokens.length;

                for(int var8 = 0; var8 < var7; ++var8) {
                    String token = var6[var8];
                    int separator = token.indexOf(45);
                    if (separator == -1) {
                        set.set(Integer.parseInt(token));
                    } else if (separator > 0) {
                        int left = Integer.parseInt(token.substring(0, separator));
                        int right = Integer.parseInt(token.substring(separator + 1));
                        int min = Math.min(left, right);
                        int max = Math.max(left, right);
                        set.set(min, max + 1);
                    }
                }

                return set;
            }
        }
    }
}
