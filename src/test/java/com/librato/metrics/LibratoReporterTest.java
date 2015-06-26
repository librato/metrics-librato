package com.librato.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;

import java.io.IOException;
import java.util.SortedMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class LibratoReporterTest extends TestCase {

    class FailingRegistry extends MetricRegistry {
        AtomicInteger timesAttempted = new AtomicInteger();

        @Override
        public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
            timesAttempted.addAndGet(1);
            throw new RuntimeException("test-exception");
        }
    }

    public void testIsResilientToExceptionsInReport() throws Exception {
        FailingRegistry registry = new FailingRegistry();
        HttpPoster poster = new HttpPoster() {
            public Future<Response> post(String userAgent, String payload) throws IOException {
                return null;
            }

            public void close() throws IOException {
              // Intentional NOP
            }
        };
        LibratoReporter reporter = LibratoReporter.builder(registry, "test-username", "test-token", "test-source")
                .setHttpPoster(poster)
                .build();
        // assert that it doesn't throw an exception
        reporter.report();
        // verify the failure occurred
        Assert.assertThat(registry.timesAttempted.get(), CoreMatchers.equalTo(1));
    }
}
