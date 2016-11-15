package com.librato.metrics.reporter;

public interface RateConverter {
    double convertMetricRate(double rate);
}
