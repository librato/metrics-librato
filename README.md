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

## Translation

This section describes how each of the Coda metrics translate into Librato metrics. 

### Coda Gauges

Given a Coda Counter with name "foo", the following values are reported:

* Gauge: name=foo

The value reported for the Gauge is the current value of the Coda Gauge at flush time.

### Coda Counters

Given a Coda Counter with name "foo", the following values are reported:

* Gauge: name=foo

The value reported for the Gauge is the current value of the Coda Counter at flush time.

_Note: Librato Counters represent monotonically increasing values. Since Coda Counters can be incremented or decremented, it makes sense to report them as Librato Gauges._

### Coda Histograms

Given a Coda Histogram with name "foo", the following values are reported:

* ComplexGauge: name=foo (includes sum, count, min, max, average) See <a href="http://dev.librato.com/v1/post/metrics">extended gauge parameters</a>.
* Gauge: name=foo.median
* Gauge: name=foo.75th
* Gauge: name=foo.95th
* Gauge: name=foo.98th
* Gauge: name=foo.99th
* Gauge: name=foo.999th
* DeltaGauge: name=foo.count (represents the number of values the Coda Histogram has recorded)

_Note that Coda Histogram percentiles are determined using configurable <a href="http://metrics.codahale.com/manual/core/#histograms">Reservoir Sampling</a>. Histograms by default use a non-biased uniform reservoir._

### Coda Meters

Given a Coda Meter with name "foo", the following values are reported:

* DeltaGauge: name=foo.count (represents the number of values the Coda Meter has recorded)
* Gauge: name=foo.meanRate
* Gauge: name=foo.1MinuteRate
* Gauge: name=foo.5MinuteRate
* Gauge: name=foo.15MinuteRate

### Coda Timers

Given a Coda Timer with name "foo", the following values are reported:

* ComplexGauge: name=foo (includes sum, count, min, max, average) See <a href="http://dev.librato.com/v1/post/metrics">extended gauge parameters</a>.
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

### Librato DeltaGauges

Librato Gauges represent a particular value in time.  The concept of a DeltaGauge as used above submits the delta between the current value and the previous value as the value of the Librato Gauge. Because the delta cannot be calculated without a previous value, the reporter will omit a metric for which a DeltaGauge is used the first time that metric is seen.

