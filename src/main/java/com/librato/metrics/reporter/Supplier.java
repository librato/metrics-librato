package com.librato.metrics.reporter;

public interface Supplier<T> {
    T get();
}
