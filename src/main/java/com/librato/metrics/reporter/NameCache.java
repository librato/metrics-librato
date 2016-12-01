package com.librato.metrics.reporter;

public class NameCache {
    public static class Key {
        final String name;
        final Signal signal;

        public Key(String name, Signal signal) {
            this.name = name;
            this.signal = signal;
        }
    }

    private final LRU<Key, String> cache;

    public NameCache(int maxSize) {
        this.cache = new LRU<Key, String>(maxSize);
    }

    public String get(String name, Signal signal, Supplier<String> fullNameSupplier) {
        Key key = new Key(name, signal);
        String result = cache.get(key);
        if (result == null) {
            result = fullNameSupplier.get();
            cache.set(key, result);
        }
        return result;
    }
}
