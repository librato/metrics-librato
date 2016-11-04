package com.librato.metrics.reporter;

import com.codahale.metrics.*;
import com.librato.metrics.*;
import com.librato.metrics.client.*;
import com.librato.metrics.client.PostResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.regex.Pattern;

import static com.librato.metrics.ExpandedMetric.*;

/**
 * The main class in this library
 */
public class LibratoMetricsReporter extends ScheduledReporter {
    private static final Logger log = LoggerFactory.getLogger(LibratoMetricsReporter.class);
    private final LibratoClient client;
    private final DeltaTracker deltaTracker;
    private final Pattern sourceRegex;
    private final String prefix;
    private final String prefixDelimiter;
    private final MetricExpansionConfig expansionConfig;
    private final boolean deleteIdleStats;
    private final boolean omitComplexGauges;


    public static ReporterBuilder builder(MetricRegistry registry,
                                          String email,
                                          String token) {
        return new ReporterBuilder(registry, email, token);
    }

    /*
     * Constructor. Should be called from the builder.
     */
    LibratoMetricsReporter(ReporterAttributes atts) {
        super(atts.registry,
                atts.reporterName,
                atts.metricFilter,
                atts.rateUnit,
                atts.durationUnit);
        LibratoClientBuilder builder = LibratoClient.builder(atts.email, atts.token)
                .setURI(atts.url)
                .setAgentIdentifier(Agent.AGENT_IDENTIFIER);
        if (atts.readTimeout != null) {
            builder.setReadTimeout(atts.readTimeout);
        }
        this.client = builder.build();
        this.deltaTracker = new DeltaTracker(new DeltaMetricSupplier(atts.registry));
        this.sourceRegex = atts.sourceRegex;
        this.prefix = checkPrefix(atts.prefix);
        this.prefixDelimiter = atts.prefixDelimiter;
        this.expansionConfig = atts.expansionConfig;
        this.deleteIdleStats = atts.deleteIdleStats;
        this.omitComplexGauges = atts.omitComplexGauges;
    }

    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        Measures measures = new Measures();
        addGauges(measures, gauges);
        addCounters(measures, counters);
        addHistograms(measures, histograms);
        addMeters(measures, meters);
        addTimers(measures, timers);
        // todo: async?
        try {
            PostMeasuresResult postResults = client.postMeasures(measures);
            for (PostResult result : postResults.results) {
                if (result.isError()) {
                    log.error("Failure to post to librato: " + result.toString());
                }
            }
        } catch (Exception e) {
            log.error("Failure to post to Librato", e);
        }
    }

    private void addGauges(Measures measures, SortedMap<String, Gauge> gauges) {
        for (String metricName : gauges.keySet()) {
            Gauge gauge = gauges.get(metricName);
            Number number = Numbers.getNumberFrom(gauge.getValue());
            if (number != null) {
                SourceInformation info = SourceInformation.from(sourceRegex, metricName);
                addGauge(measures, addPrefix(info.name), number, info.source);
            }
        }
    }

    private void addCounters(Measures measures, SortedMap<String, Counter> counters) {
        for (String metricName : counters.keySet()) {
            Counter counter = counters.get(metricName);
            long count = counter.getCount();
            SourceInformation info = SourceInformation.from(sourceRegex, metricName);
            addGauge(measures, addPrefix(info.name), count, info.source);
        }
    }

    private void addHistograms(Measures measures, SortedMap<String, Histogram> histograms) {
        for (String metricName : histograms.keySet()) {
            Histogram histogram = histograms.get(metricName);
            if (skipMetric(metricName, histogram)) {
                continue;
            }
            Long countDelta = deltaTracker.getDelta(metricName, histogram.getCount());
            maybeAdd(measures, COUNT, metricName, countDelta);
            final boolean convertDurations = false;
            addSampling(measures, metricName, histogram, convertDurations);
        }
    }

    private void addMeters(Measures measures, SortedMap<String, Meter> meters) {
        for (String metricName : meters.keySet()) {
            Meter meter = meters.get(metricName);
            if (skipMetric(metricName, meter)) {
                continue;
            }
            addMeter(measures, metricName, meter);
        }
    }

    private void addTimers(Measures measures, SortedMap<String, Timer> timers) {
        for (String metricName : timers.keySet()) {
            Timer timer = timers.get(metricName);
            if (skipMetric(metricName, timer)) {
                continue;
            }
            addMeter(measures, metricName, timer);
            final boolean convertDurations = true;
            addSampling(measures, metricName, timer, convertDurations);
        }
    }

    private void addMeter(Measures measures, String metricName, Metered meter) {
        Long countDelta = deltaTracker.getDelta(metricName, meter.getCount());
        maybeAdd(measures, COUNT, metricName, countDelta);
        maybeAdd(measures, RATE_MEAN, metricName, convertRate(meter.getMeanRate()));
        maybeAdd(measures, RATE_1_MINUTE, metricName, convertRate(meter.getOneMinuteRate()));
        maybeAdd(measures, RATE_5_MINUTE, metricName, convertRate(meter.getFiveMinuteRate()));
        maybeAdd(measures, RATE_15_MINUTE, metricName, convertRate(meter.getFifteenMinuteRate()));
    }


    private void addSampling(Measures measures, String name, Sampling sampling, boolean convert) {
        final Snapshot snapshot = sampling.getSnapshot();
        maybeAdd(measures, MEDIAN, name, convertDuration(snapshot.getMedian(), convert));
        maybeAdd(measures, PCT_75, name, convertDuration(snapshot.get75thPercentile(), convert));
        maybeAdd(measures, PCT_95, name, convertDuration(snapshot.get95thPercentile(), convert));
        maybeAdd(measures, PCT_98, name, convertDuration(snapshot.get98thPercentile(), convert));
        maybeAdd(measures, PCT_99, name, convertDuration(snapshot.get99thPercentile(), convert));
        maybeAdd(measures, PCT_999, name, convertDuration(snapshot.get999thPercentile(), convert));
        if (!omitComplexGauges) {
            final double sum = snapshot.size() * snapshot.getMean();
            final long count = (long) snapshot.size();
            if (count > 0) {
                SourceInformation info = SourceInformation.from(sourceRegex, name);
                GaugeMeasure gauge;
                try {
                    gauge = new GaugeMeasure(
                            addPrefix(info.name),
                            convertDuration(sum, convert),
                            count,
                            convertDuration(snapshot.getMin(), convert),
                            convertDuration(snapshot.getMax(), convert)
                    ).setSource(info.source);
                } catch (IllegalArgumentException e) {
                    log.warn("Could not create gauge", e);
                    return;
                }
                measures.add(gauge);
            }
        }
    }

    private void addGauge(Measures measures, String metricName, Number number, String source) {
        measures.add(new GaugeMeasure(metricName, number.doubleValue())
                .setSource(source));
    }

    private String addPrefix(String metricName) {
        if (prefix == null || prefix.length() == 0) {
            return metricName;
        }
        return prefix + prefixDelimiter + metricName;
    }

    private String checkPrefix(String prefix) {
        if ("".equals(prefix)) {
            throw new IllegalArgumentException("Prefix may either be null or a non-empty string");
        }
        return prefix;
    }

    private void maybeAdd(Measures measures, ExpandedMetric metric, String name, Number reading) {
        if (expansionConfig.isSet(metric)) {
            String metricName = metric.buildMetricName(name);
            if (!Numbers.isANumber(reading)) {
                return;
            }
            SourceInformation info = SourceInformation.from(sourceRegex, metricName);
            addGauge(measures, addPrefix(metricName), reading, info.source);
        }
    }

    private boolean skipMetric(String name, Counting counting) {
        return deleteIdleStats() && deltaTracker.peekDelta(name, counting.getCount()) == 0;
    }

    private boolean deleteIdleStats() {
        return deleteIdleStats;
    }

    private double convertDuration(double duration, boolean convert) {
        return convert ? convertDuration(duration) : duration;
    }

}
