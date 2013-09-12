## Librato Metrics Plugin for the Metrics Library

The `LibratoReporter` class runs in the background, publishing metrics from <a href="https://github.com/codahale/metrics">codahale/metrics</a> to the <a href="http://metrics.librato.com">Librato Metrics API</a> at the specified interval.

    <dependency>
        <groupId>com.librato.metrics</groupId>
        <artifactId>metrics-librato</artifactId>
        <version>2.2.0.3</version>
    </dependency>

## Usage

During the initialization of your program, simply use the `.enable` method with the appropriately configured `LibratoReporter.Builder` class. See the setters on that method for all the available customizations (there are quite a few). The constructor for the `Builder` requires only the things that are necessary; sane defaults are provided for the rest of the options.

    LibratoReporter.enable(
	    LibratoReporter.builder(
		    "<Librato Username>", 
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

Given a Coda Counter with name `foo`, the following values are reported:

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

_Note that Coda Histogram percentiles are determined using configurable <a href="http://metrics.codahale.com/manual/core/#histograms">Reservoir Sampling</a>. Histograms by default use a non-biased uniform reservoir._

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

_Note that Coda Timer percentiles are determined using configurable <a href="http://metrics.codahale.com/manual/core/#histograms">Reservoir Sampling</a>. Coda Timers by default use an exponentially decaying reservoir to prioritize newer data._

## Reducing The Volume Of Metrics Reported

While this library aims to accurately report all of the data that Coda Metrics provides, it can become somewhat verbose. One can reduce the number of metrics reported for Coda Timers, Coda Meters, and Coda Histograms when configuring the reporter. The percentiles, rates, and count for these metrics can be whitelisted (they are all on by default). In order to do this, supply a `LibratoReporter.MetricExpansionConfig` to the builder:

    LibratoReporter.builder(<username>, <token>, <source>)
		.setExpansionConfig(
			new MetricExpansionConfig(
				EnumSet.of(
					LibratoReporter.ExpandedMetric.PCT_95,
					LibratoReporter.ExpandedMetric.RATE_1_MINUTE)))
		.build();

In this configuration, the reporter will only report the 95th percentile and 1 minute rate for these metrics. Note that the `ComplexGauge`s will still be reported.



