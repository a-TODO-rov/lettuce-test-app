package io.lettuce.test.reactive;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reactive variant of GetSetWorkload using ReactiveStringRedisTemplate.
 * <p>
 * This exercises the reactive path in Spring Data Redis which requires the SDR reactor-optional fixes to work with Lettuce
 * 7.5.0-REACTOR-OPTIONAL.
 */
public class ReactiveGetSetWorkload extends io.lettuce.test.workloads.BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(ReactiveGetSetWorkload.class);

    private final ReactiveStringRedisTemplate reactiveTemplate;

    public ReactiveGetSetWorkload(ReactiveStringRedisTemplate reactiveTemplate, CommonWorkloadOptions options) {
        super(options);
        this.reactiveTemplate = reactiveTemplate;
    }

    @Override
    public void run() {
        ReactiveValueOperations<String, String> ops = reactiveTemplate.opsForValue();
        Random random = new Random();
        String payload = PayloadUtils.randomString(options().valueSize());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        log.info("Starting ReactiveGetSetWorkload with {} iterations, getSetRatio={}", options().iterationCount(),
                options().getSetRatio());

        // Create a flux of operations
        Flux.range(0, options().iterationCount()).flatMap(i -> {
            String key = keyGenerator().nextKey();
            Mono<?> operation;
            if (random.nextDouble() < options().getSetRatio()) {
                operation = ops.set(key, payload);
            } else {
                operation = ops.get(key);
            }
            return operation.doOnSuccess(v -> successCount.incrementAndGet()).doOnError(e -> {
                errorCount.incrementAndGet();
                log.error("Reactive operation failed: {}", e.getMessage());
            }).onErrorResume(e -> Mono.empty());
        }, 16) // Concurrency of 16
                .then().block(Duration.ofMinutes(5)); // Block until complete with timeout

        log.info("ReactiveGetSetWorkload completed: {} success, {} errors", successCount.get(), errorCount.get());
    }

}
