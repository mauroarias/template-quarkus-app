package datadog.lib.datadog.trace.core.processor.rule;

import datadog.lib.datadog.trace.core.processor.TraceProcessor.Rule;
import datadog.lib.datadog.trace.core.ExclusiveSpan;

public class URLAsResourceNameRule implements Rule {
    private static final BitSlicedBitapSearch PROTOCOL_SEARCH = new BitSlicedBitapSearch("://");
    private final ThreadLocal<StringBuilder> resourceNameBuilder = new ThreadLocal<StringBuilder>() {
        protected StringBuilder initialValue() {
            return new StringBuilder(100);
        }
    };

    public URLAsResourceNameRule() {
    }

    public String[] aliases() {
        return new String[]{"URLAsResourceName", "Status404Rule", "Status404Decorator"};
    }

    public void processSpan(ExclusiveSpan span) {
        if (!span.isResourceNameSet()) {
            Object httpStatus = span.getTag("http.status_code");
            if (null == httpStatus || !httpStatus.equals(404) && !"404".equals(httpStatus)) {
                Object url = span.getTag("http.url");
                if (null != url) {
                    span.setResourceName(this.extractResourceNameFromURL(span.getTag("http.method"), url.toString()));
                }
            } else {
                span.setResourceName("404");
            }
        }
    }

    private String extractResourceNameFromURL(Object method, String url) {
        StringBuilder resourceName = (StringBuilder)this.resourceNameBuilder.get();

        String verb;
        try {
            if (method != null) {
                verb = method.toString().toUpperCase().trim();
                resourceName.append(verb).append(' ');
            }

            if (url.isEmpty()) {
                resourceName.append('/');
            } else {
                int start = protocolPosition(url);
                boolean hasProtocol = start >= 0;
                start += hasProtocol ? 3 : 1;
                if (hasProtocol) {
                    start = url.indexOf(47, start);
                    if (start == -1) {
                        resourceName.append('/');
                    } else {
                        this.cleanResourceName(url, resourceName, start);
                    }
                } else {
                    this.cleanResourceName(url, resourceName, start);
                }
            }

            verb = resourceName.toString();
        } finally {
            resourceName.setLength(0);
        }

        return verb;
    }

    private void cleanResourceName(String url, StringBuilder resourceName, int start) {
        boolean lastSegment = false;

        int segmentEnd;
        for(int i = start; i < url.length() && !lastSegment; i = segmentEnd) {
            if (url.charAt(i) == '/') {
                resourceName.append('/');
                ++i;
            }

            segmentEnd = url.indexOf(47, i);
            if (segmentEnd == -1) {
                segmentEnd = url.indexOf(63, i);
                if (segmentEnd == -1) {
                    segmentEnd = url.indexOf(35, i);
                    if (segmentEnd == -1) {
                        segmentEnd = url.length();
                    }
                }

                lastSegment = true;
            }

            if (i < segmentEnd) {
                int snapshot = resourceName.length();
                char c = url.charAt(i);
                resourceName.append(c);
                boolean isVersion = !lastSegment & (c == 'v' | c == 'V') & segmentEnd - i <= 3;
                boolean containsNumerics = Character.isDigit(c);
                boolean isBlank = Character.isWhitespace(c);

                for(int j = i + 1; j < segmentEnd && (!containsNumerics || isVersion || isBlank); ++j) {
                    c = url.charAt(j);
                    isVersion &= Character.isDigit(c);
                    containsNumerics |= Character.isDigit(c);
                    isBlank &= Character.isWhitespace(c);
                    resourceName.append(c);
                }

                if (containsNumerics && !isVersion) {
                    resourceName.setLength(snapshot);
                    resourceName.append('?');
                } else if (isBlank) {
                    resourceName.setLength(snapshot);
                }
            }
        }

        if (resourceName.length() == 0) {
            resourceName.append('/');
        }

    }

    private static int protocolPosition(String url) {
        return PROTOCOL_SEARCH.indexOf(url, 0, 16);
    }

    private static class BitSlicedBitapSearch {
        private final int[] high;
        private final int[] low;
        private final int termination;

        BitSlicedBitapSearch(String term) {
            if (term.length() > 32) {
                throw new IllegalArgumentException("term must be shorter than 32 characters");
            } else {
                this.high = new int[16];
                this.low = new int[16];
                int mask = 1;
                char[] var3 = term.toCharArray();
                int var4 = var3.length;

                for(int var5 = 0; var5 < var4; ++var5) {
                    char c = var3[var5];
                    if (c >= 256) {
                        throw new IllegalStateException("term must be latin 1");
                    }

                    int[] var10000 = this.low;
                    var10000[c & 15] |= mask;
                    var10000 = this.high;
                    var10000[c >>> 4 & 15] |= mask;
                    mask <<= 1;
                }

                this.termination = 1 << term.length() - 1;
            }
        }

        public int indexOf(String text, int from, int to) {
            int state = 0;
            to = Math.min(to, text.length());

            for(int i = from; i < to; ++i) {
                char c = text.charAt(i);
                if (c >= 256) {
                    state = 0;
                } else {
                    int highMask = this.high[c >>> 4 & 15];
                    int lowMask = this.low[c & 15];
                    state = (state << 1 | 1) & highMask & lowMask;
                    if ((state & this.termination) == this.termination) {
                        return i - Long.numberOfTrailingZeros((long)this.termination);
                    }
                }
            }

            return -1;
        }
    }
}
