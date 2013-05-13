package com.librato.metrics;

import com.codahale.metrics.*;

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
        private final MetricRegistry metrics;
        private final Counter counter;
        private final Meter meter;
        private final Timer timer;
        private final File directory;

        public DirectoryLister(File directory, MetricRegistry metricRegistry) {
            this.directory = directory;
            this.metrics = metricRegistry;
            this.counter = metrics.counter(MetricRegistry.name(getClass(), "directories"));
            this.meter = metrics.meter(MetricRegistry.name(getClass(), "files", "files"));
            this.timer = metrics.timer(MetricRegistry.name(getClass(), "directory-listing"));
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
    private static final MetricRegistry REGISTRY = new MetricRegistry();
    private static final Counter QUEUE_DEPTH = REGISTRY.counter(MetricRegistry.name(DummyApp.class, "queue-depth"));
    private static final Histogram DIRECTORY_SIZE = REGISTRY.histogram(MetricRegistry.name(DummyApp.class, "directory-size"));

    public static class Job implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    final File file = JOBS.poll(1, TimeUnit.MINUTES);
                    QUEUE_DEPTH.dec();
                    if (file.isDirectory()) {
                        final List<File> contents = new DirectoryLister(file, REGISTRY).list();
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
        String username = System.getProperty("librato.user", "");
        String token = System.getProperty("librato.token", "");
        LibratoReporter.enable(LibratoReporter.builder(REGISTRY, username, token, "testing"), 10, TimeUnit.SECONDS);

        System.err.println("Scanning all files on your hard drive...");

        JOBS.add(new File("/"));
        QUEUE_DEPTH.inc();
        for (int i = 0; i < WORKER_COUNT; i++) {
            POOL.submit(new Job());
        }

        POOL.awaitTermination(30, TimeUnit.DAYS);
    }
}
