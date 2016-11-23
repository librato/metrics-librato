package com.librato.metrics;


import java.util.EnumSet;
import java.util.Set;

/**
 * Configures how to report "expanded" metrics derived from meters and histograms (e.g. percentiles,
 * rates, etc). Default is to report everything.
 *
 * @see ExpandedMetric
 */
public class MetricExpansionConfig {
    public static MetricExpansionConfig ALL = new MetricExpansionConfig(EnumSet.allOf(ExpandedMetric.class));
    private final Set<ExpandedMetric> enabled;

    public MetricExpansionConfig(Set<ExpandedMetric> enabled) {
        this.enabled = EnumSet.copyOf(enabled);
    }

    public boolean isSet(ExpandedMetric metric) {
        return enabled.contains(metric);
    }
}


