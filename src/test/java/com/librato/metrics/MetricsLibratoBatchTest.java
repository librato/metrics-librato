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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class MetricsLibratoBatchTest {
    AddsMeasurements addsMeasurements;
    HttpPoster httpPoster;

    @Before
    public void setUp() throws Exception {
        addsMeasurements = Mockito.mock(AddsMeasurements.class);
        httpPoster = Mockito.mock(HttpPoster.class);
    }

    @Test
    public void testSamplingWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.median")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.75th")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.95th")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.98th")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.99th")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.999th")));
    }

    @Test
    public void testMeteredWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addMetered("oranges", new DumbMetered());
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.count")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.meanRate")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.1MinuteRate")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.5MinuteRate")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.15MinuteRate")));
    }

    @Test
    public void testSamplingWithSome() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.of(MEDIAN, PCT_75));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.median")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.75th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("apples.95th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("apples.98th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("apples.99th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("apples.999th")));
    }

    @Test
    public void testMeteredWithSome() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.of(COUNT, RATE_MEAN));
        batch.addMetered("oranges", new DumbMetered());
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.count")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.meanRate")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("oranges.1MinuteRate")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("oranges.5MinuteRate")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("oranges.15MinuteRate")));
    }

    @Test
    public void testSamplingWithSomeAndPrefix() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.of(MEDIAN, PCT_75));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples.median")));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples.75th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples.95th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples.98th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples.99th")));
        verify(addsMeasurements, never()).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples.999th")));
    }

    @Test
    public void testAddsAPrefixForAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new SimpleGauge(1));
        verify(addsMeasurements, times(1)).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples")));
    }

    @Test
    public void testDoesNotAddInfinityAsAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new SimpleGauge(Double.POSITIVE_INFINITY));
        verify(addsMeasurements, times(0)).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples")));
    }

    @Test
    public void testDoesNotAddNaNAsAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new SimpleGauge(Double.NaN));
        verify(addsMeasurements, times(0)).addMeasurement(argThat(HasMeasurementName.of("myPrefix.apples")));
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

    static class HasMeasurementName extends BaseMatcher<Measurement> {
        private final String name;

        public HasMeasurementName(String name) {
            this.name = name;
        }

        public static HasMeasurementName of(String name) {
            return new HasMeasurementName(name);
        }

        public boolean matches(Object o) {
            Measurement measurement = (Measurement) o;
            return measurement.getName().equals(name);
        }

        public void describeTo(Description description) {
            description.appendText("measurement with name " + name);
        }
    }

    private MetricsLibratoBatch newBatch(Set<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(null, metrics);
    }

    private MetricsLibratoBatch newBatch(String prefix, Set<LibratoReporter.ExpandedMetric> metrics) {
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
                addsMeasurements,
                prefix);
    }
}
