package com.librato.metrics.reporter;

import com.librato.metrics.client.LibratoClient;
import com.librato.metrics.client.LibratoClientBuilder;

public class DefaultLibratoClientFactory implements ILibratoClientFactory {
    public LibratoClient build(ReporterAttributes atts) {
        LibratoClientBuilder builder = LibratoClient.builder(atts.email, atts.token)
                .setURI(atts.url)
                .setAgentIdentifier(Agent.AGENT_IDENTIFIER);
        if (atts.readTimeout != null) {
            builder.setReadTimeout(atts.readTimeout);
        }
        if (atts.connectTimeout != null) {
            builder.setConnectTimeout(atts.connectTimeout);
        }
        return builder.build();
    }
}
