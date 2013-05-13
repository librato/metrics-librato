package com.librato.metrics;

import com.codahale.metrics.*;
import com.ning.http.client.*;
import com.ning.http.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * User: mihasya
 * Date: 6/14/12
 * Time: 1:08 PM
 * A reporter for publishing metrics to <a href="http://metrics.librato.com/">Librato Metrics</a>
 */
public class LibratoReporter extends ScheduledReporter {
    private final String source;

    private final String authHeader;
    private final String apiUrl;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private final APIUtil.Sanitizer sanitizer;

    protected final Clock clock;

    private final AsyncHttpClient httpClient = new AsyncHttpClient();

    private static final Logger LOG = LoggerFactory.getLogger(LibratoReporter.class);

    /**
     * private to prevent someone from accidentally actually using this constructor. see .builder()
     */
    private LibratoReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit,
                            String authHeader, String apiUrl, final APIUtil.Sanitizer customSanitizer,
                            String source, long timeout, TimeUnit timeoutUnit,  Clock clock) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.authHeader = authHeader;
        this.apiUrl = apiUrl;
        this.sanitizer = customSanitizer;
        this.source = source;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.clock = clock;
    }

    /**
     * a builder for the LibratoReporter class that requires things that cannot be inferred and uses
     * sane default values for everything else.
     */
    public static class Builder {
        private final String username;
        private final String token;
        private final String source;

        private String apiUrl = "https://metrics-api.librato.com/v1/metrics";

        private APIUtil.Sanitizer sanitizer = APIUtil.noopSanitizer;

        private long timeout = 5;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;
        private TimeUnit rateUnit = TimeUnit.SECONDS;
        private TimeUnit durationUnit = TimeUnit.MILLISECONDS;

        private String name = "librato-reporter";
        private final MetricRegistry registry;
        private MetricFilter filter =MetricFilter.ALL;
        private Clock clock = Clock.defaultClock();

        public Builder(MetricRegistry registry, String username, String token, String source) {
            if (username == null || username.equals("")) {
                throw new IllegalArgumentException(String.format("Username must be a non-null, non-empty string. You used '%s'", username));
            }
            if (token == null || token.equals("")) {
                throw new IllegalArgumentException(String.format("Token must be a non-null, non-empty string. You used '%s'", username));
            }
            this.registry = registry;
            this.username = username;
            this.token = token;
            this.source = source;
        }

        /**
         * publish to a custom URL (for internal testing)
         * @param apiUrl custom API endpoint to use
         * @return itself
         */
        public Builder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        /**
         * set the HTTP timeout for a publishing attempt
         * @param timeout duration to expect a response
         * @param timeoutUnit unit for duration
         * @return itself
         */
        public Builder setTimeout(long timeout, TimeUnit timeoutUnit) {
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
            return this;
        }

        /**
         * set the timeunit to be used for rates, as required by metrics.ScheduledReporter
         * @param rateUnit the time unit. Default: TimeUnit.SECONDS
         */
        public void setRateUnit(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
        }

        /**
         * set the timeunit to be used for durations, as required by metrics.ScheduledReporter
         * @param durationUnit the time unit. Default: TimeUnit.MILLISECONDS
         */
        public void setDurationUnit(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
        }

        /**
         * Specify a custom name for this reporter
         * @param name the name to be used
         * @return itself
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Use a custom sanitizer. All metric names are run through a sanitizer to ensure validity before being sent
         * along. Librato places some restrictions on the characters allowed in keys, so all keys are ultimately run
         * through APIUtil.lastPassSanitizer. Specifying an additional sanitizer (that runs before lastPassSanitizer)
         * allows the user to customize what they want done about invalid characters and excessively long metric names.
         * @param sanitizer the custom sanitizer to use  (defaults to a noop sanitizer).
         * @return itself
         */
        public Builder setSanitizer(APIUtil.Sanitizer sanitizer) {
            this.sanitizer = sanitizer;
            return this;
        }

        /**
         * Filter the metrics that this particular reporter publishes
         * @param filter the filter by which the metrics are to be filtered
         * @return itself
         */
        public Builder setFilter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * use a custom clock
         * @param clock to be used
         * @return itself
         */
        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Build the LibratoReporter as configured by this Builder
         * @return a fully configured LibratoReporter
         */
        public LibratoReporter build() {
            String auth = String.format("Basic %s", Base64.encode((username + ":" + token).getBytes()));
            return new LibratoReporter(registry, name, filter, rateUnit, durationUnit,
                    auth, apiUrl, sanitizer, source, timeout, timeoutUnit, clock);
        }
    }

    /**
     * convenience method for creating a Builder
     */
    public static Builder builder(MetricRegistry registry, String username, String token, String source) {
        return new Builder(registry, username, token, source);
    }

    /**
     * @param builder a LibratoReporter.Builder
     * @param interval the interval at which the metrics are to be reporter
     * @param unit the timeunit for interval
     */
    public static void enable(Builder builder, long interval, TimeUnit unit) {
        builder.build().start(interval, unit);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        long ts = TimeUnit.MILLISECONDS.toSeconds(clock.getTime());
        MetricsLibratoBatch batch = new MetricsLibratoBatch(LibratoBatch.DEFAULT_BATCH_SIZE, sanitizer, timeout, timeoutUnit);
        for (SortedMap.Entry<String, Gauge> entry : gauges.entrySet()) {
            if (entry.getValue().getValue() instanceof Number) {
                batch.addGaugeMeasurement(entry.getKey(), (Number)(entry.getValue().getValue()));
            }
        }
        for (SortedMap.Entry<String, Counter> entry : counters.entrySet()) {
            batch.addCounterMeasurement(entry.getKey(), entry.getValue().getCount());
        }
        for (SortedMap.Entry<String, Histogram> entry : histograms.entrySet()) {
            batch.addSampling(entry.getKey(), entry.getValue());
        }
        for (SortedMap.Entry<String, Meter> entry : meters.entrySet()) {
            batch.addMetered(entry.getKey(), entry.getValue());
        }
        for (SortedMap.Entry<String, Timer> entry : timers.entrySet()) {
            batch.addMetered(entry.getKey(), entry.getValue());
            batch.addSampling(entry.getKey(), entry.getValue());
        }
        AsyncHttpClient.BoundRequestBuilder builder = httpClient.preparePost(apiUrl);
        builder.addHeader("Content-Type", "application/json");
        builder.addHeader("Authorization", authHeader);
        batch.post(builder, source, ts);
    }
}
