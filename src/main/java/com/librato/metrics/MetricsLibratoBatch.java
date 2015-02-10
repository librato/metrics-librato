package com.librato.metrics;

import com.codahale.metrics.*;
import com.librato.metrics.LibratoReporter.ExpandedMetric;
import com.librato.metrics.LibratoReporter.MetricExpansionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
    private final DurationConverter durationConverter;
    private final RateConverter rateConverter;
    private final Pattern sourceRegex;

    public static interface RateConverter {
        double convertMetricRate(double rate);
    }

    public static interface DurationConverter {
        double convertMetricDuration(double duration);
    }

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
                               DeltaTracker deltaTracker,
                               RateConverter rateConverter,
                               DurationConverter durationConverter,
                               Pattern sourceRegex) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
        this.prefix = checkPrefix(prefix);
        this.prefixDelimiter = prefixDelimiter;
        this.deltaTracker = deltaTracker;
        this.rateConverter = rateConverter;
        this.durationConverter = durationConverter;
        this.sourceRegex = sourceRegex;
    }

    public BatchResult post(String source, long epoch) {
        LOG.debug("Posting measurements");
        return super.post(source, epoch);
    }

    @Override
    public void addCounterMeasurement(String name, Long value) {
        SourceInformation info = SourceInformation.from(sourceRegex, name);
        super.addCounterMeasurement(info.source, addPrefix(info.name), value);
    }

    @Override
    public void addGaugeMeasurement(String name, Number value) {
        SourceInformation info = SourceInformation.from(sourceRegex, name);
        super.addGaugeMeasurement(info.source, addPrefix(info.name), value);
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
        final boolean convertDurations = false;
        addSampling(name, histogram, convertDurations);
    }

    public void addMeter(String name, Metered meter) {
        final Long deltaCount = deltaTracker.getDelta(name, meter.getCount());
        maybeAdd(COUNT, name, deltaCount);
        maybeAdd(RATE_MEAN, name, convertRate(meter.getMeanRate()));
        maybeAdd(RATE_1_MINUTE, name, convertRate(meter.getOneMinuteRate()));
        maybeAdd(RATE_5_MINUTE, name, convertRate(meter.getFiveMinuteRate()));
        maybeAdd(RATE_15_MINUTE, name, convertRate(meter.getFifteenMinuteRate()));
    }

    public void addTimer(String name, Timer timer) {
        addMeter(name, timer);
        final boolean convertDurations = true;
        addSampling(name, timer, convertDurations);
    }

    public void addSampling(String name, Sampling sampling, boolean convert) {
        final Snapshot snapshot = sampling.getSnapshot();
        maybeAdd(MEDIAN, name, convertDuration(snapshot.getMedian(), convert));
        maybeAdd(PCT_75, name, convertDuration(snapshot.get75thPercentile(), convert));
        maybeAdd(PCT_95, name, convertDuration(snapshot.get95thPercentile(), convert));
        maybeAdd(PCT_98, name, convertDuration(snapshot.get98thPercentile(), convert));
        maybeAdd(PCT_99, name, convertDuration(snapshot.get99thPercentile(), convert));
        maybeAdd(PCT_999, name, convertDuration(snapshot.get999thPercentile(), convert));

        final double sum = snapshot.size() * snapshot.getMean();
        final long count = (long) snapshot.size();
        if (count > 0) {
            SourceInformation info = SourceInformation.from(sourceRegex, name);
            addMeasurement(
                    MultiSampleGaugeMeasurement.builder(addPrefix(info.name))
                            .setSource(info.source)
                            .setCount(count)
                            .setSum(convertDuration(sum, convert))
                            .setMax(convertDuration(snapshot.getMax(), convert))
                            .setMin(convertDuration(snapshot.getMin(), convert))
                            .build());
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

    private double convertRate(double rate) {
        return rateConverter.convertMetricRate(rate);
    }

    private double convertDuration(double duration, boolean convert) {
        return convert ? convertDuration(duration) : duration;
    }

    private double convertDuration(double duration) {
        return durationConverter.convertMetricDuration(duration);
    }

}
