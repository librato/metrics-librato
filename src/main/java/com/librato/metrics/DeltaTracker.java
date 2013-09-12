package com.librato.metrics;

import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
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
                lookup.put(name, ((Metered) metric).count());
            }
            if (metric instanceof Histogram) {
                lookup.put(name, ((Histogram) metric).count());
            }
        }
    }

    /**
     * Calculates the delta.  If this is a new value that has not been seen before, zero will be assumed to be the
     * initial value.
     *
     * @param name  the name of the counter
     * @param count the counter value
     * @return the delta
     */
    public Long getDelta(String name, long count) {
        Long previous = lookup.put(name, count);
        if (previous == null) {
            // this is the first time we have seen this count
            previous = 0L;
        }
        if (count < previous) {
            LOG.error("Saw a non-monotonically inreasing value for metric {}", name);
            return 0L;
        }
        return count - previous;
    }
}
