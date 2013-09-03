package com.librato.metrics;

import com.yammer.metrics.core.MetricName;
import org.junit.Assert;
import org.junit.Test;

public class LibratoUtilTest {
    @Test
    public void testNameToStringWithScope() throws Exception {
        final MetricName metricName = new MetricName(LibratoUtilTest.class, "name", "production");
        final String string = LibratoUtil.nameToString(metricName);
        Assert.assertEquals("com.librato.metrics.LibratoUtilTest.name.production", string);
    }

    @Test
    public void testNameToStringWithoutScope() throws Exception {
        final MetricName metricName = new MetricName(LibratoUtilTest.class, "name");
        final String string = LibratoUtil.nameToString(metricName);
        Assert.assertEquals("com.librato.metrics.LibratoUtilTest.name", string);
    }
}
