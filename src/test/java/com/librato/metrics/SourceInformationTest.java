package com.librato.metrics;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.regex.Pattern;

public class SourceInformationTest extends TestCase {

    public void testHandlesNullRegex() throws Exception {
        SourceInformation info = SourceInformation.from(null, "foo");
        Assert.assertNull(info.source);
        Assert.assertEquals("foo", info.name);
    }

    public void testExtractsSource() throws Exception {
        Pattern pattern = Pattern.compile("^([^\\.]+)\\.");
        SourceInformation info = SourceInformation.from(pattern, "foo.bar");
        Assert.assertEquals("foo", info.source);
        Assert.assertEquals("bar", info.name);
    }

    public void testRequiresAMatchingGroup() throws Exception {
        Pattern pattern = Pattern.compile("^[^\\.]+\\.");
        SourceInformation info = SourceInformation.from(pattern, "foo.bar");
        Assert.assertNull(info.source);
        Assert.assertEquals("foo.bar", info.name);
    }

    public void testPassesThroughNonMatchingMetricNames() throws Exception {
        Pattern pattern = Pattern.compile("^([^\\.]+)\\.");
        SourceInformation info = SourceInformation.from(pattern, "foo-bar");
        Assert.assertNull(info.source);
        Assert.assertEquals("foo-bar", info.name);
    }
}
