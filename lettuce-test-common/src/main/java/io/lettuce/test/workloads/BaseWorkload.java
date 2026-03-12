package io.lettuce.test.workloads;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.DefaultWorkloadOptions;
import io.lettuce.test.generator.KeyGenerator;
import io.lettuce.test.generator.RandomKeyGenerator;
import io.lettuce.test.generator.SequentialKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_GENERATION_STRATEGY;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_PATTERN;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_RANGE_MAX;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_RANGE_MIN;

/**
 * Base class for workloads.
 * <p>
 * Workloads are executed by the workload runner. Workload implementations are not thread safe and should not be shared between
 * threads.
 */
public abstract class BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(BaseWorkload.class);

    private final CommonWorkloadOptions options;

    private final KeyGenerator keyGenerator;

    public BaseWorkload() {
        options = DefaultWorkloadOptions.DEFAULT;
        keyGenerator = createKeyGenerator(options);
    }

    public BaseWorkload(CommonWorkloadOptions options) {
        this.options = options;
        this.keyGenerator = createKeyGenerator(options);
    }

    public abstract void run();

    public CommonWorkloadOptions options() {
        return options;
    }

    protected void delay(Duration delay) {
        if (delay == null || Duration.ZERO.equals(delay)) {
            return;
        }

        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            log.warn("Delay interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    protected KeyGenerator keyGenerator() {
        return keyGenerator;
    }

    private KeyGenerator createKeyGenerator(CommonWorkloadOptions options) {
        String keyGenerationStrategy = options.getString("keyGenerationStrategy", DEFAULT_KEY_GENERATION_STRATEGY);
        String pattern = options.getString("keyPattern", DEFAULT_KEY_PATTERN);
        Integer rangeMin = options.getInteger("keyRangeMin", DEFAULT_KEY_RANGE_MIN);
        Integer rangeMax = options.getInteger("keyRangeMax", DEFAULT_KEY_RANGE_MAX);

        return switch (keyGenerationStrategy.toUpperCase()) {
            case "SEQUENTIAL" -> new SequentialKeyGenerator(pattern, rangeMin, rangeMax);
            case "RANDOM" -> new RandomKeyGenerator(pattern, rangeMin, rangeMax);
            default -> throw new IllegalArgumentException("Unknown key generation strategy: " + keyGenerationStrategy);
        };
    }

}
