package com.librato.metrics.reporter;

import com.codahale.metrics.*;
import com.librato.metrics.client.*;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LibratoReporterTest {
    SortedMap<String, Gauge> gauges = new TreeMap<String, Gauge>();
    SortedMap<String, Counter> counters = new TreeMap<String, Counter>();
    SortedMap<String, Histogram> histos = new TreeMap<String, Histogram>();
    SortedMap<String, Meter> meters = new TreeMap<String, Meter>();
    SortedMap<String, Timer> timers = new TreeMap<String, Timer>();
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

    @Test
    public void testCounter() throws Exception {
        Counter counter = new Counter();
        counters.put("foo", counter);
        counter.inc();
        LibratoReporter reporter = new LibratoReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo", 1));
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
        timers.put("foo", timer);
        LibratoReporter reporter = new LibratoReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
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
    public void testMeter() throws Exception {
        Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2d);
        when(meter.getOneMinuteRate()).thenReturn(3d);
        when(meter.getFiveMinuteRate()).thenReturn(4d);
        when(meter.getFifteenMinuteRate()).thenReturn(5d);
        meters.put("foo", meter);
        LibratoReporter reporter = new LibratoReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
        HashSet<IMeasure> measures = new HashSet<IMeasure>(captor.getValue().getMeasures());
        assertThat(measures).containsOnly(
                new GaugeMeasure("foo.count", 1),
                new GaugeMeasure("foo.meanRate", meter.getMeanRate()),
                new GaugeMeasure("foo.1MinuteRate", meter.getOneMinuteRate()),
                new GaugeMeasure("foo.5MinuteRate", meter.getFiveMinuteRate()),
                new GaugeMeasure("foo.15MinuteRate", meter.getFifteenMinuteRate()));
    }

    @Test
    public void testHisto() throws Exception {
        Histogram histo = new Histogram(new UniformReservoir());
        histo.update(42);
        histos.put("foo", histo);
        LibratoReporter reporter = new LibratoReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
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
    public void testUsesSourceRegexForGauges() throws Exception {
        gauges.put("ec2--foo", new Gauge() {
            public Object getValue() {
                return 42;
            }
        });

        LibratoReporter reporter = new LibratoReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
        Measures measures = captor.getValue();
        assertThat(measures.getMeasures()).hasSize(1);
        assertThat(measures.getMeasures().get(0)).isEqualTo(
                new GaugeMeasure("foo", 42).setSource("ec2"));
    }

    @Test
    public void testUsesSourceRegexForMeters() throws Exception {
        Meter meter = new Meter();
        meters.put("ec2--foo", meter);
        meter.mark();

        LibratoReporter reporter = new LibratoReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
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
