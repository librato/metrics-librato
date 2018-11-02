**DEPRECATED:** This library is deprecated, if you are using [AppOptics](https://www.appoptics.com) please refer to [metrics-appoptics](https://github.com/appoptics/metrics-appoptics).

## Librato Metrics Plugin for the Metrics Library

The `LibratoReporter` class runs in the background, publishing metrics from <a href="http://metrics.dropwizard.io/">dropwizard/metrics</a> to the <a href="http://metrics.librato.com">Librato Metrics API</a> at the specified interval.

	<dependency>
	  <groupId>com.librato.metrics</groupId>
	  <artifactId>metrics-librato</artifactId>
	  <version>5.0.5</version>
	</dependency>

## Usage

Start the reporter in your application bootstrap:

    MetricRegistry registry = environment.metrics(); // if you're not using dropwizard, use your own registry
    Librato.reporter(registry, "<Librato Email>", "<Librato API Token>")
        .setSource("<Source Identifier (usually hostname)>")
        .start(10, TimeUnit.SECONDS);

## Tagging

You can enable the reporter to submit tagged measures.  Our tagging
product is currently in beta. If you'd like to take advantage of this,
please email support@librato.com to join the beta.

    Librato.reporter(registry, "<email>", "<token>")
        .setEnableTagging(true)  
        .addTag("tier", "web")
        .addTag("environment", "staging")
        .start(10, TimeUnit.SECONDS);

The tags you add in this way will be included on every measure. If you wish to supply custom tags at runtime you can use the Librato helper:

    Librato.metric("logins").tag("userId", userId).meter().mark()


## Fluent Helper

The Librato fluent helper provides a number of ways to make it easy to interface with Dropwizard Metrics.  You do not need to use this class but if you want to specify custom sources and/or tags, it will be easier. Some examples:

    Librato.metric(registry, "logins").tag("uid", uid).meter().mark()
    Librato.metric(registry, "kafka-read-latencies").tag("broker", broker).histogram().update(latency)
    Librato.metric(registry, "temperature").source("celcius").tag("type", "celcius").gauge(() -> 42))
    Librato.metric(registry, "jobs-processed").source("ebs").meter().mark()
    Librato.metric(registry, "just-these-tags").tag('"foo", "bar").doNotInheritTags().timer.update(time)

When you start the Librato reporter as described earlier, that will set the registry used to start it as the default registry in the fluent helper.  That lets you simply use the shorter form:

    Librato.metric("logins").tag("uid", uid).meter().mark()

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

    Librato.reporter(registry, <email>, <token>)
        .setExpansionConfig(
            new MetricExpansionConfig(
                EnumSet.of(
                    LibratoReporter.ExpandedMetric.PCT_95,
                    LibratoReporter.ExpandedMetric.RATE_1_MINUTE)))

In this configuration, the reporter will only report the 95th percentile and 1 minute rate for these metrics. Note that the `ComplexGauge`s will still be reported.

### Eliding Complex Gauges

Timers and Histograms end up generating a complex gauge along with any other expanded metrics that are configured to be sent to Librato. If you wish to exclude these complex gauges, one may enable `omitComplexGauges` in the LibratoReporter.

    Librato.reporter(registry, <email>, <token>)
      .setOmitComplexGauges(true)

Note that in addition to the mean, complex gauges also include the minimum and maximum dimensions, so if you choose to enable this option, you will no longer have access to those summaries for those metrics.

### Idle Stat Detection

A new feature in `4.0.1.4` detects when certain types of metrics (Meters, Histograms, and Timers) stop getting updated by the application. When this happens, `metrics-librato` will stop reporting these streams to Librato until they are updated again. Since Librato does not charge for metrics which are not submitted to the API, this can lower your cost, especially for metrics that report infrequently.

This is enabled by default, but should you wish to disable this feature, you can do so when setting up the LibratoReporter:

    Librato.reporter(registry, <email>, <token>)
    	.setDeleteIdleStats(false)

## Custom Sources

Sources are globally set for the LibratoReporter as described above. Sometimes though it is desirable to use custom
sources for certain signals. To do this, supply a sourceRegex to the Librato.reporter(...) builder.

The regular expression must contain one matching group. As `metrics-librato` takes metrics from the registry and
batches them, it will apply this regular expression (if supplied) to each metric name.  If the regular expression
matches, it will use the first matching group as the source for that metric, and everything after the entire
expression match will be used as the actual metric name.

    Librato.reporter(registry, <email>, <token>)
        .setSourceRegex(Pattern.compile("^(.*?)--"))

The above regular expression will take a meter name like "uid:42--api.latency" and report that with a source of
`uid:42` and a metric name of `api.latency`.

## Using Dropwizard?

The [dropwizard-librato](https://github.com/librato/dropwizard-librato) project allows you to send Metrics from within your Dropwizard application to Librato Metrics by adding a section to your config file.
