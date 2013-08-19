## Librato Metrics Plugin for the Metrics Library

The `LibratoReporter` class runs in the background, publishing metrics from <a href="https://github.com/codahale/metrics">codahale/metrics</a> to the <a href="http://metrics.librato.com">Librato Metrics API</a> at the specified interval.

    <dependency>
        <groupId>com.librato.metrics</groupId>
        <artifactId>metrics-librato</artifactId>
        <version>2.1.2.4</version>
    </dependency>

## Usage

During the initialization of your program, simply use the `.enable` method with the appropriately configured `LibratoReporter.Builder` class. See the setters on that method for all the available customizations (there are quite a few). The constructor for the `Builder` requires only the things that are necessary; sane defaults are provided for the rest of the options.

    LibratoReporter.enable(LibratoReporter.builder("<Librato Username>", "<Librato API Token>", "<Source Identifier (usually hostname)>"), 10, TimeUnit.SECONDS);

## Versioning

Since this project depends heavily on `metrics-core`, the version number will be `<version of metrics-core being depended on>.<release number of the reporter>`. Thus, the first release will be version 2.1.2.0, as 2.1.2 is the current stable version of `metrics-core`. If this proves impractical, a different versioning scheme will be investigated.
