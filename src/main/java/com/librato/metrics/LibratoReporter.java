package com.librato.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A reporter for publishing metrics to <a href="http://metrics.librato.com/">Librato Metrics</a>
 */
public class LibratoReporter extends ScheduledReporter implements RateConverter, DurationConverter {
    private static final Logger LOG = LoggerFactory.getLogger(LibratoReporter.class);
    private final DeltaTracker deltaTracker;
    private final String source;
    private final long timeout;
    private final TimeUnit timeoutUnit;
    private final Sanitizer sanitizer;
    private final HttpPoster httpPoster;
    private final String prefix;
    private final String prefixDelimiter;
    private final Pattern sourceRegex;
    private final boolean deleteIdleStats;
    private final boolean omitComplexGauges;
    protected final MetricRegistry registry;
    protected final Clock clock;
    protected final MetricExpansionConfig expansionConfig;

    /**
     * Private. Use builder instead.
     */
    private LibratoReporter(MetricRegistry registry,
                            String name,
                            MetricFilter filter,
                            TimeUnit rateUnit,
                            TimeUnit durationUnit,
                            Sanitizer customSanitizer,
                            String source,
                            long timeout,
                            TimeUnit timeoutUnit,
                            Clock clock,
                            MetricExpansionConfig expansionConfig,
                            HttpPoster httpPoster,
                            String prefix,
                            String prefixDelimiter,
                            Pattern sourceRegex,
                            boolean deleteIdleStats,
                            boolean omitComplexGauges) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.registry = registry;
        this.sanitizer = customSanitizer;
        this.source = source;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.clock = clock;
        this.expansionConfig = expansionConfig;
        this.httpPoster = httpPoster;
        this.prefix = prefix;
        this.prefixDelimiter = prefixDelimiter;
        this.sourceRegex = sourceRegex;
        this.deltaTracker = new DeltaTracker(new DeltaMetricSupplier(registry));
        this.deleteIdleStats = deleteIdleStats;
        this.omitComplexGauges = omitComplexGauges;
    }

    public double convertMetricDuration(double duration) {
        return convertDuration(duration);
    }

    public double convertMetricRate(double rate) {
        return convertRate(rate);
    }

    /**
     * Starts the reporter polling at the given period.
     *
     * @param period the amount of time between polls
     * @param unit   the unit for {@code period}
     */
    @Override
    public void start(long period, TimeUnit unit) {
        LOG.debug("Reporter starting at fixed rate of every {} {}", period, unit);
        super.start(period, unit);
    }

    @Override
    public void stop() {
        // Stop the scheduling of tasks before stopping the http client which the
        // tasks use
        super.stop();
        try {
            httpPoster.close();
        } catch (IOException e) {
            // Intentional NOP
        }
    }

    @Override
    public void report() {
        try {
            super.report();
        } catch (Exception exception) {
            LOG.warn("Error sending report to librato", exception);
        }
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        final long epoch = TimeUnit.MILLISECONDS.toSeconds(clock.getTime());
        final MetricsLibratoBatch batch = new MetricsLibratoBatch(
                LibratoBatch.DEFAULT_BATCH_SIZE,
                sanitizer,
                timeout,
                timeoutUnit,
                expansionConfig,
                httpPoster,
                prefix,
                prefixDelimiter,
                deltaTracker,
                this,
                this,
                sourceRegex,
                omitComplexGauges);

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            batch.addGauge(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            batch.addCounter(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            String name = entry.getKey();
            Histogram histogram = entry.getValue();
            if (skipMetric(name, histogram)) {
                continue;
            }
            batch.addHistogram(entry.getKey(), histogram);
        }
        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            String name = entry.getKey();
            Meter meter = entry.getValue();
            if (skipMetric(name, meter)) {
                continue;
            }
            batch.addMeter(name, meter);
        }
        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            String name = entry.getKey();
            Timer timer = entry.getValue();
            if (skipMetric(name, timer)) {
                continue;
            }
            batch.addTimer(name, timer);
        }
        BatchResult result = batch.post(source, epoch);
        for (PostResult postResult : result.getFailedPosts()) {
            LOG.warn("Failure posting to Librato: " + postResult);
        }
    }

    private boolean skipMetric(String name, Counting counting) {
        return deleteIdleStats() && deltaTracker.peekDelta(name, counting.getCount()) == 0;
    }

    private boolean deleteIdleStats() {
        return deleteIdleStats;
    }

    /**
     * A builder for the LibratoReporter class that requires things that cannot be inferred and uses
     * sane default values for everything else.
     */
    public static class Builder {
        private String username;
        private String token;
        private final String source;
        private Sanitizer sanitizer = Sanitizer.NO_OP;
        private long timeout = 5;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;
        private TimeUnit rateUnit = TimeUnit.SECONDS;
        private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
        private String name = "librato-reporter";
        private final MetricRegistry registry;
        private MetricFilter filter = MetricFilter.ALL;
        private Clock clock = Clock.defaultClock();
        private MetricExpansionConfig expansionConfig = MetricExpansionConfig.ALL;
        private HttpPoster httpPoster;
        private String prefix;
        private String prefixDelimiter = ".";
        private Pattern sourceRegex;
        private boolean deleteIdleStats = true;
        private boolean omitComplexGauges;

        public Builder(MetricRegistry registry, String username, String token, String source) {
            this.registry = registry;
            this.username = username;
            this.token = token;
            this.source = source;
        }

        /**
         * Sets whether or not complex gauges (includes mean, min, max) should be sent to Librato. Only
         * applies to Timers and Histograms.
         *
         * @param omitComplexGauges if the complex gauges should be elided
         * @return itself
         */
        public Builder setOmitComplexGauges(boolean omitComplexGauges) {
            this.omitComplexGauges = omitComplexGauges;
            return this;
        }

        /**
         * Sets whether or not idle timers, meters, and histograms will be send to Librato or not.
         *
         * @param deleteIdleStats true if idle metrics should be elided
         * @return itself
         */
        @SuppressWarnings("UnusedDeclaration")
        public Builder setDeleteIdleStats(boolean deleteIdleStats) {
            this.deleteIdleStats = deleteIdleStats;
            return this;
        }

        /**
         * Sets the source regular expression to be applied against metric names to determine dynamic sources.
         *
         * @param sourceRegex the regular expression
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setSourceRegex(Pattern sourceRegex) {
            this.sourceRegex = sourceRegex;
            return this;
        }

        /**
         * Sets the timeout for POSTs to Librato
         *
         * @param timeout the timeout
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the timeout time unit for POSTs to Librato
         *
         * @param timeoutUnit the timeout unit
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setTimeoutUnit(TimeUnit timeoutUnit) {
            this.timeoutUnit = timeoutUnit;
            return this;
        }

        /**
         * Sets the delimiter which will separate the prefix from the metric name. Defaults
         * to "."
         *
         * @param prefixDelimiter the delimiter
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setPrefixDelimiter(String prefixDelimiter) {
            this.prefixDelimiter = prefixDelimiter;
            return this;
        }

        /**
         * Sets a prefix that will be prepended to all metric names
         *
         * @param prefix the prefix
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the {@link HttpPoster} which will send the payload to Librato
         *
         * @param poster the HttpPoster
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setHttpPoster(HttpPoster poster) {
            this.httpPoster = poster;
            return this;
        }

        /**
         * Sets the time unit to which rates will be converted by the reporter.  The default
         * value is TimeUnit.SECONDS
         *
         * @param rateUnit the rate
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setRateUnit(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Sets the time unit to which durations will be converted by the reporter. The default
         * value is TimeUnit.MILLISECONDS
         *
         * @param durationUnit the time unit
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setDurationUnit(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * set the HTTP timeout for a publishing attempt
         *
         * @param timeout     duration to expect a response
         * @param timeoutUnit unit for duration
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setTimeout(long timeout, TimeUnit timeoutUnit) {
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
            return this;
        }

        /**
         * Specify a custom name for this reporter
         *
         * @param name the name to be used
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Use a custom sanitizer. All metric names are run through a sanitizer to ensure validity before being sent
         * along. Librato places some restrictions on the characters allowed in keys, so all keys are ultimately run
         * through APIUtil.lastPassSanitizer. Specifying an additional sanitizer (that runs before lastPassSanitizer)
         * allows the user to customize what they want done about invalid characters and excessively long metric names.
         *
         * @param sanitizer the custom sanitizer to use  (defaults to a noop sanitizer).
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setSanitizer(Sanitizer sanitizer) {
            this.sanitizer = sanitizer;
            return this;
        }

        /**
         * Filter the metrics that this particular reporter publishes
         *
         * @param filter the {@link MetricFilter}
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setFilter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * use a custom clock
         *
         * @param clock to be used
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Enables control over how the reporter generates 'expanded' metrics from meters and histograms,
         * such as percentiles and rates.
         *
         * @param expansionConfig the configuration
         * @return itself
         * @see {@link ExpandedMetric}
         */
        @SuppressWarnings("unused")
        public Builder setExpansionConfig(MetricExpansionConfig expansionConfig) {
            this.expansionConfig = expansionConfig;
            return this;
        }

        /**
         * Build the LibratoReporter as configured by this Builder
         *
         * @return a fully configured LibratoReporter
         */
        public LibratoReporter build() {
            constructHttpPoster();
            return new LibratoReporter(
                    registry,
                    name,
                    filter,
                    rateUnit,
                    durationUnit,
                    sanitizer,
                    source,
                    timeout,
                    timeoutUnit,
                    clock,
                    expansionConfig,
                    httpPoster,
                    prefix,
                    prefixDelimiter,
                    sourceRegex,
                    deleteIdleStats,
                    omitComplexGauges);
        }

        /**
         * Construct the httpPoster with httpClientConfig if it has been set.
         */
        private void constructHttpPoster() {
            if (this.httpPoster == null) {
                String url = "https://metrics-api.librato.com/v1/metrics";
                this.httpPoster = new DefaultHttpPoster(url, username, token);
            }
        }
    }

    /**
     * convenience method for creating a Builder
     */
    public static Builder builder(MetricRegistry metricRegistry, String username, String token, String source) {
        return new Builder(metricRegistry, username, token, source);
    }

    /**
     * @param builder  a LibratoReporter.Builder
     * @param interval the interval at which the metrics are to be reporter
     * @param unit     the timeunit for interval
     */
    public static void enable(Builder builder, long interval, TimeUnit unit) {
        builder.build().start(interval, unit);
    }
}
