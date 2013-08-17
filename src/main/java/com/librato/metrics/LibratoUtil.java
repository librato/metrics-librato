package com.librato.metrics;

import com.librato.metrics.LibratoBatch;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.VirtualMachineMetrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * User: mihasya
 * Date: 6/14/12
 * Time: 2:05 PM
 * keeping general Librato utilities out of the way
 */
public class LibratoUtil {
	private LibratoUtil() {
		// do not instantiate; static utility class
	}

    /**
     * turn a MetricName into a Librato-able string key
     */
    public static String nameToString(MetricName name) {
        StringBuilder builder = new StringBuilder();
        builder
                .append(name.getGroup()).append(".")
                .append(name.getType()).append(".")
                .append(name.getName());

        if (name.hasScope()) {
            builder.append(".").append(name.getScope());
        }

        return builder.toString();
    }

    /**
     * helper method for adding VM metrics to a batch
     */
    public static void addVmMetricsToBatch(VirtualMachineMetrics vm, LibratoBatch batch) {
        // memory
        batch.addGaugeMeasurement("jvm.memory.heap_usage", vm.heapUsage());
        batch.addGaugeMeasurement("jvm.memory.non_heap_usage", vm.nonHeapUsage());
        for (Map.Entry<String, Double> pool : vm.memoryPoolUsage().entrySet()) {
            batch.addGaugeMeasurement("jvm.memory.memory_pool_usages."+pool.getKey(), pool.getValue());
        }

        // threads
        batch.addGaugeMeasurement("jvm.daemon_thread_count", vm.daemonThreadCount());
        batch.addGaugeMeasurement("jvm.thread_count", vm.threadCount());
        batch.addGaugeMeasurement("jvm.uptime", vm.uptime());
        batch.addGaugeMeasurement("jvm.fd_usage", vm.fileDescriptorUsage());

        for (Map.Entry<Thread.State, Double> entry : vm.threadStatePercentages().entrySet()) {
            batch.addGaugeMeasurement("jvm.thread-states." + entry.getKey().toString().toLowerCase(), entry.getValue());
        }

        // garbage collection
        for (Map.Entry<String, VirtualMachineMetrics.GarbageCollectorStats> entry : vm.garbageCollectors().entrySet()) {
            final String name = "jvm.gc." + entry.getKey();
            batch.addCounterMeasurement(name +".time", entry.getValue().getTime(TimeUnit.MILLISECONDS));
            batch.addCounterMeasurement(name +".runs", entry.getValue().getRuns());
        }
    }
}
