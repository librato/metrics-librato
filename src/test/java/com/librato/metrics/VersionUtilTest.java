package com.librato.metrics;

import org.junit.Assert;
import org.junit.Test;

public class VersionUtilTest {

    @Test
    public void testFindsTheVersion() throws Exception {
        final String version = VersionUtil.getVersion("com/librato/metrics/valid.pom.properties", VersionUtilTest.class);
        Assert.assertEquals("0.0.10", version);
    }

    @Test
    public void testDoesNotFindThePath() throws Exception {
        final String version = VersionUtil.getVersion("com/librato/metrics/does-not-exist", VersionUtilTest.class);
        Assert.assertEquals("unknown", version);
    }

    @Test
    public void testDoesNotFindTheVersion() throws Exception {
        final String version = VersionUtil.getVersion("com/librato/metrics/invalid.pom.properties", VersionUtilTest.class);
        Assert.assertEquals("unknown", version);
    }
}
