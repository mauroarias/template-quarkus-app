package datadog.lib.datadog.trace.api.cache;

import datadog.lib.datadog.trace.api.Function;
import datadog.lib.datadog.trace.api.TwoArgFunction;

public final class QualifiedClassNameCache {
    private final Root root;

    public QualifiedClassNameCache(Function<Class<?>, CharSequence> formatter, TwoArgFunction<CharSequence, CharSequence, CharSequence> joiner) {
        this(formatter, joiner, 16);
    }

    public QualifiedClassNameCache(Function<Class<?>, CharSequence> formatter, TwoArgFunction<CharSequence, CharSequence, CharSequence> joiner, int leafSize) {
        this.root = new Root(formatter, joiner, leafSize);
    }

    public CharSequence getClassName(Class<?> klass) {
        return ((Leaf)this.root.get(klass)).getName();
    }

    public CharSequence getQualifiedName(Class<?> klass, String qualifier) {
        return ((Leaf)this.root.get(klass)).get(qualifier);
    }

    private static class Leaf {
        private final CharSequence name;
        private final DDCache<CharSequence, CharSequence> cache;
        private final Function<CharSequence, CharSequence> joiner;

        private Leaf(CharSequence name, TwoArgFunction<CharSequence, CharSequence, CharSequence> joiner, int leafSize) {
            this.name = name;
            this.cache = DDCaches.newUnboundedCache(leafSize);
            this.joiner = joiner.curry(name);
        }

        CharSequence get(CharSequence name) {
            return (CharSequence)this.cache.computeIfAbsent(name, this.joiner);
        }

        CharSequence getName() {
            return this.name;
        }
    }

    private static final class Root extends ClassValue<Leaf> {
        private final Function<Class<?>, CharSequence> formatter;
        private final TwoArgFunction<CharSequence, CharSequence, CharSequence> joiner;
        private final int leafSize;

        private Root(Function<Class<?>, CharSequence> formatter, TwoArgFunction<CharSequence, CharSequence, CharSequence> joiner, int leafSize) {
            this.formatter = formatter;
            this.joiner = joiner;
            this.leafSize = leafSize;
        }

        protected Leaf computeValue(Class<?> type) {
            return new Leaf((CharSequence)this.formatter.apply(type), this.joiner, this.leafSize);
        }
    }
}
