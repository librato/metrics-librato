package com.yammer.metrics.librato;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * User: mihasya
 * Date: 7/1/12
 * Time: 5:17 PM
 * Test a simple gauge reading
 */
public class SingleValueGaugeMeasurementTest {
    @Test
    public void testCorrectMap() throws Exception {
        SingleValueGaugeMeasurement single = new SingleValueGaugeMeasurement("my.fancy.gauge", 45L);
        Map<String, Number> map = single.toMap();
        assertEquals(1, map.size());
        assertEquals(45L, map.get("value"));
    }
}
