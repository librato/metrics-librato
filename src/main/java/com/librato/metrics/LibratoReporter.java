package com.librato.metrics;

import com.librato.metrics.client.*;
import com.yammer.metrics.core.*;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A reporter for publishing metrics to <a href="http://metrics.librato.com/">Librato Metrics</a>
 */
public class LibratoReporter extends AbstractPollingReporter implements MetricProcessor<Measures> {
    private static final Logger LOG = LoggerFactory.getLogger(LibratoReporter.class);
    private final LibratoClient client;
    private final DeltaTracker deltaTracker;
    private final Pattern sourceRegex;
    private final String prefix;
    private final String prefixDelimiter;
    private final MetricExpansionConfig expansionConfig;
    private final boolean deleteIdleStats;
    private final boolean omitComplexGauges;
    private final String source;
    private final List<Tag> tags;
    private final boolean enableLegacy;
    private final boolean enableTagging;
    private final RateConverter rateConverter;
    private final DurationConverter durationConverter;
    private final MetricPredicate predicate;
    private final ScheduledExecutorService executor;
    private final Sanitizer sanitizer;

    /*
     * Constructor. Should be called from the builder.
     */
    LibratoReporter(ReporterAttributes atts) {
        super(atts.registry, atts.reporterName);
        this.client = atts.libratoClientFactory.build(atts);
        this.sanitizer = atts.customSanitizer;
        this.deltaTracker = new DeltaTracker(new DeltaMetricSupplier(atts.registry, atts.predicate));
        this.predicate = atts.predicate;
        this.sourceRegex = atts.sourceRegex;
        this.prefix = LibratoUtil.checkPrefix(atts.prefix);
        this.prefixDelimiter = atts.prefixDelimiter;
        this.executor = atts.registry.newScheduledThreadPool(1, atts.reporterName);
        this.expansionConfig = atts.expansionConfig;
        this.deleteIdleStats = atts.deleteIdleStats;
        this.omitComplexGauges = atts.omitComplexGauges;
        this.source = atts.source;
        this.tags = sanitize(atts.tags);
        this.enableLegacy = atts.enableLegacy;
        this.enableTagging = atts.enableTagging;
        this.rateConverter = atts.rateConverter != null ? atts.rateConverter : (RateConverter) this;
        this.durationConverter = atts.durationConverter != null ? atts.durationConverter : (DurationConverter) this;
    }

    public void processMeter(MetricName metricName, Metered metered, Measures measures) throws Exception {
        if (skipMetric(metricName.getName(), metered.count())) {
            return;
        }
        addMeter(measures, metricName.getName(), metered);
    }

    public void processCounter(MetricName metricName, Counter counter, Measures measures) throws Exception {
        long count = counter.count();
        SourceInformation info = SourceInformation.from(sourceRegex, metricName.getName());
        addGauge(measures, addPrefix(info.name), count, info.source);
    }

    public void processHistogram(MetricName metricName, Histogram histogram, Measures measures) throws Exception {
        if (skipMetric(metricName.getName(), histogram.count())) {
            return;
        }
        Long countDelta = deltaTracker.getDelta(metricName.getName(), histogram.count());
        maybeAdd(measures, ExpandedMetric.COUNT, metricName.getName(), countDelta);
        final boolean convertDurations = false;
        addSampling(measures, metricName.getName(), histogram, convertDurations);
    }

    public void processTimer(MetricName metricName, Timer timer, Measures measures) throws Exception {
        if (skipMetric(metricName.getName(), timer.count())) {
            return;
        }

        addMeter(measures, metricName.getName(), timer);
        final boolean convertDurations = true;
        addSampling(measures, metricName.getName(), timer, convertDurations);
    }

    public void processGauge(MetricName metricName, Gauge<?> gauge, Measures measures) throws Exception {
        Number number = Numbers.getNumberFrom(gauge.value());
        if (number != null) {
            SourceInformation info = SourceInformation.from(sourceRegex, metricName.getName());
            addGauge(measures, addPrefix(info.name), number, info.source);
        }
    }

    private void addGauge(Measures measures, String metricName, Number number, String source) {
        GaugeMeasure gauge = new GaugeMeasure(metricName, number.doubleValue()).setSource(source);
        addGauge(measures, gauge);
    }

    private void addGauge(Measures measures, GaugeMeasure gauge) {
        if (this.enableLegacy) {
            measures.add(gauge);
        }
        if (this.enableTagging) {
            TaggedMeasure taggedMeasure = new TaggedMeasure(gauge);
            for (Tag tag : tags) {
                taggedMeasure.addTag(tag);
            }
            String source = gauge.getSource();
            if (source != null) {
                taggedMeasure.addTag(sanitize(new Tag("source", source)));
            }
            measures.add(taggedMeasure);
        }
    }

    private void addMeter(Measures measures, String metricName, Metered meter) {
        Long countDelta = deltaTracker.getDelta(metricName, meter.count());
        maybeAdd(measures, ExpandedMetric.COUNT, metricName, countDelta);
        maybeAdd(measures, ExpandedMetric.RATE_MEAN, metricName, doConvertRate(meter.meanRate()));
        maybeAdd(measures, ExpandedMetric.RATE_1_MINUTE, metricName, doConvertRate(meter.oneMinuteRate()));
        maybeAdd(measures, ExpandedMetric.RATE_5_MINUTE, metricName, doConvertRate(meter.fiveMinuteRate()));
        maybeAdd(measures, ExpandedMetric.RATE_15_MINUTE, metricName, doConvertRate(meter.fifteenMinuteRate()));
    }

