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
        System.out.println("measures = " + measures);
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
