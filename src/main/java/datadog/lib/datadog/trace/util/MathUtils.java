package datadog.lib.datadog.trace.util;

import java.util.concurrent.atomic.AtomicLong;

public class MathUtils {
    public MathUtils() {
    }

    public static boolean boundedDecrement(AtomicLong value, long minumum) {
        long previous;
        long next;
        do {
            previous = value.get();
            next = previous - 1L;
            if (next < minumum) {
                return false;
            }
        } while(!value.compareAndSet(previous, next));

        return true;
    }
}
