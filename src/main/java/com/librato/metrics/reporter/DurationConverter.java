package com.librato.metrics.reporter;

public interface DurationConverter {
    double convertMetricDuration(double duration);
}
