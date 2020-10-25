package datadog.lib.datadog.trace.api;

import datadog.lib.datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;

public final class Functions {
    private static final Zero ZERO = new Zero();
    public static final Function<String, UTF8BytesString> UTF8_ENCODE = new Function<String, UTF8BytesString>() {
        public UTF8BytesString apply(String input) {
            return UTF8BytesString.create(input);
        }
    };

    private Functions() {
    }

    public static <T> Zero<T> zero() {
        return ZERO;
    }

    public static final class ToString<T> implements Function<T, String> {
        public ToString() {
        }

        public String apply(T key) {
            return key.toString();
        }
    }

    public static final class LowerCase implements Function<String, String> {
        public static final LowerCase INSTANCE = new LowerCase();

        public LowerCase() {
        }

        public String apply(String key) {
            return key.toLowerCase();
        }
    }

    public static class SuffixJoin extends Join {
        public SuffixJoin(CharSequence joiner, Function<CharSequence, CharSequence> transformer) {
            super(joiner, transformer);
        }

        public Function<CharSequence, CharSequence> curry(CharSequence specialisation) {
            return new Suffix(String.valueOf(this.joiner) + specialisation, this.transformer);
        }

        public static SuffixJoin of(CharSequence joiner, Function<CharSequence, CharSequence> transformer) {
            return new SuffixJoin(joiner, transformer);
        }

        public static SuffixJoin of(CharSequence joiner) {
            return of(joiner, zero());
        }
    }

    public static class PrefixJoin extends Join {
        public PrefixJoin(CharSequence joiner, Function<CharSequence, CharSequence> transformer) {
            super(joiner, transformer);
        }

        public Function<CharSequence, CharSequence> curry(CharSequence specialisation) {
            return new Prefix(String.valueOf(specialisation) + this.joiner, this.transformer);
        }

        public static PrefixJoin of(CharSequence joiner, Function<CharSequence, CharSequence> transformer) {
            return new PrefixJoin(joiner, transformer);
        }

        public static PrefixJoin of(String joiner) {
            return of(joiner, zero());
        }
    }

    public abstract static class Join implements TwoArgFunction<CharSequence, CharSequence, CharSequence> {
        protected final CharSequence joiner;
        protected final Function<CharSequence, CharSequence> transformer;

        protected Join(CharSequence joiner, Function<CharSequence, CharSequence> transformer) {
            this.joiner = joiner;
            this.transformer = transformer;
        }

        public CharSequence apply(CharSequence left, CharSequence right) {
            return UTF8BytesString.create(String.valueOf(left) + this.joiner + right);
        }
    }

    public static final class Prefix extends Concatenate implements Function<CharSequence, CharSequence> {
        private final CharSequence prefix;
        private final Function<CharSequence, CharSequence> transformer;
        public static final Prefix ZERO = new Prefix("", zero());

        public Prefix(CharSequence prefix, Function<CharSequence, CharSequence> transformer) {
            this.prefix = prefix;
            this.transformer = transformer;
        }

        public Prefix(CharSequence prefix) {
            this(prefix, zero());
        }

        public CharSequence apply(CharSequence key) {
            return this.apply(this.prefix, (CharSequence)this.transformer.apply(key));
        }

        public Function<CharSequence, CharSequence> curry(CharSequence prefix) {
            return new Prefix(prefix, this.transformer);
        }
    }

    public static final class Suffix extends Concatenate implements Function<CharSequence, CharSequence> {
        private final CharSequence suffix;
        private final Function<CharSequence, CharSequence> transformer;
        public static final Suffix ZERO = new Suffix("", zero());

        public Suffix(CharSequence suffix, Function<CharSequence, CharSequence> transformer) {
            this.suffix = suffix;
            this.transformer = transformer;
        }

        public Suffix(String suffix) {
            this(suffix, zero());
        }

        public CharSequence apply(CharSequence key) {
            return this.apply((CharSequence)this.transformer.apply(key), this.suffix);
        }

        public Function<CharSequence, CharSequence> curry(CharSequence suffix) {
            return new Suffix(suffix, this.transformer);
        }
    }

    public abstract static class Concatenate implements TwoArgFunction<CharSequence, CharSequence, CharSequence> {
        public Concatenate() {
        }

        public CharSequence apply(CharSequence left, CharSequence right) {
            return UTF8BytesString.create(String.valueOf(left) + right);
        }
    }

    public static final class Zero<T> implements Function<T, T> {
        public Zero() {
        }

        public T apply(T input) {
            return input;
        }
    }
}
