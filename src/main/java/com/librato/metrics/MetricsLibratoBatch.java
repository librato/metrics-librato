package com.librato.metrics;

import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
        final String version = getVersion("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties", LibratoReporter.class);
        final String codaVersion = getVersion("META-INF/maven/com.yammer.metrics/metrics-core/pom.properties", MetricsRegistry.class);
        agentIdentifier = String.format("metrics-librato/%s metrics/%s", version, codaVersion);
    }

    /**
     * Attempts to get a version property from a specified resource
     * @param path the path of the properties file resource
     * @param klass the Class whose classloader will be used to load the resource
     * @return the found version, "unknown" if it could not be found / determined
     */
    private static String getVersion(String path, Class<?> klass) {
        try {
            InputStream in = klass.getClassLoader().getResourceAsStream(path);
            if (in != null) {
                try {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = reader.readLine();
                    while (line != null) {
                        if (line.startsWith("version")) {
                            return line.split("=")[1];
                        }
                    }
                } finally {
                    in.close();
                }
            }
        } catch (IOException e) {
            LOG.error("Could not read package version using path " + path + ":", e);
        }
        return "unknown";
    }


    public MetricsLibratoBatch(int postBatchSize, APIUtil.Sanitizer sanitizer, long timeout, TimeUnit timeoutUnit) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, agentIdentifier);
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
        addMeasurement(new SingleValueGaugeMeasurement(name + ".median", snapshot.getMedian()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".75th", snapshot.get75thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".95th", snapshot.get95thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".98th", snapshot.get98thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".99th", snapshot.get99thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".999th", snapshot.get999thPercentile()));
    }

    public void addMetered(String name, Metered meter) {
        addMeasurement(new SingleValueGaugeMeasurement(name + ".count", meter.count()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".meanRate", meter.meanRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".1MinuteRate", meter.oneMinuteRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".5MinuteRate", meter.fiveMinuteRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name + ".15MinuteRate", meter.fifteenMinuteRate()));
    }
}
