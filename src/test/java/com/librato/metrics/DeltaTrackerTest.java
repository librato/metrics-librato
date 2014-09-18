package com.librato.metrics;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DeltaTrackerTest {
    DeltaTracker converter;

    @Before
    public void setUp() throws Exception {
        converter = new DeltaTracker();
    }

    @Test
    public void testAssumesInitialValueOfZero() throws Exception {
        assertThat(converter.getDelta("foo", 5), is(5L));
    }

    @Test
    public void testIncrementsReported() throws Exception {
        converter.getDelta("foo", 1);
        assertThat(converter.getDelta("foo", 2), equalTo(1L));
        assertThat(converter.getDelta("foo", 3), equalTo(1L));
        assertThat(converter.getDelta("foo", 4), equalTo(1L));
    }

    @Test
    public void testReportsZeroWhenNoChanges() throws Exception {
        converter.getDelta("foo", 1);
        assertThat(converter.getDelta("foo", 1), equalTo(0L));
        assertThat(converter.getDelta("foo", 1), equalTo(0L));
    }

    @Test
    public void testDoesNotReportNegativeIncrements() throws Exception {
        assertThat(converter.getDelta("foo", 1), is(1L));
        assertThat(converter.getDelta("foo", 0), is(0L));
        assertThat(converter.getDelta("foo", 5), equalTo(5L));
    }

    @Test
    public void testTracksMultipleCounters() throws Exception {
        converter.getDelta("foo", 1);
        assertThat(converter.getDelta("foo", 2), equalTo(1L));
        converter.getDelta("bar", 5);
        assertThat(converter.getDelta("bar", 10), equalTo(5L));
    }

    @Test
    public void testPeekWorks() throws Exception {
        converter.getDelta("foo", 1); // sets the value of 1 for the metric count
        assertThat(converter.peekDelta("foo", 1), equalTo(0L));
        assertThat(converter.peekDelta("foo", 1), equalTo(0L));
        assertThat(converter.peekDelta("foo", 2), equalTo(1L));
        assertThat(converter.peekDelta("foo", 2), equalTo(1L));
        // now update it
        assertThat(converter.getDelta("foo", 2), equalTo(1L));
        assertThat(converter.getDelta("foo", 2), equalTo(0L));
        // now peek it
        assertThat(converter.peekDelta("foo", 2), equalTo(0L));
        assertThat(converter.peekDelta("foo", 2), equalTo(0L));
        assertThat(converter.peekDelta("foo", 3), equalTo(1L));
        assertThat(converter.peekDelta("foo", 3), equalTo(1L));
    }
}
