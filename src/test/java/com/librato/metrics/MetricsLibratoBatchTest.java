package com.librato.metrics;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Sampling;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.stats.Snapshot;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsLibratoBatchTest {
    HttpPoster httpPoster;
    CounterGaugeConverter counterConverter;

    @Before
    public void setUp() throws Exception {
        httpPoster = mock(HttpPoster.class);
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
        batch.addMetered("oranges", new FakeMetered());
        assertThat(batch.measurements.size(), is(0));
        batch.addMetered("oranges", new FakeMetered());
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
        batch.addMetered("oranges", new FakeMetered());
        assertThat(batch.measurements.size(), is(0));
        batch.addMetered("oranges", new FakeMetered());
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
        assertThat(batch, HasMeasurement.of("myPrefix.apples"));
    }

    @Test
    public void testAddsAPrefixForAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new FakeGauge(1));
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
        batch.addGauge("apples", new FakeGauge(Double.POSITIVE_INFINITY));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples")));
    }

    @Test
    public void testDoesNotAddNaNAsAGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGauge("apples", new FakeGauge(Double.NaN));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples")));
    }

    @Test
    public void testGauge() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        batch.addGauge("foo", new FakeGauge(1L));
        assertThat(batch, HasMeasurement.of("foo", 1L, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testCounter() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        final Counter counter = mock(Counter.class);
        when(counter.count()).thenReturn(1L);
        batch.addCounter("foo", counter);
        assertThat(batch, HasMeasurement.of("foo", 1L, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testHistogram() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        final Histogram histogram = mock(Histogram.class);
        when(histogram.count()).thenReturn(1L).thenReturn(2L);
        when(histogram.max()).thenReturn(10d);
        when(histogram.min()).thenReturn(0d);
        when(histogram.mean()).thenReturn(5d);
        when(histogram.stdDev()).thenReturn(0d);
        when(histogram.sum()).thenReturn(20d);

        final Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMedian()).thenReturn(50d);
        when(snapshot.get75thPercentile()).thenReturn(75d);
        when(snapshot.get95thPercentile()).thenReturn(95d);
        when(snapshot.get98thPercentile()).thenReturn(98d);
        when(snapshot.get99thPercentile()).thenReturn(99d);
        when(snapshot.get999thPercentile()).thenReturn(99.9);

        batch.addHistogram("foo", histogram);
        assertThat(batch.measurements.size(), is(0));
        batch.addHistogram("foo", histogram);

        assertThat(batch, new HasMultiSampleGaugeMeasurement("foo", 4L, 20d, 10d, 0d));

        assertThat(batch, HasMeasurement.of("foo.count", 1L, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.median", 50d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.75th", 75d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.95th", 95d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.98th", 98d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.99th", 99d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.999th", 99.9d, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testMeter() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        final FakeMetered meteredOne = new FakeMetered(1L, 2d, 1d, 5d, 15d);
        final FakeMetered meteredTwo = new FakeMetered(2L, 2d, 1d, 5d, 15d);

        batch.addMetered("foo", meteredOne);
        assertThat(batch.measurements.size(), is(0));
        batch.addMetered("foo", meteredTwo);

        assertThat(batch, HasMeasurement.of("foo.count", 1L, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.meanRate", 2d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.1MinuteRate", 1d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.5MinuteRate", 5d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.15MinuteRate", 15d, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testTimer() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        final Timer timer = mock(Timer.class);
        when(timer.count()).thenReturn(1L).thenReturn(2L);
        when(timer.max()).thenReturn(10d);
        when(timer.min()).thenReturn(0d);
        when(timer.mean()).thenReturn(5d);
        when(timer.stdDev()).thenReturn(0d);
        when(timer.sum()).thenReturn(20d);

        final Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);
        when(snapshot.getMedian()).thenReturn(50d);
        when(snapshot.get75thPercentile()).thenReturn(75d);
        when(snapshot.get95thPercentile()).thenReturn(95d);
        when(snapshot.get98thPercentile()).thenReturn(98d);
        when(snapshot.get99thPercentile()).thenReturn(99d);
        when(snapshot.get999thPercentile()).thenReturn(99.9);

        when(timer.count()).thenReturn(1L).thenReturn(2L);

        when(timer.meanRate()).thenReturn(2d);
        when(timer.oneMinuteRate()).thenReturn(1d);
        when(timer.fiveMinuteRate()).thenReturn(5d);
        when(timer.fifteenMinuteRate()).thenReturn(15d);

        batch.addTimer("foo", timer);
        assertThat(batch.measurements.size(), is(0));
        batch.addTimer("foo", timer);

        assertThat(batch, new HasMultiSampleGaugeMeasurement("foo", 4L, 20d, 10d, 0d));

        assertThat(batch, HasMeasurement.of("foo.count", 1L, SingleValueGaugeMeasurement.class));

        assertThat(batch, HasMeasurement.of("foo.median", 50d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.75th", 75d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.95th", 95d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.98th", 98d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.99th", 99d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.999th", 99.9d, SingleValueGaugeMeasurement.class));

        assertThat(batch, HasMeasurement.of("foo.meanRate", 2d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.1MinuteRate", 1d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.5MinuteRate", 5d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.15MinuteRate", 15d, SingleValueGaugeMeasurement.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoesNotAcceptEmptyStringPrefix() throws Exception {
        newBatch("", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
    }

    static class HasMultiSampleGaugeMeasurement extends BaseMatcher<LibratoBatch> {
        final String name;
        final long count;
        final Number sum;
        final Number max;
        final Number min;

        HasMultiSampleGaugeMeasurement(String name, long count, Number sum, Number max, Number min) {
            this.name = name;
            this.count = count;
            this.sum = sum;
            this.max = max;
            this.min = min;
        }

        public boolean matches(Object o) {
            LibratoBatch batch = (LibratoBatch)o;
            for (Measurement measurement : batch.measurements) {
                if (measurement instanceof MultiSampleGaugeMeasurement) {
                    MultiSampleGaugeMeasurement multi = (MultiSampleGaugeMeasurement) measurement;
                    if (!multi.getName().equals(name)) {
                        return false;
                    }
                    final Map<String,Number> map = multi.toMap();
                    if (!map.get("count").equals(count)) {
                        return false;
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

    private MetricsLibratoBatch newBatch() {
        return newBatch(null, ".", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
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
