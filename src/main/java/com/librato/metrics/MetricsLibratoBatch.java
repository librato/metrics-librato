package com.librato.metrics;

import com.librato.metrics.LibratoBatch;
import com.librato.metrics.MultiSampleGaugeMeasurement;
import com.librato.metrics.SingleValueGaugeMeasurement;
import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * User: mihasya
 * Date: 6/17/12
 * Time: 10:57 PM
 * a LibratoBatch that understand Metrics-specific types
 */
public class MetricsLibratoBatch extends LibratoBatch {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsLibratoBatch.class);

    /**
     * a string used to identify the library
     */
    private static final String agentIdentifier;

    static {
        InputStream pomIs = LibratoReporter.class.getClassLoader().getResourceAsStream("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties");
        BufferedReader b = new BufferedReader(new InputStreamReader(pomIs));
        String version = "unknown";
        try {
            String line = b.readLine();
            while (line != null)  {
                if (line.startsWith("version")) {
                    version = line.split("=")[1];
                    break;
                }
                line = b.readLine();
            }
        } catch (IOException e) {
            LOG.error("Failure reading package version for librato-java", e);
        }

        // now coda!

        pomIs = MetricsRegistry.class.getClassLoader().getResourceAsStream("META-INF/maven/com.yammer.metrics/metrics-core/pom.properties");
        b = new BufferedReader(new InputStreamReader(pomIs));
        String codaVersion = "unknown";
        try {
            String line = b.readLine();
            while (line != null)  {
                if (line.startsWith("version")) {
                    codaVersion = line.split("=")[1];
                    break;
                }
                line = b.readLine();
            }
        } catch (IOException e) {
            LOG.error("Failure reading package version for librato-java", e);
        }

        agentIdentifier = String.format("metrics-librato/%s metrics/%s", version, codaVersion);
    }

    public MetricsLibratoBatch(int postBatchSize, long timeout, TimeUnit timeoutUnit) {
        super(postBatchSize, timeout, timeoutUnit, agentIdentifier);
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
