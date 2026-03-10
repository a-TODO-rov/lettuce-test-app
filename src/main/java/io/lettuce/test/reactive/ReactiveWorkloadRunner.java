package io.lettuce.test.reactive;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.DefaultWorkloadOptions;
import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive variant of LettuceWorkloadRunner.
 * <p>
 * Uses ReactiveStringRedisTemplate to exercise the reactive path in Spring Data Redis. This validates that SDR works correctly
 * with Lettuce's reactor-optional changes.
 * <p>
 * This component is only loaded when Reactor is on the classpath.
 */
@Component("reactiveWorkloadRunner")
@ConditionalOnClass(name = "reactor.core.publisher.Flux")
public class ReactiveWorkloadRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ReactiveWorkloadRunner.class);

    private final WorkloadRunnerConfig config;

    private final ReactiveStringRedisTemplate reactiveTemplate;

    private ExecutorService executor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public ReactiveWorkloadRunner(WorkloadRunnerConfig config, ReactiveStringRedisTemplate reactiveTemplate) {
        this.config = config;
        this.reactiveTemplate = reactiveTemplate;
    }

    public void run() {
        log.info("Starting REACTIVE workload runner with config: {}", config);

        int workers = config.getTest().getConcurrentWorkers();
        executor = Executors.newFixedThreadPool(workers);
        running.set(true);

        WorkloadConfig workloadConfig = config.getTest().getWorkload();
        log.info("Running {} concurrent REACTIVE workers with workload type: {}", workers, workloadConfig.getType());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            final int workerId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                log.info("Reactive worker {} started", workerId);
                runWorkloadLoop(workloadConfig, workerId);
                log.info("Reactive worker {} completed", workerId);
            }, executor);
            futures.add(future);
        }

        // Wait for all workers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("All reactive workers completed");
    }

    private void runWorkloadLoop(WorkloadConfig workloadConfig, int workerId) {
        long startTime = System.currentTimeMillis();
        long maxDurationMs = workloadConfig.getMaxDuration().toMillis();
        int iterationCount = 0;

        while (running.get() && (System.currentTimeMillis() - startTime) < maxDurationMs) {
            try {
                BaseWorkload workload = createWorkload(workloadConfig);
                workload.run();
                iterationCount++;
                log.debug("Reactive worker {} completed iteration {}", workerId, iterationCount);
            } catch (Exception e) {
                log.error("Reactive worker {} encountered error in iteration {}", workerId, iterationCount, e);
            }
        }

        log.info("Reactive worker {} finished after {} iterations", workerId, iterationCount);
    }

    private BaseWorkload createWorkload(WorkloadConfig workloadConfig) {
        CommonWorkloadOptions options = DefaultWorkloadOptions.create(workloadConfig.getOptions());

        return switch (workloadConfig.getType().toLowerCase()) {
            case "get_set", "reactive_get_set" -> new ReactiveGetSetWorkload(reactiveTemplate, options);
            default -> throw new IllegalArgumentException(
                    "Unknown reactive workload type: " + workloadConfig.getType() + ". Supported: get_set, reactive_get_set");
        };
    }

    @PreDestroy
    private void shutdown() {
        log.info("Shutting down reactive workload runner...");
        running.set(false);

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

}
