package com.librato.metrics;

import com.librato.metrics.client.Versions;
import com.yammer.metrics.core.Metric;

public class Agent {
    /**
     * a string used to identify the library
     */
    public static final String AGENT_IDENTIFIER;

    static {
        String metricsCoreVersion = Versions.getVersion(
                "META-INF/maven/com.yammer.metrics/metrics-core/pom.properties",
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

