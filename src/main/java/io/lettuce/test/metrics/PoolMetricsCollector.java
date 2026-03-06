package io.lettuce.test.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Collects connection pool metrics from the Lettuce connection pool. Registers gauges for active, idle, and waiting
 * connections.
 */
@Component
public class PoolMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(PoolMetricsCollector.class);

    private final LettuceConnectionFactory connectionFactory;

    private final MeterRegistry meterRegistry;

    public PoolMetricsCollector(LettuceConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        this.connectionFactory = connectionFactory;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerPoolMetrics() {
        // The pool is lazily initialized, so we need to access it after the factory is initialized
        // We'll use a supplier pattern to get the pool metrics

        GenericObjectPool<?> pool = extractPool();

        if (pool != null) {
            Gauge.builder("redis.pool.active", pool::getNumActive).description("Number of active connections in the pool")
                    .register(meterRegistry);

            Gauge.builder("redis.pool.idle", pool::getNumIdle).description("Number of idle connections in the pool")
                    .register(meterRegistry);

            Gauge.builder("redis.pool.waiting", pool::getNumWaiters).description("Number of threads waiting for a connection")
                    .register(meterRegistry);

            Gauge.builder("redis.pool.max", pool::getMaxTotal).description("Maximum number of connections in the pool")
                    .register(meterRegistry);

            Gauge.builder("redis.pool.min.idle", pool::getMinIdle).description("Minimum number of idle connections")
                    .register(meterRegistry);

            log.info("Pool metrics registered: active, idle, waiting, max, minIdle");
        } else {
            log.warn("Could not extract connection pool - pool metrics will not be available");
        }
    }

    /**
     * Extracts the underlying GenericObjectPool from the LettuceConnectionFactory. This uses reflection as there's no public
     * API to access the pool directly.
     */
    private GenericObjectPool<?> extractPool() {
        try {
            // First ensure the factory is initialized
            connectionFactory.afterPropertiesSet();

            // The pool is stored in a LettucePool which wraps GenericObjectPool
            // We need to access it through reflection
            Field poolField = LettuceConnectionFactory.class.getDeclaredField("pool");
            poolField.setAccessible(true);
            Object poolWrapper = poolField.get(connectionFactory);

            if (poolWrapper != null) {
                // The pool wrapper contains the actual GenericObjectPool
                Field internalPoolField = poolWrapper.getClass().getDeclaredField("internalPool");
                internalPoolField.setAccessible(true);
                Object internalPool = internalPoolField.get(poolWrapper);

                if (internalPool instanceof GenericObjectPool<?> genericPool) {
                    log.debug("Successfully extracted connection pool");
                    return genericPool;
                }
            }
        } catch (NoSuchFieldException e) {
            log.debug("Pool field not found - connection pooling may not be enabled: {}", e.getMessage());
        } catch (IllegalAccessException e) {
            log.warn("Could not access pool field: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error extracting connection pool: {}", e.getMessage());
        }

        return null;
    }

}
