package com.librato.metrics;
//
//import com.librato.metrics.LibratoReporter.ExpandedMetric;
//import com.librato.metrics.LibratoReporter.MetricExpansionConfig;
//import com.yammer.metrics.core.*;
//import com.yammer.metrics.stats.Snapshot;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.TimeUnit;
//
//import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;
//
//
///**
// * a LibratoBatch that understands Metrics-specific types
// */
public class MetricsLibratoBatch   {
//    private static final Logger LOG = LoggerFactory.getLogger(MetricsLibratoBatch.class);
//    private final MetricExpansionConfig expansionConfig;
//    private final String prefix;
//    private final String prefixDelimiter;
//    private final DeltaTracker deltaTracker;
//
//    /**
//     * a string used to identify the library
//     */
//    private static final String AGENT_IDENTIFIER;
//
//    static {
//        final String version = Versions.getVersion("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties", LibratoReporter.class);
//        final String codaVersion = Versions.getVersion("META-INF/maven/com.yammer.metrics/metrics-core/pom.properties", MetricsRegistry.class);
//        AGENT_IDENTIFIER = String.format("metrics-librato/%s metrics/%s", version, codaVersion);
//    }
//
//    /**
//     * Public constructor.
//     */
//    public MetricsLibratoBatch(int postBatchSize,
//                               Sanitizer sanitizer,
//                               long timeout,
//                               TimeUnit timeoutUnit,
//                               MetricExpansionConfig expansionConfig,
//                               HttpPoster httpPoster,
//                               String prefix,
//                               String delimiter,
//                               DeltaTracker deltaTracker) {
//        super(postBatchSize, sanitizer, timeout, timeoutUnit, AGENT_IDENTIFIER, httpPoster);
//        this.expansionConfig = Preconditions.checkNotNull(expansionConfig);
//        this.prefix = LibratoUtil.checkPrefix(prefix);
//        this.prefixDelimiter = delimiter;
//        this.deltaTracker = deltaTracker;
//    }
//
//    public void post(String source, long epoch) {
//        LOG.debug("Posting measurements");
//        super.post(source, epoch);
//    }
//
//    @Override
//    public void addCounterMeasurement(String name, Long value) {
//        super.addCounterMeasurement(addPrefix(name), value);
//    }
//
//    @Override
//    public void addGaugeMeasurement(String name, Number value) {
//        super.addGaugeMeasurement(addPrefix(name), value);
//    }
//
//    // begin direct support for Coda Metrics
//
//    public void addGauge(String name, Gauge gauge) {
//        final Object value = gauge.value();
//        if (value instanceof Number) {
//            final Number number = (Number)value;
//            if (isANumber(number)) {
//                addGaugeMeasurement(name, number);
//            }
//        }
//    }
//
//    public void addCounter(String name, Counter counter) {
//        addGaugeMeasurement(name, counter.count());
//    }
//
//    public void addHistogram(String name, Histogram histogram) {
//        final Long countDelta = deltaTracker.getDelta(name, histogram.count());
//        maybeAdd(COUNT, name, countDelta);
//        addSummarizable(name, histogram);
//        addSampling(name, histogram);
//    }
//
//    public void addMetered(String name, Metered meter) {
//        final Long deltaCount = deltaTracker.getDelta(name, meter.count());
//        maybeAdd(COUNT, name, deltaCount);
//        maybeAdd(RATE_MEAN, name, meter.meanRate());
//        maybeAdd(RATE_1_MINUTE, name, meter.oneMinuteRate());
//        maybeAdd(RATE_5_MINUTE, name, meter.fiveMinuteRate());
//        maybeAdd(RATE_15_MINUTE, name, meter.fifteenMinuteRate());
//    }
//
//    public void addTimer(String name, Timer timer) {
//        addMetered(name, timer);
//        addSummarizable(name, timer);
//        addSampling(name, timer);
//    }
//
//    private void addSummarizable(String name, Summarizable summarizable) {
//        // TODO: add sum_squares if/when Summarizable exposes it
//        final double countCalculation = summarizable.sum() / summarizable.mean();
//        Long countValue = null;
//        if (isANumber(countCalculation)) {
//            countValue = Math.round(countCalculation);
//        }
//        // no need to publish these additional values if they are zero, plus the API will puke
//        if (countValue != null && countValue > 0) {
//            addMeasurement(new MultiSampleGaugeMeasurement(
//                    name,
//                    countValue,
//                    summarizable.sum(),
//                    summarizable.max(),
//                    summarizable.min(),
//                    null
//            ));
//        }
//    }
//
//    public void addSampling(String name, Sampling sampling) {
//        final Snapshot snapshot = sampling.getSnapshot();
//        maybeAdd(MEDIAN, name, snapshot.getMedian());
//        maybeAdd(PCT_75, name, snapshot.get75thPercentile());
//        maybeAdd(PCT_95, name, snapshot.get95thPercentile());
//        maybeAdd(PCT_98, name, snapshot.get98thPercentile());
//        maybeAdd(PCT_99, name, snapshot.get99thPercentile());
//        maybeAdd(PCT_999, name, snapshot.get999thPercentile());
//    }
//
//    private void maybeAdd(ExpandedMetric metric, String name, Number reading) {
//        if (expansionConfig.isSet(metric)) {
//            addGaugeMeasurement(metric.buildMetricName(name), reading);
//        }
//    }
//
//    private String addPrefix(String metricName) {
//        if (prefix == null || prefix.length() == 0) {
//            return metricName;
//        }
//        return prefix + prefixDelimiter + metricName;
//    }
//
//    /**
//     * Ensures that a number's value is an actual number
//     *
//     * @param number the number
//     * @return true if the number is not NaN or infinite, false otherwise
//     */
//    private boolean isANumber(Number number) {
//        final double doubleValue = number.doubleValue();
//        return !(Double.isNaN(doubleValue) || Double.isInfinite(doubleValue));
//    }
//
}
