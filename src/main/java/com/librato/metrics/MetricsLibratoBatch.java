package com.librato.metrics;

import com.librato.metrics.LibratoBatch;
import com.librato.metrics.MultiSampleGaugeMeasurement;
import com.librato.metrics.SingleValueGaugeMeasurement;
import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;

import java.util.concurrent.TimeUnit;

/**
 * User: mihasya
 * Date: 6/17/12
 * Time: 10:57 PM
 * a LibratoBatch that understand Metrics-specific types
 */
public class MetricsLibratoBatch extends LibratoBatch {
    public MetricsLibratoBatch(int postBatchSize, long timeout, TimeUnit timeoutUnit) {
        super(postBatchSize, timeout, timeoutUnit);
    }

    public void addGauge(String name, Gauge gauge) {
        addGaugeMeasurement(name, (Number) gauge.value());
    }

    public void addSummarizable(String name, Summarizable summarizable) {
        // TODO: add sum_squares if/when Summarizble exposes it
        addMeasurement(new MultiSampleGaugeMeasurement(name, summarizable.max(), summarizable.min(), summarizable.sum() / summarizable.mean(), summarizable.sum(), null));
    }

    public void addSampling(String name, Sampling sampling) {
        Snapshot snapshot = sampling.getSnapshot();
        addMeasurement(new SingleValueGaugeMeasurement(name+".median", snapshot.getMedian()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".75th", snapshot.get75thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".95th", snapshot.get95thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".98th", snapshot.get98thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".99th", snapshot.get99thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".999th", snapshot.get999thPercentile()));
    }

    public void addMetered(String name, Metered meter) {
        addMeasurement(new SingleValueGaugeMeasurement(name+".count", meter.count()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".meanRate", meter.meanRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".1MinuteRate", meter.oneMinuteRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".5MinuteRate", meter.fiveMinuteRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".15MinuteRate", meter.fifteenMinuteRate()));
    }
}
