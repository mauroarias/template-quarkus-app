package datadog.lib.datadog.trace.core.serialization.msgpack;

public class Util {
    private static final byte[] DIGIT_TENS = new byte[]{48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 49, 49, 49, 49, 49, 49, 49, 49, 49, 49, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57};
    private static final byte[] DIGIT_ONES = new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57};

    public Util() {
    }

    public static byte[] integerToStringBuffer() {
        return new byte[20];
    }

    public static void writeLongAsString(long value, Writable destination, byte[] numberByteArray) {
        int pos = 20;
        long l = value;
        boolean negative = value < 0L;
        if (!negative) {
            l = -value;
        }

        int r;
        while(l <= -2147483648L) {
            long lq = l / 100L;
            r = (int)(lq * 100L - l);
            l = lq;
            --pos;
            numberByteArray[pos] = DIGIT_ONES[r];
            --pos;
            numberByteArray[pos] = DIGIT_TENS[r];
        }

        int iq;
        int i;
        for(i = (int)l; i <= -100; numberByteArray[pos] = DIGIT_TENS[r]) {
            iq = i / 100;
            r = iq * 100 - i;
            i = iq;
            --pos;
            numberByteArray[pos] = DIGIT_ONES[r];
            --pos;
        }

        iq = i / 10;
        r = iq * 10 - i;
        --pos;
        numberByteArray[pos] = (byte)(48 + r);
        if (iq < 0) {
            --pos;
            numberByteArray[pos] = (byte)(48 - iq);
        }

        if (negative) {
            --pos;
            numberByteArray[pos] = 45;
        }

        int len = 20 - pos;
        destination.writeUTF8(numberByteArray, pos, len);
    }
}
