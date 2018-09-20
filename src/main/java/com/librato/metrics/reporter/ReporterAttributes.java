package com.librato.metrics.reporter;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.librato.metrics.client.Duration;
import com.librato.metrics.client.IPoster;
import com.librato.metrics.client.Tag;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ReporterAttributes {
    String url = "https://metrics-api.librato.com";
    String reporterName = "librato";
    MetricFilter metricFilter = MetricFilter.ALL;
    TimeUnit rateUnit = TimeUnit.SECONDS;
    TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    MetricRegistry registry = new MetricRegistry();
    String email;
    String token;
    Pattern sourceRegex;
    String prefix;
    String prefixDelimiter = ".";
    MetricExpansionConfig expansionConfig = MetricExpansionConfig.ALL;
    boolean deleteIdleStats = true;
    boolean omitComplexGauges;
    Duration readTimeout;
    Duration connectTimeout;
    String source;
    List<Tag> tags = new LinkedList<Tag>();
    ILibratoClientFactory libratoClientFactory = new DefaultLibratoClientFactory();
    boolean enableLegacy = true;
    boolean enableTagging;
    RateConverter rateConverter;
    DurationConverter durationConverter;
    IPoster poster;
}
