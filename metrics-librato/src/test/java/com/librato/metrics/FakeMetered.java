package com.librato.metrics;

import com.codahale.metrics.Metered;

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

    public long getCount() {
        return count;
    }

    public double getFifteenMinuteRate() {
        return fifteenMinuteRate;
    }

    public double getFiveMinuteRate() {
        return fiveMinuteRate;
    }

    public double getMeanRate() {
        return meanRate;
    }

    public double getOneMinuteRate() {
        return oneMinuteRate;
    }
}
