package com.librato.metrics.reporter;

import com.codahale.metrics.Metric;
import com.librato.metrics.LibratoReporter;
import com.librato.metrics.Versions;

public class Agent {
    /**
     * a string used to identify the library
     */
    public static final String AGENT_IDENTIFIER;

    static {
        String metricsCoreVersion = Versions.getVersion(
                "META-INF/maven/io.dropwizard.metrics/metrics-core/pom.properties",
                Metric.class);
        String metricsLibratoVersion = Versions.getVersion(
                "META-INF/maven/com.librato.metrics/metrics-librato/pom.properties",
                LibratoReporter.class);
        AGENT_IDENTIFIER = String.format(
                "metrics-librato/%s metrics/%s",
                metricsLibratoVersion,
                metricsCoreVersion);
    }
}
