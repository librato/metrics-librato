package com.librato.metrics;

import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.Sampling;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MetricsLibratoBatchTest {
    AddsMeasurements adds;

    @Before
    public void setUp() throws Exception {
        adds = Mockito.mock(AddsMeasurements.class);
    }

    @Test
    public void testSamplingWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.median")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.75th")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.95th")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.98th")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.99th")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.999th")));
    }

    @Test
    public void testMeteredWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addMetered("oranges", new DumbMetered());
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.count")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.meanRate")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.1MinuteRate")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.5MinuteRate")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.15MinuteRate")));
    }

    @Test
    public void testSamplingWithSome() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.of(MEDIAN, PCT_75));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new double[]{1.0});
            }
        });
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.median")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("apples.75th")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("apples.95th")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("apples.98th")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("apples.99th")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("apples.999th")));
    }

    @Test
    public void testMeteredWithSome() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.of(COUNT, RATE_MEAN));
        batch.addMetered("oranges", new DumbMetered());
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.count")));
        verify(adds, times(1)).addMeasurement(argThat(HasMeasurementName.of("oranges.meanRate")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("oranges.1MinuteRate")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("oranges.5MinuteRate")));
        verify(adds, never()).addMeasurement(argThat(HasMeasurementName.of("oranges.15MinuteRate")));
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
        final int postBatchSize = 100;
        final APIUtil.Sanitizer sanitizer = APIUtil.noopSanitizer;
        final LibratoReporter.MetricExpansionConfig expansionConfig = new LibratoReporter.MetricExpansionConfig(EnumSet.copyOf(metrics));
        return new MetricsLibratoBatch(postBatchSize,
                sanitizer,
                1L,
                TimeUnit.SECONDS,
                expansionConfig,
                adds);
    }
}
