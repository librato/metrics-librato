package io.dropwizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.librato.metrics.HttpPoster;
import com.librato.metrics.LibratoReporter;
import com.librato.metrics.NingHttpPoster;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@JsonTypeName("librato")
public class LibratoReporterFactory extends BaseReporterFactory {
    @NotNull
    @JsonProperty
    private String username;

    @NotNull
    @JsonProperty
    private String token;

    @NotNull
    @JsonProperty
    private String source;

    @JsonProperty
    private Long timeout;

    @JsonProperty
    private String prefix;

    @JsonProperty
    private String name;

    @JsonProperty
    private String libratoUrl;

    @JsonProperty
    private String sourceRegex;

    @JsonProperty
    private String prefixDelimiter;

    public ScheduledReporter build(MetricRegistry registry) {
        LibratoReporter.Builder builder = LibratoReporter.builder(registry, username, token, source)
                .setRateUnit(getRateUnit())
                .setDurationUnit(getDurationUnit())
                .setFilter(getFilter());
        if (sourceRegex != null) {
            Pattern sourceRegexPattern = Pattern.compile(sourceRegex);
            builder.setSourceRegex(sourceRegexPattern);
        }
        if (libratoUrl != null) {
            HttpPoster httpPoster = NingHttpPoster.newPoster(username, token, libratoUrl);
            builder.setHttpPoster(httpPoster);
        }
        if (prefix != null) {
            builder.setPrefix(prefix);
        }
        if (name != null) {
            builder.setName(name);
        }
        if (timeout != null) {
            builder.setTimeout(timeout, TimeUnit.SECONDS);
        }
        if (prefixDelimiter != null) {
            builder.setPrefix(prefixDelimiter);
        }
        return builder.build();
    }
}

