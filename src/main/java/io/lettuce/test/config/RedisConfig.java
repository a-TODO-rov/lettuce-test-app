package io.lettuce.test.config;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.test.metrics.InstrumentedStringRedisTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Creates a shared ClientResources bean that can be used for event listening. This bean is injected into
     * LettuceEventListener for connection event monitoring.
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        log.info("Creating shared ClientResources for event listening");
        return DefaultClientResources.create();
    }

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(WorkloadRunnerConfig config, ClientResources clientResources) {
        WorkloadRunnerConfig.RedisConfig redis = config.getRedis();
        WorkloadRunnerConfig.PoolConfig pool = config.getPool();

        log.info("Configuring Redis connection to {}:{}", redis.getHost(), redis.getPort());

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redis.getHost());
        redisConfig.setPort(redis.getPort());
        redisConfig.setDatabase(redis.getDatabase());

        if (redis.getUsername() != null && !redis.getUsername().isEmpty()) {
            redisConfig.setUsername(redis.getUsername());
        }
        if (redis.getPassword() != null && !redis.getPassword().isEmpty()) {
            redisConfig.setPassword(redis.getPassword());
        }

        // Configure connection pooling
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        if (pool != null) {
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            if (pool.getMaxWait() != null) {
                poolConfig.setMaxWait(pool.getMaxWait());
            }
            log.info("Connection pool configured: maxActive={}, maxIdle={}, minIdle={}", pool.getMaxActive(), pool.getMaxIdle(),
                    pool.getMinIdle());
        }

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientConfig = LettucePoolingClientConfiguration
                .builder().poolConfig(poolConfig).clientResources(clientResources); // Share ClientResources for event listening

        if (redis.getTimeout() != null) {
            clientConfig.commandTimeout(redis.getTimeout());
        }

        // Configure TLS if enabled
        if (redis.isUseTls()) {
            clientConfig.useSsl();
            log.info("TLS enabled for Redis connection");
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig.build());
        factory.setValidateConnection(true);

        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        return new InstrumentedStringRedisTemplate(connectionFactory, meterRegistry);
    }

}
