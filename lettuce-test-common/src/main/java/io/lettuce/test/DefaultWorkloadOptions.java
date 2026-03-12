package io.lettuce.test;

import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_DELAY_AFTER_ITERATION;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_DELAY_AFTER_WORKLOAD;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_ELEMENTS_COUNT;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_GET_SET_RATIO;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_ITERATION_COUNT;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_TRANSACTION_SIZE;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_VALUE_SIZE;

public class DefaultWorkloadOptions implements CommonWorkloadOptions {

    public static final DefaultWorkloadOptions DEFAULT = new DefaultWorkloadOptions();

    private Map<String, String> options = Collections.emptyMap();

    // Size of the tests values
    private final int valueSize;

    // Number of elements to operate on
    // e.g. How many elements to push with single `lpush` command
    private final int elementsCount;

    // Number of iterations to execute
    // How many times to repeat block of commands.
    private final int iterationCount;

    // Artificial delay after each iteration
    private final Duration delayAfterIteration;

    // Artificial delay after each workload run
    private final Duration delayAfterWorkload;

    // Ratio of GET to SET operations
    // e.g. On each iteration, choose randomly between GET and SET operations based on this ratio
    // value between 0 and 1
    // 0.2 means 20% of operations are SET, 80% are GET
    private final double getSetRatio;

    // Number of commands to execute per iteration
    // e.g. How many commands to execute in a single multi-exec transaction
    private final int transactionSize;

    /**
     * Creates a new instance of {@link DefaultWorkloadOptions} with the specified options.
     *
     * An empty map is used. Default values are applied for known common options, and 'null' is returned for any missing
     * options.
     */
    public DefaultWorkloadOptions() {
        this(Collections.emptyMap());
    }

    /**
     * Creates a new instance of {@link DefaultWorkloadOptions} with the specified options.
     *
     * If the provided options map is null, an empty map is used. Default values are applied for known common options, and
     * 'null' is returned for any missing options.
     *
     * @param options a map of options
     */
    public DefaultWorkloadOptions(@Nullable Map<String, String> options) {

        this.options = Collections.unmodifiableMap(Optional.ofNullable(options).orElse(Collections.emptyMap()));

        this.iterationCount = getInteger("iterationCount", DEFAULT_ITERATION_COUNT);
        this.valueSize = getInteger("valueSize", DEFAULT_VALUE_SIZE);
        this.elementsCount = getInteger("elementsCount", DEFAULT_ELEMENTS_COUNT);
        this.getSetRatio = getDouble("getSetRatio", DEFAULT_GET_SET_RATIO);
        this.transactionSize = getInteger("transactionSize", DEFAULT_TRANSACTION_SIZE);
        this.delayAfterIteration = getDuration("delayAfterIteration", DEFAULT_DELAY_AFTER_ITERATION);
        this.delayAfterWorkload = getDuration("delayAfterWorkload", DEFAULT_DELAY_AFTER_WORKLOAD);

    }

    @Override
    public <T> T getOption(String key, Class<T> type) {
        String value = options.get(key);
        if (value == null) {
            return null; // Option not found
        }

        return convertValue(value, type);
    }

    @Override
    public <T> T getOption(String key, Class<T> type, T defaultValue) {
        return options.containsKey(key) ? getOption(key, type) : defaultValue;
    }

    @Override
    public int valueSize() {
        return valueSize;
    }

    @Override
    public int elementsCount() {
        return elementsCount;
    }

    @Override
    public int iterationCount() {
        return iterationCount;
    }

    @Override
    public Duration delayAfterIteration() {
        return delayAfterIteration;
    }

    @Override
    public Duration delayAfterWorkload() {
        return delayAfterWorkload;
    }

    @Override
    public double getSetRatio() {
        return getSetRatio;
    }

    @Override
    public int transactionSize() {
        return transactionSize;
    }

    public static CommonWorkloadOptions create(Map<String, String> options) {
        return new DefaultWorkloadOptions(options);
    }

    @Override
    public String toString() {
        return "DefaultWorkloadOptions{" + "options=" + options + '}';
    }

    private <T> T convertValue(String value, Class<T> type) {
        if (type == Integer.class) {
            return type.cast(Integer.parseInt(value));
        } else if (type == Double.class) {
            return type.cast(Double.parseDouble(value));
        } else if (type == String.class) {
            return type.cast(value);
        } else if (type == Boolean.class) {
            return type.cast(Boolean.valueOf(value));
        } else if (type == Duration.class) {
            try {
                return type.cast(Duration.parse(value));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid duration format: " + value, e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static class WorkloadOptionsConstants {

        public static final double DEFAULT_GET_SET_RATIO = 0.5;

        public static final int DEFAULT_VALUE_SIZE = 100;

        public static final int DEFAULT_ELEMENTS_COUNT = 1;

        public static final int DEFAULT_ITERATION_COUNT = 1000;

        public static final int DEFAULT_TRANSACTION_SIZE = 100;

        public static final Duration DEFAULT_DELAY_AFTER_ITERATION = Duration.ZERO;

        public static final Duration DEFAULT_DELAY_AFTER_WORKLOAD = Duration.ZERO;

        public static final String DEFAULT_KEY_PATTERN = "key-%d";

        public static final String DEFAULT_KEY_GENERATION_STRATEGY = "RANDOM";

        public static final int DEFAULT_KEY_RANGE_MIN = 0;

        public static final int DEFAULT_KEY_RANGE_MAX = 10000;

    }

}
