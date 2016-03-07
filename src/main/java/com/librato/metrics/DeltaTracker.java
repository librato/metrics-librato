package com.librato.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks the last named value.
 */
public class DeltaTracker {
    private static final Logger LOG = LoggerFactory.getLogger(DeltaTracker.class);
    private final ConcurrentMap<String, Long> lookup = new ConcurrentHashMap<String, Long>();

    public static interface MetricSupplier {
        Map<String, Metric> getMetrics();
    }

    public DeltaTracker() {
        this(new MetricSupplier() {
            public Map<String, Metric> getMetrics() {
                return new HashMap<String, Metric>();
            }
        });
    }

    public DeltaTracker(MetricSupplier supplier) {
        for (Map.Entry<String, Metric> entry : supplier.getMetrics().entrySet()) {
            final String name = entry.getKey();
            final Metric metric = entry.getValue();
            if (metric instanceof Metered) {
                lookup.put(name, ((Metered) metric).getCount());
            }
            if (metric instanceof Histogram) {
                lookup.put(name, ((Histogram) metric).getCount());
            }
        }
    }

    /**
     * Gets the delta without updating the internal data store
     */
    public Long peekDelta(String name, long count) {
        Long previous = lookup.get(name);
        return calculateDelta(name, previous, count);
    }

    /**
     * Calculates the delta.  If this is a new value that has not been seen before, zero will be assumed to be the
     * initial value. Updates the internal map with the supplied count.
     *
     * @param name  the name of the counter
     * @param count the counter value
     * @return the delta
     */
    public Long getDelta(String name, long count) {
        Long previous = lookup.put(name, count);
        return calculateDelta(name, previous, count);
    }

    private Long calculateDelta(String name, Long previous, long count) {
        if (previous == null) {
            previous = 0L;
        } else if (count < previous) {
            LOG.debug("Saw a non-monotonically increasing value for metric {}", name);
            previous = 0L;
        }
        return count - previous;
    }
}
