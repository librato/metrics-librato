package com.librato.metrics.reporter;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.librato.metrics.client.Tag;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class LibratoTest {
    MetricRegistry registry = new MetricRegistry();

    @Test(expected = RuntimeException.class)
    public void testErrorsOnSameNameDifferentTypes() throws Exception {
        Librato.metric(registry, "foo").timer();
        Librato.metric(registry, "foo").histogram();
    }

    @Test
    public void testReturnsSameMetric() throws Exception {
        Gauge<Integer> gauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return 42;
            }
        };

        assertThat(Librato.metric(registry, "gauge").gauge(gauge))
                .isSameAs(Librato.metric(registry, "gauge").gauge(gauge));

        assertThat(Librato.metric(registry, "foo").timer())
                .isSameAs(Librato.metric(registry, "foo").timer());

        assertThat(Librato.metric(registry, "bar").source("baz").tag("region", "us-east-1").histogram()).isSameAs(
                Librato.metric(registry, "bar").source("baz").tag("region", "us-east-1").histogram());
    }

    @Test
    public void testNoNameConversion() throws Exception {
        Librato.metric(registry, "gauge").gauge(new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return 42;
            }
        });
        assertThat(Signal.decode(registry.getGauges().keySet().iterator().next())).isEqualTo(
                new Signal("gauge"));

        Librato.metric(registry, "counter").counter();
        assertThat(Signal.decode(registry.getCounters().keySet().iterator().next())).isEqualTo(
                new Signal("counter"));

        Librato.metric(registry, "histogram").histogram();
        assertThat(Signal.decode(registry.getHistograms().keySet().iterator().next())).isEqualTo(
                new Signal("histogram"));

        Librato.metric(registry, "meter").meter();
        assertThat(Signal.decode(registry.getMeters().keySet().iterator().next())).isEqualTo(
                new Signal("meter"));

        Librato.metric(registry, "timer").timer();
        assertThat(Signal.decode(registry.getTimers().keySet().iterator().next())).isEqualTo(
                new Signal("timer"));
    }

    @Test
    public void testSourceConversion() throws Exception {
        Librato.metric(registry, "foo").source("test-source").counter();
        Signal signal = Signal.decode(registry.getCounters().keySet().iterator().next());
        assertThat(signal.name).isEqualTo("foo");
        assertThat(signal.source).isEqualTo("test-source");
        assertThat(signal.tags).isEmpty();
        assertThat(signal.overrideTags).isFalse();
    }

    @Test
    public void testTagsConversion() throws Exception {
        Librato.metric(registry, "foo")
                .tag("region", "us-east-1")
                .inheritTags(false)
                .counter();
        Signal signal = Signal.decode(registry.getCounters().keySet().iterator().next());
        assertThat(signal).isEqualTo(new Signal(
                "foo",
                null,
                asList(new Tag("region", "us-east-1")),
                true));
    }

    @Test
    public void testSourceAndTagsConversion() throws Exception {
        Librato.metric(registry, "foo")
                .tag("region", "us-east-1")
                .inheritTags(false)
                .source("bar")
                .counter();
        Signal signal = Signal.decode(registry.getCounters().keySet().iterator().next());
        assertThat(signal).isEqualTo(new Signal(
                "foo",
                "bar",
                asList(new Tag("region", "us-east-1")),
                true));
    }
}
