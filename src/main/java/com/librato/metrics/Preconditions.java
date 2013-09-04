package com.librato.metrics;

/**
 * A subset of Guava's but without the extra dependency
 */
public class Preconditions {
    private Preconditions() {
        // do not instantiate
    }

    public static <T> T checkNotNull(T reference, String message) {
        if (reference == null) {
            throw new IllegalArgumentException(message);
        }
        return reference;
    }
}
