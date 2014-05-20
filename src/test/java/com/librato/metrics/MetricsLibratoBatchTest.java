package com.librato.metrics;

import com.codahale.metrics.*;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsLibratoBatchTest {
    HttpPoster httpPoster;
    DeltaTracker deltaTracker;

    @Before
    public void setUp() throws Exception {
        httpPoster = mock(HttpPoster.class);
        deltaTracker = new DeltaTracker();
    }

    @Test
    public void testSamplingWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addSampling("apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new long[]{1});
            }
        }, false);
        assertThat(batch, HasMeasurement.of("apples.median"));
        assertThat(batch, HasMeasurement.of("apples.75th"));
        assertThat(batch, HasMeasurement.of("apples.95th"));
        assertThat(batch, HasMeasurement.of("apples.98th"));
        assertThat(batch, HasMeasurement.of("apples.99th"));
        assertThat(batch, HasMeasurement.of("apples.999th"));
        assertThat(batch, HasMeasurement.of("apples"));
    }

    @Test
    public void testSamplingWithCustomSource() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addSampling("farm--apples", new Sampling() {
            public Snapshot getSnapshot() {
                return new Snapshot(new long[]{1});
            }
        }, false);
        assertThat(batch, HasMeasurement.of("farm", "apples.median"));
        assertThat(batch, HasMeasurement.of("farm", "apples.75th"));
        assertThat(batch, HasMeasurement.of("farm", "apples.95th"));
        assertThat(batch, HasMeasurement.of("farm", "apples.98th"));
        assertThat(batch, HasMeasurement.of("farm", "apples.99th"));
        assertThat(batch, HasMeasurement.of("farm", "apples.999th"));
        assertThat(batch, HasMeasurement.of("farm", "apples"));
    }

    @Test
    public void testMeteredWithAll() throws Exception {
        final MetricsLibratoBatch batch = newBatch(EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addMeter("oranges", new FakeMetered());
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
                return new Snapshot(new long[]{1});
            }
        }, false);
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
        batch.addMeter("oranges", new FakeMetered());
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
                return new Snapshot(new long[]{1});
            }
        }, false);
        assertThat(batch, HasMeasurement.of("myPrefix.apples.median"));
        assertThat(batch, HasMeasurement.of("myPrefix.apples.75th"));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.95th")));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.98th")));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.99th")));
        assertThat(batch, not(HasMeasurement.of("myPrefix.apples.999th")));
    }

    @Test
    public void testAddsPrefixForACounterMeasurement() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addCounterMeasurement("apples", 1L);
        batch.addCounterMeasurement("apples", 1L); // call it twice because of counter->gauge conversion
        assertThat(batch, HasMeasurement.of("myPrefix.apples"));
    }

    @Test
    public void testReportsCountersAsCounters() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addCounterMeasurement("apples", 1L);
        assertThat(batch, HasMeasurement.of("myPrefix.apples", 1L, CounterMeasurement.class));
    }

    @Test
    public void testAddsAPrefixAndDelimiterForAGaugeMeasurement() throws Exception {
        final MetricsLibratoBatch batch = newBatch("myPrefix", "," , EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
        batch.addGaugeMeasurement("apples", 1);
        assertThat(batch, HasMeasurement.of("myPrefix,apples"));
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
        when(counter.getCount()).thenReturn(1L);
        batch.addCounter("foo", counter);
        assertThat(batch, HasMeasurement.of("foo", 1L, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testGaugeWithSource() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        batch.addGauge("mysource--foo", new FakeGauge(1L));
        assertThat(batch, HasMeasurement.of("mysource", "foo", 1L, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testGaugeWithSourceAndDashesInName() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        batch.addGauge("mysource--foo-bar", new FakeGauge(1L));
        assertThat(batch, HasMeasurement.of("mysource", "foo-bar", 1L, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testCounterWithSource() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(1L);
        batch.addCounter("othersource--foo", counter);
        assertThat(batch, HasMeasurement.of("othersource", "foo", 1L, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testHistogram() throws Exception {
        final MetricsLibratoBatch batch = newBatch();
        final Histogram histogram = mock(Histogram.class);
        final Snapshot snapshot = mock(Snapshot.class);
        when(histogram.getSnapshot()).thenReturn(snapshot);

        when(histogram.getCount()).thenReturn(1L);
        when(snapshot.size()).thenReturn(4);
        when(snapshot.getMax()).thenReturn(10L);
        when(snapshot.getMin()).thenReturn(0L);
        when(snapshot.getMean()).thenReturn(5d);
        when(snapshot.getStdDev()).thenReturn(0d);

        when(snapshot.getMedian()).thenReturn(50d);
        when(snapshot.get75thPercentile()).thenReturn(75d);
        when(snapshot.get95thPercentile()).thenReturn(95d);
        when(snapshot.get98thPercentile()).thenReturn(98d);
        when(snapshot.get99thPercentile()).thenReturn(99d);
        when(snapshot.get999thPercentile()).thenReturn(99.9);

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
        batch.addMeter("foo", meteredOne);

        assertThat(batch, HasMeasurement.of("foo.count", 1L, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.meanRate", 2d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.1MinuteRate", 1d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.5MinuteRate", 5d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.15MinuteRate", 15d, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testMeterWithRateConversion() throws Exception {
        final MetricsLibratoBatch batch = this.newBatch(new MetricsLibratoBatch.RateConverter() {
            public double convertMetricRate(double rate) {
                return rate * 2;
            }
        });
        final FakeMetered meteredOne = new FakeMetered(1L, 2d, 1d, 5d, 15d);
        batch.addMeter("foo", meteredOne);

        assertThat(batch, HasMeasurement.of("foo.count", 1L, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.meanRate", 4d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.1MinuteRate", 2d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.5MinuteRate", 10d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.15MinuteRate", 30d, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testTimer() throws Exception {
        final MetricsLibratoBatch batch = newBatch(new MetricsLibratoBatch.DurationConverter() {
            public double convertMetricDuration(double duration) {
                return duration * 2;
            }
        });
        final Timer timer = mock(Timer.class);
        final Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);

        when(timer.getCount()).thenReturn(1L);
        when(snapshot.size()).thenReturn(4);
        when(snapshot.getMax()).thenReturn(10L);
        when(snapshot.getMin()).thenReturn(0L);
        when(snapshot.getMean()).thenReturn(5d);
        when(snapshot.getStdDev()).thenReturn(0d);

        when(snapshot.getMedian()).thenReturn(50d);
        when(snapshot.get75thPercentile()).thenReturn(75d);
        when(snapshot.get95thPercentile()).thenReturn(95d);
        when(snapshot.get98thPercentile()).thenReturn(98d);
        when(snapshot.get99thPercentile()).thenReturn(99d);
        when(snapshot.get999thPercentile()).thenReturn(99.9);

        when(timer.getCount()).thenReturn(1L).thenReturn(2L);

        when(timer.getMeanRate()).thenReturn(2d);
        when(timer.getOneMinuteRate()).thenReturn(1d);
        when(timer.getFiveMinuteRate()).thenReturn(5d);
        when(timer.getFifteenMinuteRate()).thenReturn(15d);

        batch.addTimer("foo", timer);

        // todo: should this be using the total count from the timer instead of the snapshot? probably.

        assertThat(batch, new HasMultiSampleGaugeMeasurement("foo", 4L, 20d, 10d, 0d));
        assertThat(batch, HasMeasurement.of("foo.count", 1L, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.median", 100d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.75th", 150d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.95th", 190d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.98th", 196d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.99th", 198d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.999th", 199.8d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.meanRate", 2d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.1MinuteRate", 1d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.5MinuteRate", 5d, SingleValueGaugeMeasurement.class));
        assertThat(batch, HasMeasurement.of("foo.15MinuteRate", 15d, SingleValueGaugeMeasurement.class));
    }

    @Test
    public void testSetsInitialValuesInTheDeltaTracker() throws Exception {
        final FakeMetered metered = new FakeMetered(1L, 2d, 1d, 5d, 15d); // a count of one

        // we'll test the simple case of a meter and that when the tracker is created it initializes the values
        deltaTracker = new DeltaTracker(new DeltaTracker.MetricSupplier() {
            public Map<String, Metric> getMetrics() {
                final Map<String, Metric> map = new HashMap<String, Metric>();
                map.put("foo", metered);
                return map;
            }
        });
        // the batch will be constructed with the deltaTracker just created
        final MetricsLibratoBatch batch = newBatch();
        batch.addMeter("foo", new FakeMetered(6L, 2d, 1d, 5d, 15d)); // increment by five
        assertThat(batch, HasMeasurement.of("foo.count", 5l, SingleValueGaugeMeasurement.class));
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
                    return map.get("count").equals(count);
                }
            }
            return false;
        }

        public void describeTo(Description description) {
            description.appendText("measurement with name " + name);
        }
    }

    static class HasMeasurement extends BaseMatcher<LibratoBatch> {
        private final String source;
        private final String name;
        private final Number value;
        private final Class<?> klass;

        public HasMeasurement(String source, String name, Number value, Class<?> klass) {
            this.source = source;
            this.name = name;
            this.value = value;
            this.klass = klass;
        }

        public static HasMeasurement of(String name) {
            return new HasMeasurement(null, name, null, null);
        }

        public static HasMeasurement of(String source, String name) {
            return new HasMeasurement(source, name, null, null);
        }

        public static HasMeasurement of(String name, Number value, Class<?> klass) {
            return new HasMeasurement(null, name, value, klass);
        }

        public static HasMeasurement of(String source, String name, Number value, Class<?> klass) {
            return new HasMeasurement(source, name, value, klass);
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
                        if (measurementValue == null || !measurementValue.equals(value)) {
                            return false;
                        }
                    }
                    if (source != null && measurement.getSource() == null) {
                        return false;
                    }
                    if (source == null && measurement.getSource() != null) {
                        return false;
                    }
                    if (source != null && !source.equals(measurement.getSource())) {
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

    private MetricsLibratoBatch newBatch(String prefix, EnumSet<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(prefix, ".", metrics);
    }

    private MetricsLibratoBatch newBatch() {
        return newBatch(null, ".", EnumSet.allOf(LibratoReporter.ExpandedMetric.class));
    }

    private MetricsLibratoBatch newBatch(MetricsLibratoBatch.RateConverter rateConverter) {
        return newBatch(null, ".", EnumSet.allOf(LibratoReporter.ExpandedMetric.class), rateConverter, identityDurationConverter);
    }

    private MetricsLibratoBatch newBatch(MetricsLibratoBatch.DurationConverter durationConverter) {
        return newBatch(null, ".", EnumSet.allOf(LibratoReporter.ExpandedMetric.class), identityRateConverter, durationConverter);
    }

    private MetricsLibratoBatch newBatch(Set<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(null, ".", metrics);
    }

    private MetricsLibratoBatch newBatch(String prefix, Set<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(prefix, ".", metrics);
    }

    static MetricsLibratoBatch.RateConverter identityRateConverter = new MetricsLibratoBatch.RateConverter() {
        public double convertMetricRate(double rate) {
            return rate;
        }
    };

    static MetricsLibratoBatch.DurationConverter identityDurationConverter = new MetricsLibratoBatch.DurationConverter() {
        public double convertMetricDuration(double duration) {
            return duration;
        }
    };

    private MetricsLibratoBatch newBatch(String prefix, String prefixDelimiter, Set<LibratoReporter.ExpandedMetric> metrics) {
        return newBatch(prefix, prefixDelimiter, metrics, identityRateConverter, identityDurationConverter);
    }

    private MetricsLibratoBatch newBatch(String prefix,
                                         String prefixDelimiter,
                                         Set<LibratoReporter.ExpandedMetric> metrics,
                                         MetricsLibratoBatch.RateConverter rateConverter,
                                         MetricsLibratoBatch.DurationConverter durationConverter) {
        final int postBatchSize = 100;
        final Sanitizer sanitizer = Sanitizer.NO_OP;
        final LibratoReporter.MetricExpansionConfig expansionConfig = new LibratoReporter.MetricExpansionConfig(EnumSet.copyOf(metrics));
        final Pattern sourceRegex = Pattern.compile("^(.*?)--");
        return new MetricsLibratoBatch(
                postBatchSize,
                sanitizer,
                1L,
                TimeUnit.SECONDS,
                expansionConfig,
                httpPoster,
                prefix,
                prefixDelimiter,
                deltaTracker,
                rateConverter,
                durationConverter,
                sourceRegex);
    }
}
