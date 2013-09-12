package com.librato.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A reporter for publishing metrics to <a href="http://metrics.librato.com/">Librato Metrics</a>
 */
public class LibratoReporter extends AbstractPollingReporter implements MetricProcessor<MetricsLibratoBatch> {
    private static final Logger LOG = LoggerFactory.getLogger(LibratoReporter.class);
    private final CounterGaugeConverter counterConverter = new CounterGaugeConverter();
    private final String source;
    private final long timeout;
    private final TimeUnit timeoutUnit;
    private final Sanitizer sanitizer;
    private final ScheduledExecutorService executor;
    private final HttpPoster httpPoster;
    private final String prefix;
    private final String prefixDelimiter;

    protected final MetricsRegistry registry;
    protected final MetricPredicate predicate;
    protected final Clock clock;
    protected final VirtualMachineMetrics vm;
    protected final boolean reportVmMetrics;
    protected final MetricExpansionConfig expansionConfig;

    /**
     * private to prevent someone from accidentally actually using this constructor. see .builder()
     */
    private LibratoReporter(String name,
                            final Sanitizer customSanitizer,
                            String source,
                            long timeout,
                            TimeUnit timeoutUnit,
                            MetricsRegistry registry,
                            MetricPredicate predicate,
                            Clock clock,
                            VirtualMachineMetrics vm,
                            boolean reportVmMetrics,
                            MetricExpansionConfig expansionConfig,
                            HttpPoster httpPoster,
                            String prefix,
                            String prefixDelimiter) {
        super(registry, name);
        this.sanitizer = customSanitizer;
        this.source = source;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.registry = registry;
        this.predicate = predicate;
        this.clock = clock;
        this.vm = vm;
        this.reportVmMetrics = reportVmMetrics;
        this.expansionConfig = expansionConfig;
        this.executor = registry.newScheduledThreadPool(1, name);
        this.httpPoster = httpPoster;
        this.prefix = LibratoUtil.checkPrefix(prefix);
        this.prefixDelimiter = prefixDelimiter;
    }

    @Override
    public void run() {
        // accumulate all the metrics in the batch, then post it allowing the LibratoBatch class to break up the work
        MetricsLibratoBatch batch = new MetricsLibratoBatch(
                LibratoBatch.DEFAULT_BATCH_SIZE,
                sanitizer,
                timeout,
                timeoutUnit,
                expansionConfig,
                httpPoster,
                prefix,
                prefixDelimiter,
                counterConverter);
        if (reportVmMetrics) {
            reportVmMetrics(batch);
        }
        reportRegularMetrics(batch);
        try {
            final long epoch = TimeUnit.MILLISECONDS.toSeconds(this.clock.time());
            batch.post(source, epoch);
        } catch (Exception e) {
            LOG.error("Librato post failed: ", e);
        }
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
        executor.scheduleAtFixedRate(this, period, period, unit);
    }

    protected void reportVmMetrics(MetricsLibratoBatch batch) {
        LibratoUtil.addVmMetricsToBatch(vm, batch);
    }

    protected void reportRegularMetrics(MetricsLibratoBatch batch) {
        final SortedMap<String, SortedMap<MetricName, Metric>> metrics = getMetricsRegistry().groupedMetrics(predicate);
        LOG.debug("Preparing batch of {} top level metrics", metrics.size());
        for (Map.Entry<String, SortedMap<MetricName, Metric>> entry : metrics.entrySet()) {
            for (Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
                final Metric metric = subEntry.getValue();
                if (metric != null) {
                    try {
                        metric.processWith(this, subEntry.getKey(), batch);
                    } catch (Exception e) {
                        LOG.error("Error processing regular metrics:", e);
                    }
                }
            }
        }
    }

    public void processGauge(MetricName name, Gauge<?> gauge, MetricsLibratoBatch batch) throws Exception {
        batch.addGauge(getStringName(name), gauge);
    }

    public void processCounter(MetricName name, Counter counter, MetricsLibratoBatch batch) throws Exception {
        batch.addCounter(getStringName(name), counter);
    }

    public void processHistogram(MetricName name, Histogram histogram, MetricsLibratoBatch batch) throws Exception {
        batch.addHistogram(getStringName(name), histogram);
    }

    public void processMeter(MetricName name, Metered meter, MetricsLibratoBatch batch) throws Exception {
        batch.addMetered(getStringName(name), meter);
    }

