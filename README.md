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

Coda's metrics will be translated to Librato metrics in the following manner:

### Gauges

Given a Coda Counter with name "foo", the following values are reported:

* Gauge: name=foo


### Counters

Given a Coda Counter with name "foo", the following values are reported:

* Gauge: name=foo

Note: Librato Counters represent monotonically increasing values. Since Coda Counters can be incremented or decremented, it makes sense to report them as Librato Gauges.

### Histograms

Given a Coda Histogram with name "foo", the following values are reported:

* Gauge: name=foo [includes sum, count, min, max, average]
* Gauge: name=foo.median
* Gauge: name=foo.75th
* Gauge: name=foo.95th
* Gauge: name=foo.98th
* Gauge: name=foo.99th
* Gauge: name=foo.999th

### Meters

Given a Coda Meter with name "foo", the following values are reported:

* Gauge: name=foo.count (reported as delta from previous value)
* Gauge: name=foo.meanRate
* Gauge: name=foo.1MinuteRate
* Gauge: name=foo.5MinuteRate
* Gauge: name=foo.15MinuteRate

### Timers

Given a Coda Timer with name "foo", the following values are reported:

* Gauge: name=foo [includes sum, count, min, max, average]
* Gauge: name=foo.median
* Gauge: name=foo.75th
* Gauge: name=foo.95th
* Gauge: name=foo.98th
* Gauge: name=foo.99th
* Gauge: name=foo.999th
* Gauge: name=foo.count (reported as delta from previous value)
* Gauge: name=foo.meanRate
* Gauge: name=foo.1MinuteRate
* Gauge: name=foo.5MinuteRate
* Gauge: name=foo.15MinuteRate




