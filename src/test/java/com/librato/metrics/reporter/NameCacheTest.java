package com.librato.metrics.reporter;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class NameCacheTest {
    @Test
    public void testServesUpCachedVersion() throws Exception {
        NameCache cache = new NameCache(10);

        final AtomicInteger invocations = new AtomicInteger();
        Supplier<String> supplier = new Supplier<String>() {
            @Override
            public String get() {
                invocations.incrementAndGet();
                return "value";
            }
        };
        assertThat(cache.get(new Signal("foo", "bar"), supplier)).isEqualTo("value");
        assertThat(invocations.get()).isEqualTo(1);
        assertThat(cache.get(new Signal("foo", "bar"), supplier)).isEqualTo("value");
        assertThat(invocations.get()).isEqualTo(1);

    }
}
