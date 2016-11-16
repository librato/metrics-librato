package com.librato.metrics.reporter;

import com.librato.metrics.client.LibratoClient;

public interface ILibratoClientFactory {
    LibratoClient build(ReporterAttributes atts);
}
