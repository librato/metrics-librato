package com.librato.metrics;

import com.librato.metrics.client.Duration;
import com.librato.metrics.client.Sanitizer;
import com.librato.metrics.client.Tag;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ReporterAttributes {
    String url = "https://metrics-api.librato.com";
    String reporterName = "librato";
    TimeUnit rateUnit = TimeUnit.SECONDS;
    TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    MetricsRegistry registry = new MetricsRegistry();
    String email;
    String token;
    Pattern sourceRegex;
    MetricPredicate predicate;
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
    public Sanitizer customSanitizer;
}


