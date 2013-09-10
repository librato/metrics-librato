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
    private final String prefix;

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
                               HttpPoster httpPoster,
                               String prefix) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
        this.addsMeasurements = this;
        this.prefix = LibratoUtil.checkPrefix(prefix);
    }

    /**
     * Protected constructor. Uses the {@link AddsMeasurements} instance to delegate the reponsibility of
     * adding a measurement to the batch.
     * <p/>
     * Visible for testing
     */
    MetricsLibratoBatch(int postBatchSize,
                        Sanitizer sanitizer,
                        long timeout,
                        TimeUnit timeoutUnit,
                        MetricExpansionConfig expansionConfig,
                        HttpPoster httpPoster,
                        AddsMeasurements addsMeasurements,
                        String prefix) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
        this.addsMeasurements = Preconditions.checkNotNull(addsMeasurements);
        this.prefix = LibratoUtil.checkPrefix(prefix);
    }

    /**
     * Adds the specified gauge. It will only add the gauge if the gauge value if it is numeric
     * and is an actual number.
     *
     * @param name the name of the metric
     * @param gauge the gauge
     */
    public void addGauge(String name, Gauge gauge) {
        final Object value = gauge.value();
        if (value instanceof Number) {
            final Number number = (Number)value;
            if (isANumber(number)) {
                addsMeasurements.addMeasurement(new SingleValueGaugeMeasurement(addPrefix(name), number));
            }
        }
    }

    public void addSummarizable(String name, Summarizable summarizable) {
        // TODO: add sum_squares if/when Summarizable exposes it
        final double countCalculation = summarizable.sum() / summarizable.mean();
        Long countValue = null;
        if (isANumber(countCalculation)) {
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
            final String metricName = addPrefix(metric.buildMetricName(name));
            addsMeasurements.addMeasurement(new SingleValueGaugeMeasurement(metricName, reading));
        }
    }

    private String addPrefix(String metricName) {
        if (prefix == null || prefix.length() == 0) {
            return metricName;
        }
        return prefix + "." + metricName;
    }

    /**
     * Ensures that a number's value is an actual number
     *
     * @param number the number
     * @return true if the number is not NaN or infinite, false otherwise
     */
    private boolean isANumber(Number number) {
        final double doubleValue = number.doubleValue();
        return !(Double.isNaN(doubleValue) || Double.isInfinite(doubleValue));
    }


}
