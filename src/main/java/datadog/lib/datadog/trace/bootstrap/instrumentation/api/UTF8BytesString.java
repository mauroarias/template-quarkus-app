package datadog.lib.datadog.trace.bootstrap.instrumentation.api;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class UTF8BytesString implements CharSequence {
    private static final Allocator ALLOCATOR = new Allocator();
    private final String string;
    private final byte[] utf8Bytes;
    private final int offset;
    private final int length;

    public static UTF8BytesString createConstant(CharSequence string) {
        return create(string, true);
    }

    public static UTF8BytesString create(CharSequence chars) {
        return create(chars, false);
    }

    private static UTF8BytesString create(CharSequence sequence, boolean constant) {
        if (null == sequence) {
            return null;
        } else {
            return sequence instanceof UTF8BytesString ? (UTF8BytesString)sequence : new UTF8BytesString(sequence, constant);
        }
    }

    private UTF8BytesString(CharSequence chars, boolean constant) {
        this(chars, constant, false);
    }

    private UTF8BytesString(CharSequence chars, boolean constant, boolean weak) {
        this.string = String.valueOf(chars);
        byte[] utf8Bytes = this.string.getBytes(StandardCharsets.UTF_8);
        this.length = utf8Bytes.length;
        if (constant) {
            Allocator.Allocation allocation = ALLOCATOR.allocate(utf8Bytes);
            if (null != allocation) {
                this.offset = allocation.position;
                this.utf8Bytes = allocation.page;
                return;
            }
        }

        this.offset = 0;
        this.utf8Bytes = utf8Bytes;
    }

    public void transferTo(ByteBuffer buffer) {
        buffer.put(this.utf8Bytes, this.offset, this.length);
    }

    public int encodedLength() {
        return this.length;
    }

    public String toString() {
        return this.string;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null) {
            return false;
        } else {
            String that = null;
            if (o instanceof UTF8BytesString) {
                that = ((UTF8BytesString)o).string;
            }

            return this.string.equals(that);
        }
    }

    public int hashCode() {
        return this.string.hashCode();
    }

    public int length() {
        return this.string.length();
    }

    public char charAt(int index) {
        return this.string.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return this.string.subSequence(start, end);
    }

    private static class Allocator {
        private static final int PAGE_SIZE = 8192;
        private final List<byte[]> pages = new ArrayList();
        private int currentPage = -1;
        int currentPosition = 0;

        Allocator() {
        }

        synchronized Allocator.Allocation allocate(byte[] utf8) {
            byte[] page = this.getPageWithCapacity(utf8.length);
            if (null == page) {
                return null;
            } else {
                System.arraycopy(utf8, 0, page, this.currentPosition, utf8.length);
                this.currentPosition += utf8.length;
                return new Allocator.Allocation(this.currentPosition - utf8.length, page);
            }
        }

        private byte[] getPageWithCapacity(int length) {
            if (length >= 8192) {
                return null;
            } else {
                if (this.currentPage < 0) {
                    this.newPage();
                } else if (this.currentPosition + length >= 8192) {
                    this.newPage();
                }

                return (byte[])this.pages.get(this.currentPage);
            }
        }

        private void newPage() {
            this.pages.add(new byte[8192]);
            ++this.currentPage;
            this.currentPosition = 0;
        }

        private static final class Allocation {
            final int position;
            final byte[] page;

            private Allocation(int position, byte[] page) {
                this.position = position;
                this.page = page;
            }
        }
    }
}
