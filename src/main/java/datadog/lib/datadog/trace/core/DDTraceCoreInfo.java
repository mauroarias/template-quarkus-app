package datadog.lib.datadog.trace.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DDTraceCoreInfo {
    private static final Logger log = LoggerFactory.getLogger(DDTraceCoreInfo.class);
    public static final String JAVA_VERSION = System.getProperty("java.version", "unknown");
    public static final String JAVA_VM_NAME = System.getProperty("java.vm.name", "unknown");
    public static final String JAVA_VM_VENDOR = System.getProperty("java.vm.vendor", "unknown");
    public static final String VERSION;

    public DDTraceCoreInfo() {
    }

    public static void main(String... args) {
        System.out.println(VERSION);
    }

    static {
        String v;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(DDTraceCoreInfo.class.getResourceAsStream("/dd-trace-core.version"), "UTF-8"));
            Throwable var2 = null;

            try {
                StringBuilder sb = new StringBuilder();

                for(int c = br.read(); c != -1; c = br.read()) {
                    sb.append((char)c);
                }

                v = sb.toString().trim();
            } catch (Throwable var13) {
                var2 = var13;
                throw var13;
            } finally {
                if (br != null) {
                    if (var2 != null) {
                        try {
                            br.close();
                        } catch (Throwable var12) {
                            var2.addSuppressed(var12);
                        }
                    } else {
                        br.close();
                    }
                }

            }
        } catch (Exception var15) {
            v = "unknown";
        }

        VERSION = v;
    }
}