    public void processTimer(MetricName name, Timer timer, MetricsLibratoBatch batch) throws Exception {
        batch.addTimer(getStringName(name), timer);
    }

    private String getStringName(MetricName fullName) {
        return sanitizer.apply(LibratoUtil.nameToString(fullName));
    }

    /**
     * a builder for the LibratoReporter class that requires things that cannot be inferred and uses
     * sane default values for everything else.
     */
    public static class Builder {
        private final String source;
        private Sanitizer sanitizer = Sanitizer.NO_OP;
        private long timeout = 5;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;
        private String name = "librato-reporter";
        private MetricsRegistry registry = Metrics.defaultRegistry();
        private MetricPredicate predicate = MetricPredicate.ALL;
        private Clock clock = Clock.defaultClock();
        private VirtualMachineMetrics vm = VirtualMachineMetrics.getInstance();
        private boolean reportVmMetrics = true;
        private MetricExpansionConfig expansionConfig = MetricExpansionConfig.ALL;
        private HttpPoster httpPoster;
        private String prefix;
        private String prefixDelimiter = ".";

        public Builder(String username, String token, String source) {
            this.httpPoster = NingHttpPoster.newPoster(username, token);
            this.source = source;
        }

        /**
         * Sets the character that will follow the prefix. Defaults to ".".
         *
         * @param delimiter the delimiter
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setPrefixDelimiter(String delimiter) {
            this.prefixDelimiter = delimiter;
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
         * Sets the instance that will perform the HTTP posting
         *
         * @param httpPoster the poster
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setHttpPoster(HttpPoster httpPoster) {
            this.httpPoster = httpPoster;
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
         * override default MetricsRegistry
         *
         * @param registry registry to be used
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setRegistry(MetricsRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Filter the metrics that this particular reporter publishes
         *
         * @param predicate the predicate by which the metrics are to be filtered
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setPredicate(MetricPredicate predicate) {
            this.predicate = predicate;
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
         * use a custom instance of VirtualMachineMetrics
         *
         * @param vm the instance to use
         * @return itself
         */
        @SuppressWarnings("unused")
        public Builder setVm(VirtualMachineMetrics vm) {
            this.vm = vm;
            return this;
        }

        /**
         * turn on/off reporting of VM internal metrics (if, for example, you already get those elsewhere)
         *
         * @param reportVmMetrics true (report) or false (don't report)
         * @return itself
         */
        public Builder setReportVmMetrics(boolean reportVmMetrics) {
            this.reportVmMetrics = reportVmMetrics;
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
            return new LibratoReporter(
                    name,
                    sanitizer,
                    source,
                    timeout,
                    timeoutUnit,
                    registry,
                    predicate,
                    clock,
                    vm,
                    reportVmMetrics,
                    expansionConfig,
                    httpPoster,
                    prefix,
                    prefixDelimiter);
        }
    }

    /**
     * convenience method for creating a Builder
     */
    public static Builder builder(String username, String token, String source) {
        return new Builder(username, token, source);
    }

    public static enum ExpandedMetric {
        // sampling
        MEDIAN("median"),
        PCT_75("75th"),
        PCT_95("95th"),
        PCT_98("98th"),
        PCT_99("99th"),
        PCT_999("999th"),
        // metered
        COUNT("count"),
        RATE_MEAN("meanRate"),
        RATE_1_MINUTE("1MinuteRate"),
        RATE_5_MINUTE("5MinuteRate"),
        RATE_15_MINUTE("15MinuteRate");

        private final String displayName;

        public String buildMetricName(String metric) {
            return new StringBuilder(metric)
                    .append(".")
                    .append(displayName)
                    .toString();
        }

        private ExpandedMetric(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * Configures how to report "expanded" metrics derived from meters and histograms (e.g. percentiles,
     * rates, etc). Default is to report everything.
     *
     * @see ExpandedMetric
     */
    public static class MetricExpansionConfig {
        public static MetricExpansionConfig ALL = new MetricExpansionConfig(EnumSet.allOf(ExpandedMetric.class));
        private final Set<ExpandedMetric> enabled;

        public MetricExpansionConfig(Set<ExpandedMetric> enabled) {
            this.enabled = EnumSet.copyOf(enabled);
        }

        public boolean isSet(ExpandedMetric metric) {
            return enabled.contains(metric);
        }
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
