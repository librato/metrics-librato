package com.librato.metrics;

import com.codahale.metrics.*;
import com.librato.metrics.LibratoReporter.ExpandedMetric;
import com.librato.metrics.LibratoReporter.MetricExpansionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;


/**
 * a LibratoBatch that understands Metrics-specific types
 */
public class MetricsLibratoBatch extends LibratoBatch {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsLibratoBatch.class);
    private final MetricExpansionConfig expansionConfig;
    private final String prefix;
    private final String prefixDelimiter;
    private final DeltaTracker deltaTracker;

    /**
     * a string used to identify the library
     */
    private static final String AGENT_IDENTIFIER = String.format(
            "metrics-librato/%s metrics/%s",
            Versions.getVersion("META-INF/maven/com.codahale.metrics/metrics-core/pom.properties", Metric.class),
            Versions.getVersion("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties", LibratoReporter.class));

    /**
     * Public constructor.
     */
    public MetricsLibratoBatch(int postBatchSize,
                               Sanitizer sanitizer,
                               long timeout,
                               TimeUnit timeoutUnit,
                               MetricExpansionConfig expansionConfig,
                               HttpPoster httpPoster,
                               String prefix,
                               String prefixDelimiter,
                               DeltaTracker deltaTracker) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
        this.prefix = checkPrefix(prefix);
        this.prefixDelimiter = prefixDelimiter;
        this.deltaTracker = deltaTracker;
    }

    public void post(String source, long epoch) {
        LOG.debug("Posting measurements");
        super.post(source, epoch);
    }

    @Override
    public void addCounterMeasurement(String name, Long value) {
        super.addCounterMeasurement(addPrefix(name), value);
    }

    @Override
    public void addGaugeMeasurement(String name, Number value) {
        super.addGaugeMeasurement(addPrefix(name), value);
    }

    // begin direct support for Coda Metrics

    public void addGauge(String name, Gauge gauge) {
        final Object value = gauge.getValue();
        if (value instanceof Number) {
            final Number number = (Number)value;
            if (isANumber(number)) {
                addGaugeMeasurement(name, number);
            }
        }
    }

    public void addCounter(String name, Counter counter) {
        addGaugeMeasurement(name, counter.getCount());
    }

    public void addHistogram(String name, Histogram histogram) {
        final Long countDelta = deltaTracker.getDelta(name, histogram.getCount());
        maybeAdd(COUNT, name, countDelta);
        addSampling(name, histogram);
    }

    public void addMeter(String name, Metered meter) {
        final Long deltaCount = deltaTracker.getDelta(name, meter.getCount());
        maybeAdd(COUNT, name, deltaCount);
        maybeAdd(RATE_MEAN, name, meter.getMeanRate());
        maybeAdd(RATE_1_MINUTE, name, meter.getOneMinuteRate());
        maybeAdd(RATE_5_MINUTE, name, meter.getFiveMinuteRate());
        maybeAdd(RATE_15_MINUTE, name, meter.getFifteenMinuteRate());
    }

    public void addTimer(String name, Timer timer) {
        addMeter(name, timer);
        addSampling(name, timer);
    }

    public void addSampling(String name, Sampling sampling) {
        final Snapshot snapshot = sampling.getSnapshot();
        maybeAdd(MEDIAN, name, snapshot.getMedian());
        maybeAdd(PCT_75, name, snapshot.get75thPercentile());
        maybeAdd(PCT_95, name, snapshot.get95thPercentile());
        maybeAdd(PCT_98, name, snapshot.get98thPercentile());
        maybeAdd(PCT_99, name, snapshot.get99thPercentile());
        maybeAdd(PCT_999, name, snapshot.get999thPercentile());

        final double sum = snapshot.size() * snapshot.getMean();
        final long size = (long) snapshot.size();
        if (size > 0) {
            addMeasurement(
                    new MultiSampleGaugeMeasurement(
                            addPrefix(name),
                            size,
                            sum,
                            snapshot.getMax(),
                            snapshot.getMin(),
                            null));
        }
    }

    private void maybeAdd(ExpandedMetric metric, String name, Number reading) {
        if (expansionConfig.isSet(metric)) {
            addGaugeMeasurement(metric.buildMetricName(name), reading);
        }
    }

    private String addPrefix(String metricName) {
        if (prefix == null || prefix.length() == 0) {
            return metricName;
        }
        return prefix + prefixDelimiter + metricName;
    }

    private static String checkPrefix(String prefix) {
        if ("".equals(prefix)) {
            throw new IllegalArgumentException("Prefix may either be null or a non-empty string");
        }
        return prefix;
    }

    /**
     * Determine if a number's double value is a valid number
     *
     * @param number the number
     * @return true if it is neither NaN not infinity
     */
    private boolean isANumber(Number number) {
        final double doubleValue = number.doubleValue();
        return !(Double.isNaN(doubleValue) || Double.isInfinite(doubleValue));
    }

}
