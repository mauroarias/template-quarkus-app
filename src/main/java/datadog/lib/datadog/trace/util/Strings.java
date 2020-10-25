package datadog.lib.datadog.trace.util;

import java.util.Iterator;

public final class Strings {
    public Strings() {
    }

    public static String join(CharSequence joiner, Iterable<? extends CharSequence> strings) {
        StringBuilder sb = new StringBuilder();
        Iterator var3 = strings.iterator();

        while(var3.hasNext()) {
            CharSequence string = (CharSequence)var3.next();
            sb.append(string).append(joiner);
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - joiner.length());
        }

        return sb.toString();
    }

    public static String join(CharSequence joiner, CharSequence... strings) {
        if (strings.length <= 0) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(strings[0]);

            for(int i = 1; i < strings.length; ++i) {
                sb.append(joiner).append(strings[i]);
            }

            return sb.toString();
        }
    }
}
