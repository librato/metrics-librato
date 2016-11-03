package com.librato.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to supply metrics to the delta tracker on initialization. Uses the metric name conversion
 * to ensure that the correct names are supplied for the metric.
 */
public class DeltaMetricSupplier implements DeltaTracker.MetricSupplier {
    final MetricRegistry registry;

    public DeltaMetricSupplier(MetricRegistry registry) {
        this.registry = registry;
    }

    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> map = new HashMap<String, Metric>();
        for (Map.Entry<String, Metric> entry : registry.getMetrics().entrySet()) {
            // todo: ensure the name here is what we expect
            final String name = entry.getKey();
            map.put(name, entry.getValue());
        }
        return map;
    }
}
