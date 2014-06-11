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
        Pattern sourceRegexPattern = null;
        if (sourceRegex != null) {
            sourceRegexPattern = Pattern.compile(sourceRegex);
        }
        HttpPoster httpPoster = null;
        if (libratoUrl != null) {
            httpPoster = NingHttpPoster.newPoster(username, token, libratoUrl);
        }
        LibratoReporter.Builder builder = LibratoReporter.builder(registry, username, token, source)
                .setPrefix(prefix)
                .setRateUnit(getRateUnit())
                .setDurationUnit(getDurationUnit())
                .setName(name)
                .setFilter(getFilter())
                .setHttpPoster(httpPoster)
                .setSourceRegex(sourceRegexPattern);

        if (timeout != null) {
            builder.setTimeout(timeout, TimeUnit.SECONDS);
        }
        if (prefixDelimiter != null) {
            builder.setPrefix(prefixDelimiter);
        }

        return builder.build();
    }
}

