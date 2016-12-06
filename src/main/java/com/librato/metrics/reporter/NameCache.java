package com.librato.metrics.reporter;

public class NameCache {
    private final LRU<Signal, String> cache;

    public NameCache(int maxSize) {
        this.cache = new LRU<Signal, String>(maxSize);
    }

    public String get(Signal signal, Supplier<String> fullNameSupplier) {
        String result = cache.get(signal);
        if (result == null) {
            result = fullNameSupplier.get();
            cache.set(signal, result);
        }
        return result;
    }
}
