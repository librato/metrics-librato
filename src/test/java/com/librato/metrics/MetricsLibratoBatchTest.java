package com.librato.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test basic behaviors about MetricsLibratoBatch
 */
public class MetricsLibratoBatchTest {

    @Test
    public void testPost() throws Exception {
        MetricRegistry metrics = new MetricRegistry();
        final AtomicLong along = new AtomicLong(0);
        Counter counter = metrics.counter("test_counter");
        Gauge gauge = metrics.register("test_gauge", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return along.get();
            }
        });

        MetricsLibratoBatch batch = new MetricsLibratoBatch(500, APIUtil.noopSanitizer, 5, TimeUnit.SECONDS);
        AsyncHttpClient.BoundRequestBuilder builder = mock(AsyncHttpClient.BoundRequestBuilder.class);
        /*//when(builder.execute())

        when(builder.build()).thenReturn(req);
        batch.post(builder, "test", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        */
    }
}
