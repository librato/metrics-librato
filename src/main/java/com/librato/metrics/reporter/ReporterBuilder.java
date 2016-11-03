package com.librato.metrics.reporter;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.librato.metrics.MetricExpansionConfig;
import com.librato.metrics.client.Duration;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ReporterBuilder {
    private final ReporterAttributes atts = new ReporterAttributes();

    public ReporterBuilder(MetricRegistry registry, String email, String token) {
        this.atts.registry = registry;
        this.atts.email = email;
        this.atts.token = token;
    }

    public LibratoMetricsReporter build() {
        return new LibratoMetricsReporter(atts);
    }

    public ReporterBuilder setUrl(String url) {
        this.atts.url = url;
        return this;
    }

    public ReporterBuilder setName(String name) {
        this.atts.reporterName = name;
        return this;
    }

    public ReporterBuilder setSourceRegex(String regex) {
        this.atts.sourceRegex = Pattern.compile(regex);
        return this;
    }

    public ReporterBuilder setExpansionConfig(MetricExpansionConfig config) {
        this.atts.expansionConfig = config;
        return this;
    }

    public ReporterBuilder setDeleteIdleStats(boolean value) {
        this.atts.deleteIdleStats = value;
        return this;
    }

    public ReporterBuilder setOmitComplexGauges(boolean value) {
        this.atts.omitComplexGauges = value;
        return this;
    }

    public ReporterBuilder setRateUnit(TimeUnit unit) {
        this.atts.rateUnit = unit;
        return this;
    }

    public ReporterBuilder setDurationUnit(TimeUnit unit) {
        this.atts.durationUnit = unit;
        return this;
    }

    public ReporterBuilder setFilter(MetricFilter filter) {
        this.atts.metricFilter = filter;
        return this;
    }

    public ReporterBuilder setPrefix(String prefix) {
        this.atts.prefix = prefix;
        return this;
    }

    public ReporterBuilder setPrefixDelimiter(String prefixDelimiter) {
        this.atts.prefixDelimiter = prefixDelimiter;
        return this;
    }

    public ReporterBuilder setTimeout(long time, TimeUnit unit) {
        this.atts.readTimeout = new Duration(time, unit);
        return this;
    }
}
