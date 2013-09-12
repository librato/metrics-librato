package com.librato.metrics;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CounterGaugeConverterTest {
    CounterGaugeConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new CounterGaugeConverter();
    }

    @Test
    public void testAssumesInitialValueOfZero() throws Exception {
        assertThat(converter.getGaugeValue("foo", 5), is(5L));
    }

    @Test
    public void testIncrementsReported() throws Exception {
        converter.getGaugeValue("foo", 1);
        assertThat(converter.getGaugeValue("foo", 2), equalTo(1L));
        assertThat(converter.getGaugeValue("foo", 3), equalTo(1L));
        assertThat(converter.getGaugeValue("foo", 4), equalTo(1L));
    }

    @Test
    public void testReportsZeroWhenNoChanges() throws Exception {
        converter.getGaugeValue("foo", 1);
        assertThat(converter.getGaugeValue("foo", 1), equalTo(0L));
        assertThat(converter.getGaugeValue("foo", 1), equalTo(0L));
    }

    @Test
    public void testDoesNotReportNegativeIncrements() throws Exception {
        assertThat(converter.getGaugeValue("foo", 1), is(1L));
        assertThat(converter.getGaugeValue("foo", 0), is(0L));
        assertThat(converter.getGaugeValue("foo", 5), equalTo(5L));
    }

    @Test
    public void testTracksMultipleCounters() throws Exception {
        converter.getGaugeValue("foo", 1);
        assertThat(converter.getGaugeValue("foo", 2), equalTo(1L));
        converter.getGaugeValue("bar", 5);
        assertThat(converter.getGaugeValue("bar", 10), equalTo(5L));
    }
}
