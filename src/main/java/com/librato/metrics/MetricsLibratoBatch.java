package com.librato.metrics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.librato.metrics.LibratoReporter.ExpandedMetric.*;

import com.librato.metrics.LibratoReporter.ExpandedMetric;
import com.librato.metrics.LibratoReporter.MetricExpansionConfig;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Sampling;
import com.yammer.metrics.core.Summarizable;
import com.yammer.metrics.stats.Snapshot;



/**
 * User: mihasya
 * Date: 6/17/12
 * Time: 10:57 PM
 * a LibratoBatch that understand Metrics-specific types
 */
public class MetricsLibratoBatch extends LibratoBatch {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsLibratoBatch.class);

    private final MetricExpansionConfig expansionConfig;

    /**
     * a string used to identify the library
     */
    private static final String agentIdentifier;

    static {
        InputStream pomIs = null;
        BufferedReader b = null;
        String version = "unknown";
        try {
            pomIs = LibratoReporter.class.getClassLoader().getResourceAsStream("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties");
            b = new BufferedReader(new InputStreamReader(pomIs));
            String line = b.readLine();
            while (line != null)  {
                if (line.startsWith("version")) {
                    version = line.split("=")[1];
                    break;
                }
                line = b.readLine();
            }
        } catch (Throwable e) {
            LOG.error("Failure reading package version for librato-java", e);
        }

        // now coda!
        String codaVersion = "unknown";
        try {
            pomIs = MetricsRegistry.class.getClassLoader().getResourceAsStream("META-INF/maven/com.yammer.metrics/metrics-core/pom.properties");
            b = new BufferedReader(new InputStreamReader(pomIs));
            String line = b.readLine();
            while (line != null)  {
                if (line.startsWith("version")) {
                    codaVersion = line.split("=")[1];
                    break;
                }
                line = b.readLine();
            }
        } catch (Throwable e) {
            LOG.error("Failure reading package version for librato-java", e);
        }

        agentIdentifier = String.format("metrics-librato/%s metrics/%s", version, codaVersion);
    }

    public MetricsLibratoBatch(int postBatchSize, APIUtil.Sanitizer sanitizer, long timeout, TimeUnit timeoutUnit,
                               MetricExpansionConfig expansionConfig) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, agentIdentifier);
        this.expansionConfig = expansionConfig;
    }

    public void addGauge(String name, Gauge gauge) {
        addGaugeMeasurement(name, (Number) gauge.value());
    }

    public void addSummarizable(String name, Summarizable summarizable) {
        // TODO: add sum_squares if/when Summarizble exposes it
        double countCalculation = summarizable.sum() / summarizable.mean();
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
        Snapshot snapshot = sampling.getSnapshot();
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
        addMeasurement(new SingleValueGaugeMeasurement(metric.buildMetricName(name), reading));
      }
    }
}
