package com.librato.metrics;

import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricProcessor;

import java.util.concurrent.TimeUnit;

class FakeMetered implements Metered {
    private final long count;
    private final double meanRate;
    private final double oneMinuteRate;
    private final double fiveMinuteRate;
    private final double fifteenMinuteRate;

    FakeMetered(long count, double meanRate , double oneMinuteRate, double fiveMinuteRate, double fifteenMinuteRate) {
        this.count = count;
        this.fifteenMinuteRate = fifteenMinuteRate;
        this.fiveMinuteRate = fiveMinuteRate;
        this.meanRate = meanRate;
        this.oneMinuteRate = oneMinuteRate;
    }

    FakeMetered() {
        this(0, 0, 0, 0, 0);
    }

    public TimeUnit rateUnit() {
        return TimeUnit.DAYS;
    }

    public String eventType() {
        return "no event type";
    }

    public long count() {
        return count;
    }

    public double fifteenMinuteRate() {
        return fifteenMinuteRate;
    }

    public double fiveMinuteRate() {
        return fiveMinuteRate;
    }

    public double meanRate() {
        return meanRate;
    }

    public double oneMinuteRate() {
        return oneMinuteRate;
    }

    public <T> void processWith(MetricProcessor<T> processor, MetricName name, T context) throws Exception {
    }
}
