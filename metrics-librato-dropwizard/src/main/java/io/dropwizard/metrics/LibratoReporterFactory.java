package io.dropwizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.librato.metrics.LibratoReporter;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

@JsonTypeName("librato")
public class LibratoReporterFactory extends BaseReporterFactory {
    @NotNull
    @JsonProperty
    private String username = null;

    @NotNull
    @JsonProperty
    private String token = null;

    @NotNull
    @JsonProperty
    private String source = null;

    @JsonProperty
    private long timeout = 30000;

    @JsonProperty
    private String prefix = null;

    @JsonProperty
    private String name = null;

    public ScheduledReporter build(MetricRegistry registry) {
        return LibratoReporter.builder(registry, username, token, source)
                .setTimeout(timeout, TimeUnit.MILLISECONDS)
                .setPrefix(prefix)
                .setRateUnit(getRateUnit())
                .setDurationUnit(getDurationUnit())
                .setName(name)
                .setFilter(getFilter())
                .build();
    }
}

