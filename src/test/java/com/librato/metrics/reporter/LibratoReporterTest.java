package com.librato.metrics.reporter;

import com.codahale.metrics.*;
import com.librato.metrics.client.*;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LibratoReporterTest {
    MetricRegistry registry = new MetricRegistry();
    LibratoClient client = mock(LibratoClient.class);
    ReporterAttributes atts = new ReporterAttributes();
    ArgumentCaptor<Measures> captor = ArgumentCaptor.forClass(Measures.class);

    @Before
    public void setUp() throws Exception {
        atts.sourceRegex = Pattern.compile("^(.*)--");
        atts.libratoClientFactory = new ILibratoClientFactory() {
            public LibratoClient build(ReporterAttributes atts) {
                return client;
            }
        };
        when(client.postMeasures(captor.capture()))
                .thenReturn(new PostMeasuresResult());
        atts.durationConverter = new DurationConverter() {
            @Override
            public double convertDuration(double duration) {
                return duration;
            }
        };
        atts.rateConverter = new RateConverter() {
            @Override
            public double convertRate(double rate) {
                return rate;
            }
        };
    }

    private void report(LibratoReporter reporter) {
        reporter.report(registry.getGauges(),
                registry.getCounters(),
                registry.getHistograms(),
                registry.getMeters(),
                registry.getTimers());
    }

    @Test
    public void testOmitsTagsAtTheRootLevel() throws Exception {
        atts.enableLegacy = false;
        atts.enableTagging = true;
        atts.tags.add(new Tag("root", "tag"));

        Librato.metric(registry, "foo").tag("foo", "bar").counter().inc();
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        Measures captured = captor.getValue();
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captured.getMeasures());
        assertThat(captured.getTags()).isEmpty();
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo", 1, new Tag("foo", "bar"), new Tag("root", "tag")));
    }

    @Test
    public void testCounter() throws Exception {
        Counter counter = new Counter();
        registry.register("foo", counter);
        counter.inc();
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo", 1));
    }

    @Test
    public void testRejectsNonTaggedMetrics() throws Exception {
        atts.enableLegacy = false;
        atts.enableTagging = true;
        Librato.metric(registry, "foo").counter().inc();
        LibratoReporter reporter;
        reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures;
        measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures.isEmpty()).isFalse();
    }

    @Test
    public void testTaggedCounter() throws Exception {
        atts.enableLegacy = false;
        atts.enableTagging = true;
        Counter counter = Librato.metric(registry, "foo").tag("a", "b").counter();
        counter.inc();
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo", 1, new Tag("a", "b")));
    }

    @Test
    public void testTimer() throws Exception {
        Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2d);
        when(timer.getOneMinuteRate()).thenReturn(3d);
        when(timer.getFiveMinuteRate()).thenReturn(4d);
        when(timer.getFifteenMinuteRate()).thenReturn(5d);
        Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);
        when(snapshot.size()).thenReturn(1);
        when(snapshot.getMean()).thenReturn(6d);
        when(snapshot.getMin()).thenReturn(7L);
        when(snapshot.getMax()).thenReturn(8L);
        when(snapshot.getMedian()).thenReturn(9d);
        when(snapshot.get75thPercentile()).thenReturn(10d);
        when(snapshot.get95thPercentile()).thenReturn(11d);
        when(snapshot.get98thPercentile()).thenReturn(12d);
        when(snapshot.get99thPercentile()).thenReturn(13d);
        when(snapshot.get999thPercentile()).thenReturn(14d);
        registry.register("foo", timer);
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo", snapshot.getMean() * timer.getCount(), timer.getCount(), snapshot.getMin(), snapshot.getMax()),
                new GaugeMeasure("foo.count", timer.getCount()),
                new GaugeMeasure("foo.median", snapshot.getMedian()),
                new GaugeMeasure("foo.75th", snapshot.get75thPercentile()),
                new GaugeMeasure("foo.95th", snapshot.get95thPercentile()),
                new GaugeMeasure("foo.98th", snapshot.get98thPercentile()),
                new GaugeMeasure("foo.99th", snapshot.get99thPercentile()),
                new GaugeMeasure("foo.999th", snapshot.get999thPercentile()),
                new GaugeMeasure("foo.meanRate", timer.getMeanRate()),
                new GaugeMeasure("foo.1MinuteRate", timer.getOneMinuteRate()),
                new GaugeMeasure("foo.5MinuteRate", timer.getFiveMinuteRate()),
                new GaugeMeasure("foo.15MinuteRate", timer.getFifteenMinuteRate()));
    }

    @Test
    public void testTaggedTimer() throws Exception {
        atts.enableLegacy = false;
        atts.enableTagging = true;

        Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2d);
        when(timer.getOneMinuteRate()).thenReturn(3d);
        when(timer.getFiveMinuteRate()).thenReturn(4d);
        when(timer.getFifteenMinuteRate()).thenReturn(5d);
        Snapshot snapshot = mock(Snapshot.class);
        when(timer.getSnapshot()).thenReturn(snapshot);
        when(snapshot.size()).thenReturn(1);
        when(snapshot.getMean()).thenReturn(6d);
        when(snapshot.getMin()).thenReturn(7L);
        when(snapshot.getMax()).thenReturn(8L);
        when(snapshot.getMedian()).thenReturn(9d);
        when(snapshot.get75thPercentile()).thenReturn(10d);
        when(snapshot.get95thPercentile()).thenReturn(11d);
        when(snapshot.get98thPercentile()).thenReturn(12d);
        when(snapshot.get99thPercentile()).thenReturn(13d);
        when(snapshot.get999thPercentile()).thenReturn(14d);
        Librato.metric(registry, "foo").tag("a", "z").timer(timer);
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo", snapshot.getMean() * timer.getCount(), timer.getCount(), snapshot.getMin(), snapshot.getMax(), new Tag("a", "z")),
                new TaggedMeasure("foo.count", timer.getCount(), new Tag("a", "z")),
                new TaggedMeasure("foo.median", snapshot.getMedian(), new Tag("a", "z")),
                new TaggedMeasure("foo.75th", snapshot.get75thPercentile(), new Tag("a", "z")),
                new TaggedMeasure("foo.95th", snapshot.get95thPercentile(), new Tag("a", "z")),
                new TaggedMeasure("foo.98th", snapshot.get98thPercentile(), new Tag("a", "z")),
                new TaggedMeasure("foo.99th", snapshot.get99thPercentile(), new Tag("a", "z")),
                new TaggedMeasure("foo.999th", snapshot.get999thPercentile(), new Tag("a", "z")),
                new TaggedMeasure("foo.meanRate", timer.getMeanRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.1MinuteRate", timer.getOneMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.5MinuteRate", timer.getFiveMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.15MinuteRate", timer.getFifteenMinuteRate(), new Tag("a", "z")));
    }

    @Test
    public void testMeter() throws Exception {
        Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2d);
        when(meter.getOneMinuteRate()).thenReturn(3d);
        when(meter.getFiveMinuteRate()).thenReturn(4d);
        when(meter.getFifteenMinuteRate()).thenReturn(5d);
        registry.register("foo", meter);
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo.count", 1),
                new GaugeMeasure("foo.meanRate", meter.getMeanRate()),
                new GaugeMeasure("foo.1MinuteRate", meter.getOneMinuteRate()),
                new GaugeMeasure("foo.5MinuteRate", meter.getFiveMinuteRate()),
                new GaugeMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate()));
    }

    @Test
    public void testTaggedMeter() throws Exception {
        atts.enableLegacy = false;
        atts.enableTagging = true;

        Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2d);
        when(meter.getOneMinuteRate()).thenReturn(3d);
        when(meter.getFiveMinuteRate()).thenReturn(4d);
        when(meter.getFifteenMinuteRate()).thenReturn(5d);
        Librato.metric(registry, "foo").tag("a", "z").meter(meter);
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo.count", 1, new Tag("a", "z")),
                new TaggedMeasure("foo.meanRate", meter.getMeanRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.1MinuteRate", meter.getOneMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.5MinuteRate", meter.getFiveMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate(), new Tag("a", "z")));
    }

    @Test
    public void testInheritsSource() throws Exception {
        atts.enableLegacy = true;
        atts.enableTagging = true;
        atts.tags.add(new Tag("source", "slingbot"));

        Counter counter = Librato.metric(registry, "foo").source("uid:362").counter();
        counter.inc();

        LibratoReporter reporter = new LibratoReporter(atts);
        HashSet<IMeasure> measures;

        report(reporter);
        measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo",1).setSource("uid:362"),
                new TaggedMeasure("foo", 1, 1, 1, 1, new Tag("source", "uid:362")));

    }

    @Test
    public void testTaggedAndSourceMeter() throws Exception {
        atts.enableLegacy = true;
        atts.enableTagging = true;

        Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2d);
        when(meter.getOneMinuteRate()).thenReturn(3d);
        when(meter.getFiveMinuteRate()).thenReturn(4d);
        when(meter.getFifteenMinuteRate()).thenReturn(5d);
        Librato.metric(registry, "foo").tag("a", "z").source("test").meter(meter);
        LibratoReporter reporter = new LibratoReporter(atts);
        HashSet<IMeasure> measures;

        report(reporter);
        measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo.count", 1, new Tag("a", "z")),
                new TaggedMeasure("foo.meanRate", meter.getMeanRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.1MinuteRate", meter.getOneMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.5MinuteRate", meter.getFiveMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate(), new Tag("a", "z")),
                new GaugeMeasure("foo.count", 1).setSource("test"),
                new GaugeMeasure("foo.meanRate", meter.getMeanRate()).setSource("test"),
                new GaugeMeasure("foo.1MinuteRate", meter.getOneMinuteRate()).setSource("test"),
                new GaugeMeasure("foo.5MinuteRate", meter.getFiveMinuteRate()).setSource("test"),
                new GaugeMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate()).setSource("test"));

        // this time it should be empty because there has been no count increase
        report(reporter);
        measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).isEmpty();

        // bump the count and verify that the values report correctly
        when(meter.getCount()).thenReturn(2L);
        report(reporter);
        measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo.count", 1, new Tag("a", "z")), // delta
                new TaggedMeasure("foo.meanRate", meter.getMeanRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.1MinuteRate", meter.getOneMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.5MinuteRate", meter.getFiveMinuteRate(), new Tag("a", "z")),
                new TaggedMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate(), new Tag("a", "z")),
                new GaugeMeasure("foo.count", 1).setSource("test"), // delta
                new GaugeMeasure("foo.meanRate", meter.getMeanRate()).setSource("test"),
                new GaugeMeasure("foo.1MinuteRate", meter.getOneMinuteRate()).setSource("test"),
                new GaugeMeasure("foo.5MinuteRate", meter.getFiveMinuteRate()).setSource("test"),
                new GaugeMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate()).setSource("test"));
    }

    @Test
    public void testHisto() throws Exception {
        Histogram histo = new Histogram(new UniformReservoir());
        histo.update(42);
        registry.register("foo", histo);
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo", 42, 1, 42, 42),
                new GaugeMeasure("foo.count", 1),
                new GaugeMeasure("foo.median", 42),
                new GaugeMeasure("foo.75th", 42),
                new GaugeMeasure("foo.95th", 42),
                new GaugeMeasure("foo.98th", 42),
                new GaugeMeasure("foo.99th", 42),
                new GaugeMeasure("foo.999th", 42));
    }

    @Test
    public void testTaggedHisto() throws Exception {
        atts.enableLegacy = false;
        atts.enableTagging = true;

        Histogram histo = new Histogram(new UniformReservoir());
        histo.update(42);
        Librato.metric(registry, "foo").tag("a", "z").histogram(histo);
        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new TaggedMeasure("foo", 42, 1, 42, 42, new Tag("a", "z")),
                new TaggedMeasure("foo.count", 1, new Tag("a", "z")),
                new TaggedMeasure("foo.median", 42, new Tag("a", "z")),
                new TaggedMeasure("foo.75th", 42, new Tag("a", "z")),
                new TaggedMeasure("foo.95th", 42, new Tag("a", "z")),
                new TaggedMeasure("foo.98th", 42, new Tag("a", "z")),
                new TaggedMeasure("foo.99th", 42, new Tag("a", "z")),
                new TaggedMeasure("foo.999th", 42, new Tag("a", "z")));
    }

    @Test
    public void testUsesSourceRegexForGauges() throws Exception {
        registry.register("ec2--foo", new Gauge() {
            public Object getValue() {
                return 42;
            }
        });

        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        Measures measures = captor.getValue();
        assertThat(measures.getMeasures()).hasSize(1);
        assertThat(measures.getMeasures().get(0)).isEqualTo(
                new GaugeMeasure("foo", 42).setSource("ec2"));
    }

    @Test
    public void testUsesSourceRegexForMeters() throws Exception {
        Meter meter = new Meter();
        registry.register("ec2--foo", meter);
        meter.mark();

        LibratoReporter reporter = new LibratoReporter(atts);
        report(reporter);
        Measures measures = captor.getValue();
        assertThat(measures.getMeasures()).hasSize(5);
        for (IMeasure measure : measures.getMeasures()) {
            Map<String, Object> map = measure.toMap();
            String name = map.get("name").toString();
            if (name.endsWith(".count")) {
                assertThat(name).isEqualTo("foo.count");
                assertThat(map.get("source")).isEqualTo("ec2");
                assertThat(map.get("value")).isEqualTo(1.0);
                return;
            }
        }
        Assertions.fail("Did not find the right metric");
    }
}
