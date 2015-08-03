## Librato Metrics Plugin for the Metrics Library

The `LibratoReporter` class runs in the background, publishing metrics from <a href="https://github.com/dropwizard/metrics">dropwizard/metrics</a> to the <a href="http://metrics.librato.com">Librato Metrics API</a> at the specified interval.

    <dependency>
        <groupId>com.librato.metrics</groupId>
        <artifactId>metrics-librato</artifactId>
        <version>4.0.1.10</version>
    </dependency>

## Updating from 3.x?

The `metrics-librato:4.x.x.x` release depends on `librato-java:1.x` which includes a fix for an [issue](https://github.com/librato/librato-java/pull/12) 
that was causing dashes to be stripped from metric names incorrectly. It is recommended to view the [readme](https://github.com/librato/librato-java#updating-from-01x-)
for that project before upgrading to understand how some existing metric names might change.

## Usage

During the initialization of your program, simply use the `.enable` method with the appropriately configured `LibratoReporter.Builder` class. See the setters on that method for all the available customizations (there are quite a few). The constructor for the `Builder` requires only the things that are necessary; sane defaults are provided for the rest of the options.

    MetricRegistry registry = environment.metrics(); // if you're not using dropwizard, use your own registry
    LibratoReporter.enable(
        LibratoReporter.builder(
            registry,
            "<Librato Email>",
            "<Librato API Token>",
            "<Source Identifier (usually hostname)>"),
        10,
        TimeUnit.SECONDS);

## Versioning

Since this project depends heavily on `metrics-core`, the version number will be `<version of metrics-core being depended on>.<release number of the reporter>`. Thus, the first release will be version 2.1.2.0, as 2.1.2 is the current stable version of `metrics-core`. If this proves impractical, a different versioning scheme will be investigated.

## Librato Metrics Used

This library will output a few different kinds of Librato Metrics to Librato:

* Gauges: a measurement at a point in time
* DeltaGauge: a gauge that submits the delta between the current value of the gauge and the previous value. Note that the reporter will omit the first value for a DeltaGauge because it knows no previous value at that time.
* ComplexGauge: includes sum, count, min, max, and average measurements.See <a href="http://dev.librato.com/v1/post/metrics">the API documentation</a> for more information on extended gauge parameters.

## Translation to Librato Metrics

This section describes how each of the Coda metrics translate into Librato metrics.

### Coda Gauges

Given a Coda Gauge with name `foo`, the following values are reported:

* Gauge: name=foo

The value reported for the Gauge is the current value of the Coda Gauge at flush time.

### Coda Counters

Given a Coda Counter with name `foo`, the following values are reported:

* Gauge: name=foo

The value reported for the Gauge is the current value of the Coda Counter at flush time.

_Note: Librato Counters represent monotonically increasing values. Since Coda Counters can be incremented or decremented, it makes sense to report them as Librato Gauges._

### Coda Histograms

Given a Coda Histogram with name `foo`, the following values are reported:

* ComplexGauge: name=foo
* Gauge: name=foo.median
* Gauge: name=foo.75th
* Gauge: name=foo.95th
* Gauge: name=foo.98th
* Gauge: name=foo.99th
* Gauge: name=foo.999th
* DeltaGauge: name=foo.count (represents the number of values the Coda Histogram has recorded)

_Note that Coda Histogram percentiles are determined using configurable <a href="https://dropwizard.github.io/metrics/3.1.0/manual/core/#histograms">Reservoir Sampling</a>. Histograms by default use a non-biased uniform reservoir._

### Coda Meters

Given a Coda Meter with name `foo`, the following values are reported:

* DeltaGauge: name=foo.count (represents the number of values the Coda Meter has recorded)
* Gauge: name=foo.meanRate
* Gauge: name=foo.1MinuteRate
* Gauge: name=foo.5MinuteRate
* Gauge: name=foo.15MinuteRate

### Coda Timers

Coda Timers compose a Coda Meter as well as a Coda Histogram, so the values reported to Librato are the union of the values reported for these two metric types.

Given a Coda Timer with name `foo`, the following values are reported:

* ComplexGauge: name=foo
* Gauge: name=foo.median
* Gauge: name=foo.75th
* Gauge: name=foo.95th
* Gauge: name=foo.98th
* Gauge: name=foo.99th
* Gauge: name=foo.999th
* DeltaGauge: name=foo.count (represents the number of values the Coda Timer has recorded)
* Gauge: name=foo.meanRate
* Gauge: name=foo.1MinuteRate
* Gauge: name=foo.5MinuteRate
* Gauge: name=foo.15MinuteRate

_Note that Coda Timer percentiles are determined using configurable <a href="https://dropwizard.github.io/metrics/3.1.0/manual/core/#histograms">Reservoir Sampling</a>. Coda Timers by default use an exponentially decaying reservoir to prioritize newer data._

## Reducing The Volume Of Metrics Reported

### Eliding Certain Metrics

While this library aims to accurately report all of the data that Coda Metrics provides, it can become somewhat verbose. One can reduce the number of metrics reported for Coda Timers, Coda Meters, and Coda Histograms when configuring the reporter. The percentiles, rates, and count for these metrics can be whitelisted (they are all on by default). In order to do this, supply a `LibratoReporter.MetricExpansionConfig` to the builder:

    LibratoReporter.builder(<username>, <token>, <source>)
        .setExpansionConfig(
            new MetricExpansionConfig(
                EnumSet.of(
                    LibratoReporter.ExpandedMetric.PCT_95,
                    LibratoReporter.ExpandedMetric.RATE_1_MINUTE)))
        .build();

In this configuration, the reporter will only report the 95th percentile and 1 minute rate for these metrics. Note that the `ComplexGauge`s will still be reported.

### Eliding Complex Gauges

Timers and Histograms end up generating a complex gauge along with any other expanded metrics that are configured to be sent to Librato. If you wish to exclude these complex gauges, one may enable `omitComplexGauges` in the LibratoReporter.

    LibratoReporter.builder(<username>, <token>, <source>)
      .setOmitComplexGauges(true)
      .build();
      
Note that in addition to the mean, complex gauges also include the minimum and maximum dimensions, so if you choose to enable this option, you will no longer have access to those summaries for those metrics.

### Idle Stat Detection

A new feature in `4.0.1.4` detects when certain types of metrics (Meters, Histograms, and Timers) stop getting updated by the application. When this happens, `metrics-librato` will stop reporting these streams to Librato until they are updated again. Since Librato does not charge for metrics which are not submitted to the API, this can lower your cost, especially for metrics that report infrequently.

This is enabled by default, but should you wish to disable this feature, you can do so when setting up the LibratoReporter:

    LibratoReporter.builder(<username>, <token>, <source>)
    	...
    	.setDeleteIdleStats(false)
    	

## Custom Sources

Sources are globally set for the LibratoReporter as described above. Sometimes though it is desirable to use custom
sources for certain signals. To do this, supply a sourceRegex to the LibratoReporter builder.

The regular expression must contain one matching group. As `metrics-librato` takes metrics from the registry and
batches them, it will apply this regular expression (if supplied) to each metric name.  If the regular expression 
matches, it will use the first matching group as the source for that metric, and everything after the entire
expression match will be used as the actual metric name.

    builder.setSourceRegex(Pattern.compile("^(.*?)--"))
    
The above regular expression will take a meter name like "uid:42--api.latency" and report that with a source of
`uid:42` and a metric name of `api.latency`.

## Using Dropwizard?

The [dropwizard-librato](https://github.com/librato/dropwizard-librato) project allows you to send Metrics from within your Dropwizard application to Librato Metrics by adding a section to your config file.
