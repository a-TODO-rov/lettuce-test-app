package io.lettuce.test.reactive;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.types.Expiration;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An instrumented ReactiveStringRedisTemplate that wraps operations with metrics tracking.
 */
public class InstrumentedReactiveStringRedisTemplate extends ReactiveStringRedisTemplate {

    private static final Logger log = LoggerFactory.getLogger(InstrumentedReactiveStringRedisTemplate.class);

    private final MeterRegistry meterRegistry;

    private final Map<String, Timer> successTimers = new ConcurrentHashMap<>();

    private final Map<String, Timer> errorTimers = new ConcurrentHashMap<>();

    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();

    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();

    private volatile ReactiveValueOperations<String, String> instrumentedOps;

    public InstrumentedReactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory,
            MeterRegistry meterRegistry) {
        super(connectionFactory);
        this.meterRegistry = meterRegistry;
        log.info("InstrumentedReactiveStringRedisTemplate initialized with metrics tracking");
    }

    @Override
    public ReactiveValueOperations<String, String> opsForValue() {
        if (instrumentedOps == null) {
            instrumentedOps = new InstrumentedReactiveValueOperations(super.opsForValue());
        }
        return instrumentedOps;
    }

    private void recordSuccess(String command, Timer.Sample sample) {
        Timer timer = successTimers.computeIfAbsent(command, this::createSuccessTimer);
        sample.stop(timer);
        Counter counter = successCounters.computeIfAbsent(command, this::createSuccessCounter);
        counter.increment();
    }

    private void recordError(String command, Timer.Sample sample, Throwable ex) {
        Timer timer = errorTimers.computeIfAbsent(command, this::createErrorTimer);
        sample.stop(timer);
        Counter counter = errorCounters.computeIfAbsent(command, this::createErrorCounter);
        counter.increment();
    }

    private Timer createSuccessTimer(String command) {
        return Timer.builder("redis.operation.duration").tag("command", command).tag("status", "success")
                .description("Redis operation duration").publishPercentiles(0.5, 0.95, 0.99).register(meterRegistry);
    }

    private Timer createErrorTimer(String command) {
        return Timer.builder("redis.operation.duration").tag("command", command).tag("status", "error")
                .description("Redis operation duration").publishPercentiles(0.5, 0.95, 0.99).register(meterRegistry);
    }

    private Counter createSuccessCounter(String command) {
        return Counter.builder("redis.operations.count").tag("command", command).tag("status", "success")
                .description("Redis operation count").register(meterRegistry);
    }

    private Counter createErrorCounter(String command) {
        return Counter.builder("redis.operations.count").tag("command", command).tag("status", "error")
                .description("Redis operation count").register(meterRegistry);
    }

    /**
     * Instrumented wrapper for ReactiveValueOperations.
     */
    private class InstrumentedReactiveValueOperations implements ReactiveValueOperations<String, String> {

        private final ReactiveValueOperations<String, String> delegate;

        InstrumentedReactiveValueOperations(ReactiveValueOperations<String, String> delegate) {
            this.delegate = delegate;
        }

        // Instrumented methods (SET and GET)
        @Override
        public Mono<Boolean> set(String key, String value) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return delegate.set(key, value).doOnSuccess(v -> recordSuccess("SET", sample))
                    .doOnError(e -> recordError("SET", sample, e));
        }

        @Override
        public Mono<Boolean> set(String key, String value, Duration timeout) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return delegate.set(key, value, timeout).doOnSuccess(v -> recordSuccess("SET", sample))
                    .doOnError(e -> recordError("SET", sample, e));
        }

        @Override
        public Mono<Boolean> set(String key, String value, Expiration expiration) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return delegate.set(key, value, expiration).doOnSuccess(v -> recordSuccess("SET", sample))
                    .doOnError(e -> recordError("SET", sample, e));
        }

        @Override
        public Mono<String> get(Object key) {
            Timer.Sample sample = Timer.start(meterRegistry);
            return delegate.get(key).doOnSuccess(v -> recordSuccess("GET", sample))
                    .doOnError(e -> recordError("GET", sample, e));
        }

        // Delegated methods (non-instrumented for brevity)
        @Override
        public Mono<String> setGet(String key, String value, Expiration expiration) {
            return delegate.setGet(key, value, expiration);
        }

        @Override
        public Mono<String> setGet(String key, String value, Duration timeout) {
            return delegate.setGet(key, value, timeout);
        }

        @Override
        public Mono<Boolean> setIfAbsent(String key, String value) {
            return delegate.setIfAbsent(key, value);
        }

        @Override
        public Mono<Boolean> setIfAbsent(String key, String value, Expiration expiration) {
            return delegate.setIfAbsent(key, value, expiration);
        }

        @Override
        public Mono<Boolean> setIfAbsent(String key, String value, Duration timeout) {
            return delegate.setIfAbsent(key, value, timeout);
        }

        @Override
        public Mono<Boolean> setIfPresent(String key, String value) {
            return delegate.setIfPresent(key, value);
        }

        @Override
        public Mono<Boolean> setIfPresent(String key, String value, Expiration expiration) {
            return delegate.setIfPresent(key, value, expiration);
        }

        @Override
        public Mono<Boolean> setIfPresent(String key, String value, Duration timeout) {
            return delegate.setIfPresent(key, value, timeout);
        }

        @Override
        public Mono<Boolean> multiSet(Map<? extends String, ? extends String> map) {
            return delegate.multiSet(map);
        }

        @Override
        public Mono<Boolean> multiSetIfAbsent(Map<? extends String, ? extends String> map) {
            return delegate.multiSetIfAbsent(map);
        }

        @Override
        public Mono<String> getAndDelete(String key) {
            return delegate.getAndDelete(key);
        }

        @Override
        public Mono<String> getAndExpire(String key, Duration timeout) {
            return delegate.getAndExpire(key, timeout);
        }

        @Override
        public Mono<String> getAndPersist(String key) {
            return delegate.getAndPersist(key);
        }

        @Override
        public Mono<String> getAndSet(String key, String value) {
            return delegate.getAndSet(key, value);
        }

        @Override
        public Mono<List<String>> multiGet(Collection<String> keys) {
            return delegate.multiGet(keys);
        }

        @Override
        public Mono<Long> increment(String key) {
            return delegate.increment(key);
        }

        @Override
        public Mono<Long> increment(String key, long delta) {
            return delegate.increment(key, delta);
        }

        @Override
        public Mono<Double> increment(String key, double delta) {
            return delegate.increment(key, delta);
        }

        @Override
        public Mono<Long> decrement(String key) {
            return delegate.decrement(key);
        }

        @Override
        public Mono<Long> decrement(String key, long delta) {
            return delegate.decrement(key, delta);
        }

        @Override
        public Mono<Long> append(String key, String value) {
            return delegate.append(key, value);
        }

        @Override
        public Mono<String> get(String key, long start, long end) {
            return delegate.get(key, start, end);
        }

        @Override
        public Mono<Long> set(String key, String value, long offset) {
            return delegate.set(key, value, offset);
        }

        @Override
        public Mono<Long> size(String key) {
            return delegate.size(key);
        }

        @Override
        public Mono<Boolean> setBit(String key, long offset, boolean value) {
            return delegate.setBit(key, offset, value);
        }

        @Override
        public Mono<Boolean> getBit(String key, long offset) {
            return delegate.getBit(key, offset);
        }

        @Override
        public Mono<List<Long>> bitField(String key, BitFieldSubCommands subCommands) {
            return delegate.bitField(key, subCommands);
        }

        @Override
        public Mono<Boolean> delete(String key) {
            return delegate.delete(key);
        }

    }

}
