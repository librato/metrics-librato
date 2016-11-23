package com.librato.metrics;

public enum ExpandedMetric {
    // sampling
    MEDIAN("median"),
    PCT_75("75th"),
    PCT_95("95th"),
    PCT_98("98th"),
    PCT_99("99th"),
    PCT_999("999th"),
    // metered
    COUNT("count"),
    RATE_MEAN("meanRate"),
    RATE_1_MINUTE("1MinuteRate"),
    RATE_5_MINUTE("5MinuteRate"),
    RATE_15_MINUTE("15MinuteRate");

    private final String displayName;

    public String buildMetricName(String metric) {
        return metric + "." + displayName;
    }

    private ExpandedMetric(String displayName) {
        this.displayName = displayName;
    }
}


