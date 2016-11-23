package com.librato.metrics;


import com.librato.metrics.client.LibratoClient;

public interface ILibratoClientFactory {
    LibratoClient build(ReporterAttributes atts);
}

