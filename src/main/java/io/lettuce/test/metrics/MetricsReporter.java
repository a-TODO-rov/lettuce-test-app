package io.lettuce.test.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reports metrics periodically to logs and generates a final JSON summary at shutdown.
 */
@Component
public class MetricsReporter {

    private static final Logger log = LoggerFactory.getLogger(MetricsReporter.class);

    private static final Logger metricsLog = LoggerFactory.getLogger("metrics-reporter");

    private static final Logger summaryLog = LoggerFactory.getLogger("simple-metrics-reporter");

    private final MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper;

    @Value("${metrics.dump.rate:PT5S}")
    private Duration dumpRate;

    @Value("${logging.file.path:logs}")
    private String logPath;

    @Value("${runner.test.workload.type:unknown}")
    private String workloadType;

    @Value("${spring.application.name:lettuce-test-app}")
    private String appName;

    private TaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledFuture;

    private Instant runStart;

    public MetricsReporter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void startScheduledDump() {
        runStart = Instant.now();

        // Create a task scheduler for periodic dumps
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("metrics-reporter-");
        scheduler.initialize();
        this.taskScheduler = scheduler;

        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::dumpMetrics, dumpRate);
        log.info("MetricsReporter started with dump rate: {}", dumpRate);
    }

    /**
     * Dumps current metrics snapshot to log.
     */
    public void dumpMetrics() {
        metricsLog.info("--- Metrics Snapshot ---");

        meterRegistry.getMeters().stream().sorted(Comparator.comparing(m -> m.getId().getName())).forEach(this::logMeter);

        metricsLog.info("--- End Snapshot ---");
    }

    /**
     * Dumps summary metrics for periodic summary logging.
     */
    public void dumpSummary() {
        summaryLog.info("--- Metric Summary ---");

        meterRegistry.getMeters().stream().sorted(Comparator.comparing(m -> m.getId().getName())).forEach(meter -> {
            String name = meter.getId().getName();
            String tags = meter.getId().getTags().toString();

            if (meter instanceof Counter counter) {
                summaryLog.info("Counter: {} {} value: {}", name, tags, counter.count());
            } else if (meter instanceof Timer timer) {
                summaryLog.info("Timer: {} {} count: {} total: {}ms", name, tags, timer.count(),
                        timer.totalTime(TimeUnit.MILLISECONDS));
            } else if (meter instanceof Gauge gauge) {
                summaryLog.info("Gauge: {} {} value: {}", name, tags, gauge.value());
            }
        });
    }

    private void logMeter(Meter meter) {
        String name = meter.getId().getName();
        String tags = meter.getId().getTags().toString();

        if (meter instanceof Counter counter) {
            metricsLog.info("Counter: MeterId{{name='{}', tags={}}} value: {}", name, tags, counter.count());
        } else if (meter instanceof Timer timer) {
            double throughput = timer.count() / Math.max(1, dumpRate.toSeconds());
            metricsLog.info("Timer: MeterId{{name='{}', tags={}}} count: {} throughput: {}/s mean: {}ms max: {}ms", name, tags,
                    timer.count(), String.format("%.2f", throughput), String.format("%.3f", timer.mean(TimeUnit.MILLISECONDS)),
                    String.format("%.3f", timer.max(TimeUnit.MILLISECONDS)));
        } else if (meter instanceof Gauge gauge) {
            metricsLog.info("Gauge: MeterId{{name='{}', tags={}}} value: {}", name, tags, gauge.value());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("MetricsReporter is shutting down...");

        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        // Shutdown the task scheduler to allow JVM exit
        if (taskScheduler instanceof ThreadPoolTaskScheduler threadPoolScheduler) {
            threadPoolScheduler.shutdown();
        }

        // Final dumps
        dumpMetrics();
        dumpSummary();
        writeFinalSummary();

        log.info("MetricsReporter stopped");
    }

    private void writeFinalSummary() {
        try {
            ObjectNode summary = buildSummaryJson();
            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);

            log.info("=== FINAL TEST RESULTS ===");
            log.info(jsonResult);
            log.info("=== END FINAL TEST RESULTS ===");

            writeToFile(jsonResult);
        } catch (Exception e) {
            log.error("Error generating final result", e);
        }
    }

    private ObjectNode buildSummaryJson() {
        ObjectNode result = objectMapper.createObjectNode();
        Instant runEnd = Instant.now();

        // Scenario information (detected from system properties set by Maven profiles)
        result.put("dependency_profile", detectDependencyProfile());
        result.put("runner_mode", detectRunnerMode());
        result.put("reactor_available", isReactorAvailable());

        // Basic run information
        result.put("app_name", appName);
        result.put("run_id", generateRunId());
        result.put("workload_name", workloadType);
        result.put("run_start", runStart.getEpochSecond());
        result.put("run_end", runEnd.getEpochSecond());
        result.put("duration_seconds", Duration.between(runStart, runEnd).toSeconds());

        // Command counts
        OperationStats stats = getOperationStats();
        result.put("total_commands_count", stats.totalCommands);
        result.put("successful_commands_count", stats.successfulCommands);
        result.put("failed_commands_count", stats.failedCommands);
        result.put("success_rate", String.format("%.2f%%",
                stats.totalCommands > 0 ? (stats.successfulCommands * 100.0 / stats.totalCommands) : 0.0));

        // Latency statistics
        LatencyStats latency = getLatencyStats();
        result.put("min_latency_ms", latency.min);
        result.put("max_latency_ms", latency.max);
        result.put("median_latency_ms", latency.median);
        result.put("p95_latency_ms", latency.p95);
        result.put("p99_latency_ms", latency.p99);

        // Connection events
        result.put("reconnect_attempts", getCounterValue("redis.reconnect.attempts"));
        result.put("reconnect_failures", getCounterValue("redis.reconnect.failures"));

        return result;
    }

    private String generateRunId() {
        return workloadType + "-" + runStart.toEpochMilli();
    }

    private String detectDependencyProfile() {
        // -Dstandard is set when using standard profile
        return System.getProperty("standard") != null ? "standard" : "reactor-optional";
    }

    private String detectRunnerMode() {
        // -Dreactive is set when using reactive profile
        return System.getProperty("reactive") != null ? "reactive" : "sync";
    }

    private boolean isReactorAvailable() {
        try {
            Class.forName("reactor.core.publisher.Mono");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    record OperationStats(long totalCommands, long successfulCommands, long failedCommands) {
    }

    private OperationStats getOperationStats() {
        long successful = 0;
        long failed = 0;

        Collection<Counter> counters = meterRegistry.find("redis.operations.count").counters();
        for (Counter counter : counters) {
            String status = counter.getId().getTag("status");
            if ("success".equals(status)) {
                successful += (long) counter.count();
            } else if ("error".equals(status)) {
                failed += (long) counter.count();
            }
        }

        return new OperationStats(successful + failed, successful, failed);
    }

    record LatencyStats(double min, double max, double median, double p95, double p99) {
    }

    private LatencyStats getLatencyStats() {
        double min = 0, max = 0, median = -1, p95 = -1, p99 = -1;

        Collection<Timer> timers = meterRegistry.find("redis.operation.duration").timers();
        for (Timer timer : timers) {
            max = Math.max(max, timer.max(TimeUnit.MILLISECONDS));

            HistogramSnapshot snapshot = timer.takeSnapshot();
            for (ValueAtPercentile p : snapshot.percentileValues()) {
                if (p.percentile() == 0.5) {
                    median = Math.max(median, p.value(TimeUnit.MILLISECONDS));
                } else if (p.percentile() == 0.95) {
                    p95 = Math.max(p95, p.value(TimeUnit.MILLISECONDS));
                } else if (p.percentile() == 0.99) {
                    p99 = Math.max(p99, p.value(TimeUnit.MILLISECONDS));
                }
            }
        }

        return new LatencyStats(min, max, median, p95, p99);
    }

    private double getCounterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0;
    }

    private void writeToFile(String jsonResult) {
        try {
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            Path summaryFile = logDir.resolve("test-run-summary.json");
            Files.writeString(summaryFile, jsonResult);
            log.info("Final test results written to: {}", summaryFile.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write final results to file", e);
        }
    }

}
