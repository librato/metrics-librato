package com.librato.metrics.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds information about a metric -- name and source. This is derived from a static method
 * which uses a regular expression to pull source and metric name out of a codahale metric name.
 */
public class SourceInformation {
    static final Logger log = LoggerFactory.getLogger(SourceInformation.class);
    /**
     * If the pattern is not null, it will attempt to match against the supplied name to pull out the source.
     */
    public static SourceInformation from(Pattern sourceRegex, String name) {
        if (sourceRegex == null) {
            return new SourceInformation(null, name);
        }
        Matcher matcher = sourceRegex.matcher(name);
        if (matcher.groupCount() != 1) {
            log.error("Source regex matcher must define a group");
            return new SourceInformation(null, name);
        }
        if (!matcher.find()) {
            return new SourceInformation(null, name);
        }
        String source = matcher.group(1);
        int endPos = matcher.toMatchResult().end();
        if (endPos >= name.length()) {
            // the source matched the whole metric name, probably in error.
            log.error("Source '{}' matched the whole metric name. Metric name cannot be empty");
            return new SourceInformation(null, name);
        }
        String newName = name.substring(endPos);
        return new SourceInformation(source, newName);
    }

    public final String source;
    public final String name;

    public SourceInformation(String source, String name) {
        this.source = source;
        this.name = name;
    }
}
