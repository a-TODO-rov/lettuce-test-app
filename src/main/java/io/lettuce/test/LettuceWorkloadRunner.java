package io.lettuce.test;

import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;
import io.lettuce.test.workloads.GetSetWorkload;
import io.lettuce.test.workloads.RedisCommandsWorkload;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class LettuceWorkloadRunner {

    private static final Logger log = LoggerFactory.getLogger(LettuceWorkloadRunner.class);

    private final WorkloadRunnerConfig config;

    private final StringRedisTemplate redisTemplate;

    private ExecutorService executor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public LettuceWorkloadRunner(WorkloadRunnerConfig config, StringRedisTemplate redisTemplate) {
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    public void run() {
        log.info("Starting workload runner with config: {}", config);

        int workers = config.getTest().getConcurrentWorkers();
        executor = Executors.newFixedThreadPool(workers);
        running.set(true);

        WorkloadConfig workloadConfig = config.getTest().getWorkload();
        log.info("Running {} concurrent workers with workload type: {}", workers, workloadConfig.getType());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            final int workerId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                log.info("Worker {} started", workerId);
                runWorkloadLoop(workloadConfig, workerId);
                log.info("Worker {} completed", workerId);
            }, executor);
            futures.add(future);
        }

        // Wait for all workers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("All workers completed");
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
                log.debug("Worker {} completed iteration {}", workerId, iterationCount);
            } catch (Exception e) {
                log.error("Worker {} encountered error in iteration {}", workerId, iterationCount, e);
            }
        }

        log.info("Worker {} finished after {} iterations", workerId, iterationCount);
    }

    private BaseWorkload createWorkload(WorkloadConfig workloadConfig) {
        CommonWorkloadOptions options = DefaultWorkloadOptions.create(workloadConfig.getOptions());

        return switch (workloadConfig.getType().toLowerCase()) {
            case "get_set" -> new GetSetWorkload(redisTemplate, options);
            case "redis_commands" -> new RedisCommandsWorkload(redisTemplate, options);
            default -> throw new IllegalArgumentException("Unknown workload type: " + workloadConfig.getType());
        };
    }

    @PreDestroy
    private void shutdown() {
        log.info("Shutting down...");
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
