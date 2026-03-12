package io.lettuce.test.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An instrumented StringRedisTemplate that wraps operations with metrics tracking. Uses dynamic proxies to time and count all
 * Redis operations.
 */
public class InstrumentedStringRedisTemplate extends StringRedisTemplate {

    private static final Logger log = LoggerFactory.getLogger(InstrumentedStringRedisTemplate.class);

    private final MeterRegistry meterRegistry;

    // Cache timers and counters to avoid repeated lookups
    private final Map<String, Timer> successTimers = new ConcurrentHashMap<>();

    private final Map<String, Timer> errorTimers = new ConcurrentHashMap<>();

    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();

    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();

    // Maps Spring Data method names to Redis command names
    private static final Map<String, String> COMMAND_MAP = Map.ofEntries(
            // ValueOperations
            Map.entry("set", "SET"), Map.entry("get", "GET"), Map.entry("setIfAbsent", "SETNX"),
            Map.entry("setIfPresent", "SET"), Map.entry("getAndSet", "GETSET"), Map.entry("increment", "INCR"),
            Map.entry("decrement", "DECR"), Map.entry("append", "APPEND"), Map.entry("size", "STRLEN"),
            Map.entry("multiGet", "MGET"), Map.entry("multiSet", "MSET"),
            // ListOperations
            Map.entry("leftPush", "LPUSH"), Map.entry("leftPushAll", "LPUSH"), Map.entry("rightPush", "RPUSH"),
            Map.entry("rightPushAll", "RPUSH"), Map.entry("leftPop", "LPOP"), Map.entry("rightPop", "RPOP"),
            Map.entry("range", "LRANGE"), Map.entry("trim", "LTRIM"), Map.entry("index", "LINDEX"),
            // RedisTemplate
            Map.entry("delete", "DEL"), Map.entry("hasKey", "EXISTS"), Map.entry("expire", "EXPIRE"),
            Map.entry("getExpire", "TTL"), Map.entry("keys", "KEYS"), Map.entry("rename", "RENAME"));

    public InstrumentedStringRedisTemplate(RedisConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        super(connectionFactory);
        this.meterRegistry = meterRegistry;
        log.info("InstrumentedStringRedisTemplate initialized with metrics tracking");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValueOperations<String, String> opsForValue() {
        ValueOperations<String, String> ops = super.opsForValue();
        return (ValueOperations<String, String>) Proxy.newProxyInstance(ValueOperations.class.getClassLoader(),
                new Class<?>[] { ValueOperations.class }, new MetricsInvocationHandler(ops, "value"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListOperations<String, String> opsForList() {
        ListOperations<String, String> ops = super.opsForList();
        return (ListOperations<String, String>) Proxy.newProxyInstance(ListOperations.class.getClassLoader(),
                new Class<?>[] { ListOperations.class }, new MetricsInvocationHandler(ops, "list"));
    }

    /**
     * InvocationHandler that wraps each method call with timing and counting.
     */
    private class MetricsInvocationHandler implements InvocationHandler {

        private final Object target;

        private final String operationType;

        MetricsInvocationHandler(Object target, String operationType) {
            this.target = target;
            this.operationType = operationType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Skip non-operation methods
            if (methodName.equals("getOperations") || methodName.equals("toString") || methodName.equals("hashCode")
                    || methodName.equals("equals")) {
                return method.invoke(target, args);
            }

            String command = COMMAND_MAP.getOrDefault(methodName, methodName.toUpperCase());
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                Object result = method.invoke(target, args);
                recordSuccess(command, sample);
                return result;
            } catch (Throwable ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                recordError(command, sample, cause);
                throw cause;
            }
        }

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
        meterRegistry.counter("redis.command.errors", "command", command, "exception", ex.getClass().getSimpleName())
                .increment();
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

}
