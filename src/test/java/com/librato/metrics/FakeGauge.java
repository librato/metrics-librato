package com.librato.metrics;

import com.yammer.metrics.core.Gauge;

class FakeGauge extends Gauge {
    final Object value;

    FakeGauge(Object value) {
        this.value = value;
    }

    @Override
    public Object value() {
        return value;
    }
}