    private boolean skipMetric(String name, long counting) {
        return deleteIdleStats() && deltaTracker.peekDelta(name, counting) == 0;
    }

    private boolean deleteIdleStats() {
        return deleteIdleStats;
    }

    private void maybeAdd(Measures measures, ExpandedMetric metric, String name, Number reading) {
        if (expansionConfig.isSet(metric)) {
            String metricName = metric.buildMetricName(name);
            if (!Numbers.isANumber(reading)) {
                return;
            }
            SourceInformation info = SourceInformation.from(sourceRegex, metricName);
            addGauge(measures, addPrefix(info.name), reading, info.source);
        }
    }

    private void addSampling(Measures measures, String name, Sampling sampling, boolean convert) {
        final Snapshot snapshot = sampling.getSnapshot();
        maybeAdd(measures, ExpandedMetric.MEDIAN, name, doConvertDuration(snapshot.getMedian(), convert));
        maybeAdd(measures, ExpandedMetric.PCT_75, name, doConvertDuration(snapshot.get75thPercentile(), convert));
        maybeAdd(measures, ExpandedMetric.PCT_95, name, doConvertDuration(snapshot.get95thPercentile(), convert));
        maybeAdd(measures, ExpandedMetric.PCT_98, name, doConvertDuration(snapshot.get98thPercentile(), convert));
        maybeAdd(measures, ExpandedMetric.PCT_99, name, doConvertDuration(snapshot.get99thPercentile(), convert));
        maybeAdd(measures, ExpandedMetric.PCT_999, name, doConvertDuration(snapshot.get999thPercentile(), convert));

        // Yammer samplings don't support mean/min/max we can probably pull this out from getValues() but
        // for now just omitting
//        if (!omitComplexGauges) {
//            final double sum = snapshot.size() * snapshot.getMean();
//            final long count = (long) snapshot.size();
//            if (count > 0) {
//                SourceInformation info = SourceInformation.from(sourceRegex, name);
//                GaugeMeasure gauge;
//                try {
//                    gauge = new GaugeMeasure(
//                            addPrefix(info.name),
//                            doConvertDuration(sum, convert),
//                            count,
//                            doConvertDuration(snapshot.getMin(), convert),
//                            doConvertDuration(snapshot.getMax(), convert)
//                    ).setSource(info.source);
//                } catch (IllegalArgumentException e) {
//                    LOG.warn("Could not create gauge", e);
//                    return;
//                }
//                addGauge(measures, gauge);
//            }
//        }
    }

    private double doConvertRate(double rate) {
        return rateConverter.convertRate(rate);
    }

    private double doConvertDuration(double duration, boolean convert) {
        return convert ? durationConverter.convertDuration(duration) : duration;
    }

    private List<Tag> sanitize(List<Tag> tags) {
        List<Tag> result = new LinkedList<Tag>();
        for (Tag tag : tags) {
            result.add(sanitize(tag));
        }
        return result;
    }

    private String addPrefix(String metricName) {
        if (prefix == null || prefix.length() == 0) {
            return metricName;
        }
        return prefix + prefixDelimiter + metricName;
    }

    private Tag sanitize(Tag tag) {
        return new Tag(Sanitizer.LAST_PASS.apply(tag.name), Sanitizer.LAST_PASS.apply(tag.value));
    }

    /**
     * Used to supply metrics to the delta tracker on initialization. Uses the metric name conversion
     * to ensure that the correct names are supplied for the metric.
     */
    class DeltaMetricSupplier implements DeltaTracker.MetricSupplier {
        final MetricsRegistry registry;
        final MetricPredicate predicate;

        DeltaMetricSupplier(MetricsRegistry registry, MetricPredicate predicate) {
            this.registry = registry;
            this.predicate = predicate;
        }

        public Map<String, Metric> getMetrics() {
            final Map<String, Metric> map = new HashMap<String, Metric>();
            for (Map.Entry<String, SortedMap<MetricName, Metric>> entry : registry.groupedMetrics(predicate).entrySet()) {
                for (Map.Entry<MetricName, Metric> metricEntry : entry.getValue().entrySet()) {
                    final String name = getStringName(metricEntry.getKey());
                    map.put(name, metricEntry.getValue());
                }
            }
            return map;
        }
    }

    @Override
    public void run() {
        Measures measures = new Measures(source, Collections.<Tag>emptyList(), System.currentTimeMillis() / 1000);
        reportRegularMetrics(measures);
        try {
            client.postMeasures(measures);
        } catch (Exception e) {
            LOG.error("Librato post failed: ", e);
        }
    }

    /**
     * Starts the reporter polling at the given period.
     *
     * @param period the amount of time between polls
     * @param unit   the unit for {@code period}
     */
    @Override
    public void start(long period, TimeUnit unit) {
        LOG.debug("Reporter starting at fixed rate of every {} {}", period, unit);
        executor.scheduleAtFixedRate(this, period, period, unit);
    }

    protected void reportRegularMetrics(Measures measures) {
        final SortedMap<String, SortedMap<MetricName, Metric>> metrics = getMetricsRegistry().groupedMetrics(predicate);
        LOG.debug("Preparing batch of {} top level metrics", metrics.size());
        for (Map.Entry<String, SortedMap<MetricName, Metric>> entry : metrics.entrySet()) {
            for (Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
                final Metric metric = subEntry.getValue();
                if (metric != null) {
                    try {
                        metric.processWith(this, subEntry.getKey(), measures);
                    } catch (Exception e) {
                        LOG.error("Error processing regular metrics:", e);
                    }
                }
            }
        }
    }

    private String getStringName(MetricName fullName) {
        return sanitizer.apply(LibratoUtil.nameToString(fullName));
    }
}
