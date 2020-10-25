package datadog.lib.datadog.trace.api.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class CapturedEnvironment {
    private static final Logger log = LoggerFactory.getLogger(CapturedEnvironment.class);
    private static final CapturedEnvironment INSTANCE = new CapturedEnvironment();
    private final Map<String, String> properties = new HashMap();

    CapturedEnvironment() {
        this.properties.put("service.name", this.autodetectServiceName());
    }

    static void useFixedEnv(Map<String, String> props) {
        INSTANCE.properties.clear();
        Iterator var1 = props.entrySet().iterator();

        while(var1.hasNext()) {
            Entry<String, String> entry = (Entry)var1.next();
            INSTANCE.properties.put(entry.getKey(), entry.getValue());
        }

    }

    private String autodetectServiceName() {
        return this.extractJarOrClass(System.getProperty("sun.java.command"));
    }

    private String extractJarOrClass(String command) {
        if (command != null && !command.equals("")) {
            String[] split = command.trim().split(" ");
            if (split.length != 0 && !split[0].equals("")) {
                String candidate = split[0];
                return candidate.endsWith(".jar") ? (new File(candidate)).getName().replace(".jar", "") : candidate;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static CapturedEnvironment get() {
        return INSTANCE;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }
}
