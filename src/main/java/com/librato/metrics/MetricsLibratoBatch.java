package com.librato.metrics;

import com.librato.metrics.LibratoReporter.ExpandedMetric;
import com.librato.metrics.LibratoReporter.MetricExpansionConfig;
import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;

import java.util.concurrent.TimeUnit;

import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;


/**
 * a LibratoBatch that understands Metrics-specific types
 */
public class MetricsLibratoBatch extends LibratoBatch implements AddsMeasurements {
    private final MetricExpansionConfig expansionConfig;
    private final AddsMeasurements addsMeasurements;

    /**
     * a string used to identify the library
     */
    private static final String AGENT_IDENTIFIER;

    static {
        final String version = VersionUtil.getVersion("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties", LibratoReporter.class);
        final String codaVersion = VersionUtil.getVersion("META-INF/maven/com.yammer.metrics/metrics-core/pom.properties", MetricsRegistry.class);
        AGENT_IDENTIFIER = String.format("metrics-librato/%s metrics/%s", version, codaVersion);
    }

    /**
     * Public constructor.
     */
    public MetricsLibratoBatch(int postBatchSize,
                               Sanitizer sanitizer,
                               long timeout,
                               TimeUnit timeoutUnit,
                               MetricExpansionConfig expansionConfig,
                               HttpPoster httpPoster) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
        this.addsMeasurements = this;
    }

    /**
     * Protected constructor. Uses the {@link AddsMeasurements} instance to delegate the reponsibility of
     * adding a measurement to the batch.
     *
     * Visible for testing
     */
    MetricsLibratoBatch(int postBatchSize,
                        Sanitizer sanitizer,
                        long timeout,
                        TimeUnit timeoutUnit,
                        MetricExpansionConfig expansionConfig,
                        HttpPoster httpPoster,
                        AddsMeasurements addsMeasurements) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
        this.addsMeasurements = Preconditions.checkNotNull(addsMeasurements);
    }

    public void addGauge(String name, Gauge gauge) {
        addGaugeMeasurement(name, (Number) gauge.value());
    }

    public void addSummarizable(String name, Summarizable summarizable) {
        // TODO: add sum_squares if/when Summarizable exposes it
        final double countCalculation = summarizable.sum() / summarizable.mean();
        Long countValue = null;
        if (!(Double.isNaN(countCalculation) || Double.isInfinite(countCalculation))) {
            countValue = Math.round(countCalculation);
        }
        // no need to publish these additional values if they are zero, plus the API will puke
        if (countValue != null && countValue > 0) {
            addMeasurement(new MultiSampleGaugeMeasurement(
                    name,
                    countValue,
                    summarizable.sum(),
                    summarizable.max(),
                    summarizable.min(),
                    null
            ));
        }
    }

    public void addSampling(String name, Sampling sampling) {
        final Snapshot snapshot = sampling.getSnapshot();
        maybeAdd(MEDIAN, name, snapshot.getMedian());
        maybeAdd(PCT_75, name, snapshot.get75thPercentile());
        maybeAdd(PCT_95, name, snapshot.get95thPercentile());
        maybeAdd(PCT_98, name, snapshot.get98thPercentile());
        maybeAdd(PCT_99, name, snapshot.get99thPercentile());
        maybeAdd(PCT_999, name, snapshot.get999thPercentile());
    }

    public void addMetered(String name, Metered meter) {
        maybeAdd(COUNT, name, meter.count());
        maybeAdd(RATE_MEAN, name, meter.meanRate());
        maybeAdd(RATE_1_MINUTE, name, meter.oneMinuteRate());
        maybeAdd(RATE_5_MINUTE, name, meter.fiveMinuteRate());
        maybeAdd(RATE_15_MINUTE, name, meter.fifteenMinuteRate());
    }

    private void maybeAdd(ExpandedMetric metric, String name, Number reading) {
        if (expansionConfig.isSet(metric)) {
            final String metricName = metric.buildMetricName(name);
            addsMeasurements.addMeasurement(new SingleValueGaugeMeasurement(metricName, reading));
        }
    }
}
