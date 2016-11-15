package com.librato.metrics.reporter;

import com.codahale.metrics.*;
import com.librato.metrics.client.*;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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

    @Test
    public void testUsesSourceRegexForGauges() throws Exception {
        ReporterAttributes atts = new ReporterAttributes();
        atts.sourceRegex = Pattern.compile("^(.*)--");
        atts.libratoClientFactory = new ILibratoClientFactory() {
            public LibratoClient build(ReporterAttributes atts) {
                return client;
            }
        };
        ArgumentCaptor<Measures> captor = ArgumentCaptor.forClass(Measures.class);
        when(client.postMeasures(captor.capture())).thenReturn(new PostMeasuresResult());

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
                new GaugeMeasure("name", 42).setSource("ec2"));
    }

    @Test
    public void testUsesSourceRegexForMeters() throws Exception {
        ReporterAttributes atts = new ReporterAttributes();
        atts.sourceRegex = Pattern.compile("^(.*)--");
        atts.libratoClientFactory = new ILibratoClientFactory() {
            public LibratoClient build(ReporterAttributes atts) {
                return client;
            }
        };
        ArgumentCaptor<Measures> captor = ArgumentCaptor.forClass(Measures.class);
        when(client.postMeasures(captor.capture())).thenReturn(new PostMeasuresResult());

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
