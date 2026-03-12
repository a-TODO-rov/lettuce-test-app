package io.lettuce.test.reactive;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * Reactive Redis configuration - only loaded when Reactor is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "reactor.core.publisher.Flux")
public class ReactiveRedisConfig {

    private static final Logger log = LoggerFactory.getLogger(ReactiveRedisConfig.class);

    /**
     * Instrumented reactive template - exercises the reactor-optional path in SDR + Lettuce.
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(LettuceConnectionFactory connectionFactory,
            MeterRegistry meterRegistry) {
        log.info("Creating InstrumentedReactiveStringRedisTemplate to test reactive path");
        return new InstrumentedReactiveStringRedisTemplate(connectionFactory, meterRegistry);
    }

}
