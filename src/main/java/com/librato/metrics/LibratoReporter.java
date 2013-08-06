package com.librato.metrics;

import com.ning.http.client.*;
import com.ning.http.util.Base64;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

/**
 * User: mihasya
 * Date: 6/14/12
 * Time: 1:08 PM
 * A reporter for publishing metrics to <a href="http://metrics.librato.com/">Librato Metrics</a>
 */
public class LibratoReporter extends AbstractPollingReporter implements MetricProcessor<MetricsLibratoBatch> {
    private final String source;

    private final String authHeader;
    private final String apiUrl;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private final APIUtil.Sanitizer sanitizer;

    protected final MetricsRegistry registry;
    protected final MetricPredicate predicate;
    protected final Clock clock;
    protected final VirtualMachineMetrics vm;
    protected final boolean reportVmMetrics;
    protected final MetricExpansionConfig expansionConfig;

    private final AsyncHttpClient httpClient = new AsyncHttpClient();

    private final ScheduledExecutorService executor;

    private static final LibratoUtil util = new LibratoUtil();

    private static final Logger LOG = LoggerFactory.getLogger(LibratoReporter.class);

    /**
     * private to prevent someone from accidentally actually using this constructor. see .builder()
     */
    private LibratoReporter(String authHeader, String apiUrl, String name, final APIUtil.Sanitizer customSanitizer,
                            String source, long timeout, TimeUnit timeoutUnit, MetricsRegistry registry,
                            MetricPredicate predicate, Clock clock, VirtualMachineMetrics vm, boolean reportVmMetrics,
                            MetricExpansionConfig expansionConfig) {
        super(registry, name);
        this.authHeader = authHeader;
        this.sanitizer = customSanitizer;
        this.apiUrl = apiUrl;
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
    }

    @Override
    public void run() {
        // accumulate all the metrics in the batch, then post it allowing the LibratoBatch class to break up the work
        MetricsLibratoBatch batch =
            new MetricsLibratoBatch(LibratoBatch.DEFAULT_BATCH_SIZE, sanitizer, timeout, timeoutUnit, expansionConfig);
        if (reportVmMetrics) {
            reportVmMetrics(batch);
        }
        reportRegularMetrics(batch);
        AsyncHttpClient.BoundRequestBuilder builder = httpClient.preparePost(apiUrl);
        builder.addHeader("Content-Type", "application/json");
        builder.addHeader("Authorization", authHeader);

        try {
            batch.post(builder, source, TimeUnit.MILLISECONDS.toSeconds(Clock.defaultClock().time()));
        } catch (Exception e) {
            LOG.error("Librato post failed: ", e);
        }
    }

    /**
     * Starts the reporter polling at the given period.
     *
     * @param period    the amount of time between polls
     * @param unit      the unit for {@code period}
     */
    @Override
    public void start(long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(this, period, period, unit);
    }

    /**
     * Shuts down the reporter polling, waiting the specific amount of time for any current polls to
     * complete.
     *
     * @param timeout    the maximum time to wait
     * @param unit       the unit for {@code timeout}
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public void shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        super.shutdown(timeout, unit);
        executor.shutdown();
        executor.awaitTermination(timeout, unit);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        super.shutdown();
    }

    protected void reportVmMetrics(MetricsLibratoBatch batch) {
        util.addVmMetricsToBatch(vm, batch);
    }

    protected void reportRegularMetrics(MetricsLibratoBatch batch) {
        for (Map.Entry<String,SortedMap<MetricName,Metric>> entry :
                getMetricsRegistry().groupedMetrics(predicate).entrySet()) {

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

    private String getStringName(MetricName fullName) {
        return sanitizer.apply(util.nameToString(fullName));
    }

    @Override
    public void processMeter(MetricName name, Metered meter, MetricsLibratoBatch batch) throws Exception {
        batch.addMetered(getStringName(name), meter);
    }

    @Override
    public void processCounter(MetricName name, Counter counter, MetricsLibratoBatch batch) throws Exception {
         batch.addCounterMeasurement(getStringName(name), counter.count());
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, MetricsLibratoBatch batch) throws Exception {
        String sanitizedName = getStringName(name);
        batch.addSummarizable(sanitizedName, histogram);
        batch.addSampling(sanitizedName, histogram);
    }

    @Override
    public void processTimer(MetricName name, Timer timer, MetricsLibratoBatch batch) throws Exception {
        String sanitizedName = getStringName(name);
        batch.addMetered(sanitizedName, timer);
        batch.addSummarizable(sanitizedName, timer);
        batch.addSampling(sanitizedName, timer);
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, MetricsLibratoBatch batch) throws Exception {
        if (gauge.value() instanceof Number) {
            batch.addGauge(getStringName(name), gauge);
        }
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

        private String name = "librato-reporter";
        private MetricsRegistry registry = Metrics.defaultRegistry();
        private MetricPredicate predicate = MetricPredicate.ALL;
        private Clock clock = Clock.defaultClock();
        private VirtualMachineMetrics vm = VirtualMachineMetrics.getInstance();
        private boolean reportVmMetrics = true;
        private MetricExpansionConfig expansionConfig = MetricExpansionConfig.ALL;

        public Builder(String username, String token, String source) {
            if (username == null || username.equals("")) {
                throw new IllegalArgumentException(String.format("Username must be a non-null, non-empty string. You used '%s'", username));
            }
            if (token == null || token.equals("")) {
                throw new IllegalArgumentException(String.format("Token must be a non-null, non-empty string. You used '%s'", username));
            }
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
         * override default MetricsRegistry
         * @param registry registry to be used
         * @return itself
         */
        public Builder setRegistry(MetricsRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Filter the metrics that this particular reporter publishes
         * @param predicate the predicate by which the metrics are to be filtered
         * @return itself
         */
        public Builder setPredicate(MetricPredicate predicate) {
            this.predicate = predicate;
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
         * use a custom instance of VirtualMachineMetrics
         * @param vm the instance to use
         * @return itself
         */
        public Builder setVm(VirtualMachineMetrics vm) {
            this.vm = vm;
            return this;
        }

        /**
         * turn on/off reporting of VM internal metrics (if, for example, you already get those elsewhere)
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
         * @See {@link ExpandedMetric}
         * @return itself
         */
        public Builder setExpansionConfig(MetricExpansionConfig expansionConfig) {
          this.expansionConfig = expansionConfig;
          return this;
        }

        /**
         * Build the LibratoReporter as configured by this Builder
         * @return a fully configured LibratoReporter
         */
        public LibratoReporter build() {
            String auth = String.format("Basic %s", Base64.encode((username + ":" + token).getBytes()));
            return new LibratoReporter(auth,
                    apiUrl, name, sanitizer, source, timeout, timeoutUnit,
                    registry, predicate, clock, vm, reportVmMetrics, expansionConfig);
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

      private String displayName;

      public String buildMetricName(String metric) {
        StringBuilder sb = new StringBuilder(metric);
        sb.append('.').append(this.displayName);
        return sb.toString();
      }

      private ExpandedMetric(String displayName) {
        this.displayName = displayName;
      }
    }

    /**
     * Configures how to report "expanded" metrics derived from meters and histograms (e.g. percentiles,
     * rates, etc). Default is to report everything.
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
     * @param builder a LibratoReporter.Builder
     * @param interval the interval at which the metrics are to be reporter
     * @param unit the timeunit for interval
     */
    public static void enable(Builder builder, long interval, TimeUnit unit) {
        builder.build().start(interval, unit);
    }
}
