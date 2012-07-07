package com.yammer.metrics.librato;

import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * User: mihasya
 * Date: 7/1/12
 * Time: 5:10 PM
 * test a counter reading
 */
public class CounterMeasurementTest {
    @Test
    public void testCorrectMap() throws Exception {
        CounterMeasurement counterMeasurement = new CounterMeasurement("my.fancy.counter", 17L);
        Map<String, Number> map = counterMeasurement.toMap();

        assertEquals(1, map.size());
        assertEquals(17L, map.get("value"));
    }
}
