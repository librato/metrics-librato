package com.librato.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Helps convert counter measurements to gauge measurements. It does this
 * by tracking the last value of a counter and converting the difference
 * of successive measurements.
 */
public class CounterGaugeConverter {
    private static final Logger LOG = LoggerFactory.getLogger(CounterGaugeConverter.class);
    private final ConcurrentMap<String, Long> lookup = new ConcurrentHashMap<String, Long>();

    /**
     * Gets the converted gauge value for the specified count.  If this is a new count
     * that has not been seen before, null will be returned to specify that the difference
     * is not yet known and should not be used.
     *
     * @param name  the name of the counter
     * @param count the counter value
     * @return the gauge value, null if the counter has not been seen before
     */
    public Long getGaugeValue(String name, long count) {
        final Long previous = lookup.put(name, count);
        if (previous == null) {
            // this is the first time we have seen this count
            return null;
        }
        if (count < previous) {
            LOG.error("Saw a non-monotonically inreasing value for counter {}", name);
            return null;
        }
        return count - previous;
    }
}
