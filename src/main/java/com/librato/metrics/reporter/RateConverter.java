package com.librato.metrics.reporter;

public interface RateConverter {
    double convertRate(double rate);
}
