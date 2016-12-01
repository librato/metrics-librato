package com.librato.metrics.reporter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRU<K, V> {
    private final Map<K,V> cache;

    public LRU(final int maxSize) {
        this.cache = Collections.synchronizedMap(new LinkedHashMap<K, V>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void set(K key, V result) {
        cache.put(key, result);
    }
}
