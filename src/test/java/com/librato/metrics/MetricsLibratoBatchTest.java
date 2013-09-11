package com.librato.metrics;

import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class MetricsLibratoBatchTest {
    HttpPoster httpPoster;
    CounterGaugeConverter counterConverter;

    @Before
    public void setUp() throws Exception {
        httpPoster = Mockito.mock(HttpPoster.class);
        counterConverter = new CounterGaugeConverter();
    }

    @Test
    public void testSamplingWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        assertThat(batch, HasMeasurement.of("apples.median"));
        assertThat(batch, HasMeasurement.of("apples.75th"));
        assertThat(batch, HasMeasurement.of("apples.95th"));
        assertThat(batch, HasMeasurement.of("apples.98th"));
        assertThat(batch, HasMeasurement.of("apples.99th"));
        assertThat(batch, HasMeasurement.of("apples.999th"));
    }

    @Test
    public void testMeteredWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addMetered("oranges", new DumbMetered());
        assertThat(batch, HasMeasurement.of("oranges.count"));
        assertThat(batch, HasMeasurement.of("oranges.meanRate"));
        assertThat(batch, HasMeasurement.of("oranges.1MinuteRate"));
        assertThat(batch, HasMeasurement.of("oranges.5MinuteRate"));
        assertThat(batch, HasMeasurement.of("oranges.15MinuteRate"));
    }

    @Test
    public void testSamplingWithSome() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.of(MEDIAN, PCT_75));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        assertThat(batch, HasMeasurement.of("apples.median"));
        assertThat(batch, HasMeasurement.of("apples.75th"));
        assertThat(batch, not(HasMeasurement.of("apples.95th")));
        assertThat(batch, not(HasMeasurement.of("apples.98th")));
        assertThat(batch, not(HasMeasurement.of("apples.99th")));
        assertThat(batch, not(HasMeasurement.of("apples.999th")));
    }

    @Test
    public void testMeteredWithSome() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.of(COUNT, RATE_MEAN));
        batch.addMetered("oranges", new DumbMetered());
        assertThat(batch, HasMeasurement.of("oranges.count"));
        assertThat(batch, HasMeasurement.of("oranges.meanRate"));
        assertThat(batch, not(HasMeasurement.of("oranges.1MinuteRate")));
        assertThat(batch, not(HasMeasurement.of("oranges.5MinuteRate")));
        assertThat(batch, not(HasMeasurement.of("oranges.15MinuteRate")));
    }

    @Test
    public void testSamplingWithSomeAndPrefix() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.of(MEDIAN, PCT_75));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        assertThat(batch, HasMeasurement.of("myPrefix.apples.median"));
        assertThat(batch, HasMeasurement.of("myPrefix.apples.75th"));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.95th")));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.98th")));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.99th")));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.999th")));
    }


    @Test
    public void testAddsAPrefixForAGaugeMeasurement() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGaugeMeasurement("apples", 1);
        assertThat(batch, HasMeasurement.of("myPrefix.apples"));
    }

    @Test
    public void testAddsAPrefixForACounterMeasurement() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addCounterMeasurement("apples", 1L);
        batch.addCounterMeasurement("apples", 1L); // call it twice because of counter->gauge conversion
        assertThat(batch, HasMeasurement.of("myPrefix.apples"));
    }

    @Test
    public void testAddsAPrefixForAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new SimpleGauge(1));
        assertThat(batch, HasMeasurement.of("myPrefix.apples"));
    }

    @Test
    public void testAddsAPrefixAndDelimiterForAGaugeMeasurement() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", "," , EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGaugeMeasurement("apples", 1);
        assertThat(batch, HasMeasurement.of("myPrefix,apples"));
    }

    @Test
    public void testDoesNotAddInfinityAsAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new SimpleGauge(Double.POSITIVE_INFINITY));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples")));
    }

    @Test
    public void testDoesNotAddNaNAsAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new SimpleGauge(Double.NaN));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples")));
    }

    @Test
    public void testReportsCountersAsGauges() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addCounterMeasurement("apples", 1L);
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples")));
        batch.addCounterMeasurement("apples", 2L);
        assertThat(batch, HasMeasurement.of("myPrefix.apples", 1L, SingleValueGaugeMeasurement.class));
    }

    class SimpleGauge extends Gauge {
        final Object value;

        SimpleGauge(Object value) {
            this.value = value;
        }

        @Override
        public Object value() {
            return value;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoesNotAcceptEmptyStringPrefix() throws Exception {
        newBatch("", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
    }

    class DumbMetered implements Metered {
        public TimeUnit rateUnit() {
            return TimeUnit.DAYS;
        }

        public String eventType() {
            return "no event type";
        }

        public long count() {
            return 0;
        }

        public double fifteenMinuteRate() {
            return 0;
        }

        public double fiveMinuteRate() {
            return 0;
        }

        public double meanRate() {
            return 0;
        }

        public double oneMinuteRate() {
            return 0;
        }

        public <T> void processWith(MetricProcessor<T> processor, MetricName name, T context) throws Exception {
        }
    }

    static class HasMeasurement extends BaseMatcher<LibratoBatch> {
        private final String name;
        private final Number value;
        private final Class<?> klass;

        public HasMeasurement(String name, Number value, Class<?> klass) {
            this.name = name;
            this.value = value;
            this.klass = klass;
        }

        public static HasMeasurement of(String name) {
            return new HasMeasurement(name, null, null);
        }

        public static HasMeasurement of(String name, Number value, Class<?> klass) {
            return new HasMeasurement(name, value, klass);
        }

        public boolean matches(Object o) {
            LibratoBatch batch = (LibratoBatch) o;
            for (Measurement measurement : batch.measurements) {
                if (klass != null && !klass.equals(measurement.getClass())) {
                    continue;
                }
                if (measurement.getName().equals(name)) {
                    if (value != null) {
                        final Number measurementValue = measurement.toMap().get("value");
                        return measurementValue != null && measurementValue.equals(value);
                    }
                    return true;
                }
            }
            return false;
        }

        public void describeTo(Description description) {
            description.appendText("measurement with name " + name);
        }
    }

    private MetricsLibratoBatch newBatch(String prefix, EnumSet<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(prefix, ".", metrics);
    }

    private MetricsLibratoBatch newBatch(Set<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(null, ".", metrics);
    }

    private MetricsLibratoBatch newBatch(String prefix, String prefixDelimiter, Set<LibratoReporter.ExpandedMetric> metrics) {
        final int postBatchSize = 100;
        final Sanitizer sanitizer = Sanitizer.NO_OP;
        final LibratoReporter.MetricExpansionConfig expansionConfig = new LibratoReporter.MetricExpansionConfig(EnumSet.copyOf(metrics));
        return new MetricsLibratoBatch(
                postBatchSize,
                sanitizer,
                1L,
                TimeUnit.SECONDS,
                expansionConfig,
                httpPoster,
                prefix,
                prefixDelimiter,
                counterConverter);
    }
}
