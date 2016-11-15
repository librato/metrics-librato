package com.librato.metrics.reporter;

import com.codahale.metrics.Gauge;

class FakeGauge implements Gauge {
    final Object value;

    FakeGauge(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
