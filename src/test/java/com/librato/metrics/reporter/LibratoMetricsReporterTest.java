package com.librato.metrics.reporter;

import com.codahale.metrics.*;
import com.librato.metrics.client.IMeasure;
import com.librato.metrics.client.LibratoClient;
import com.librato.metrics.client.Measures;
import com.librato.metrics.client.PostMeasuresResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LibratoMetricsReporterTest {
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

        LibratoMetricsReporter reporter = new LibratoMetricsReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
        Measures measures = captor.getValue();
        assertThat(measures.getMeasures().size(), equalTo(1));
        IMeasure measure = measures.getMeasures().get(0);
        Map<String, Object> map = measure.toMap();
        assertThat(map.get("name").toString(), equalTo("foo"));
        assertThat(map.get("source").toString(), equalTo("ec2"));
        assertThat(map.get("value"), equalTo((Object)42d));
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

        LibratoMetricsReporter reporter = new LibratoMetricsReporter(atts);
        reporter.report(gauges, counters, histos, meters, timers);
        Measures measures = captor.getValue();
        assertThat(measures.getMeasures().size(), equalTo(5));
        for (IMeasure measure : measures.getMeasures()) {
            Map<String, Object> map = measure.toMap();
            String name = map.get("name").toString();
            if (name.endsWith(".count")) {
                assertThat(name, equalTo("foo.count"));
                assertThat(map.get("source").toString(), equalTo("ec2"));
                assertThat(map.get("value"), equalTo((Object) 1.0));
                return;
            }
        }
        fail("Did not find the right metric");
    }
}
