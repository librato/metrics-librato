package com.yammer.metrics.librato;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


/**
 * copied from the ExampleRunner in metrics-core
 */
public class DummyApp {

    public static class DirectoryLister {
        private final MetricsRegistry registry = Metrics.defaultRegistry();
        private final Counter counter = registry.newCounter(getClass(), "directories");
        private final Meter meter = registry.newMeter(getClass(), "files", "files", TimeUnit.SECONDS);
        private final Timer timer = registry.newTimer(getClass(),
                "directory-listing",
                TimeUnit.MILLISECONDS,
                TimeUnit.SECONDS);
        private final File directory;

        public DirectoryLister(File directory) {
            this.directory = directory;
        }

        public List<File> list() throws Exception {
            counter.inc();
            final File[] list = timer.time(new Callable<File[]>() {
                @Override
                public File[] call() throws Exception {
                    return directory.listFiles();
                }
            });
            counter.dec();

            if (list == null) {
                return Collections.emptyList();
            }

            final List<File> result = new ArrayList<File>(list.length);
            for (File file : list) {
                meter.mark();
                result.add(file);
            }
            return result;
        }
    }


    private static final int WORKER_COUNT = 10;
    private static final BlockingQueue<File> JOBS = new LinkedBlockingQueue<File>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(WORKER_COUNT);
    private static final MetricsRegistry REGISTRY = Metrics.defaultRegistry();
    private static final Counter QUEUE_DEPTH = REGISTRY.newCounter(DummyApp.class, "queue-depth");
    private static final Histogram DIRECTORY_SIZE = REGISTRY.newHistogram(DummyApp.class, "directory-size", false);

    public static class Job implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    final File file = JOBS.poll(1, TimeUnit.MINUTES);
                    QUEUE_DEPTH.dec();
                    if (file.isDirectory()) {
                        final List<File> contents = new DirectoryLister(file).list();
                        DIRECTORY_SIZE.update(contents.size());
                        QUEUE_DEPTH.inc(contents.size());
                        JOBS.addAll(contents);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws Exception {
        LibratoReporter.enable(LibratoReporter.builder("", "", "testing").setReportVmMetrics(false), 10, TimeUnit.SECONDS);

        System.err.println("Scanning all files on your hard drive...");

        JOBS.add(new File("/"));
        QUEUE_DEPTH.inc();
        for (int i = 0; i < WORKER_COUNT; i++) {
            POOL.submit(new Job());
        }

        POOL.awaitTermination(30, TimeUnit.DAYS);
    }
}
