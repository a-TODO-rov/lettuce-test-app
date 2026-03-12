package io.lettuce.test.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Micrometer metrics.
 */
@Configuration
public class MetricsConfig {

    /**
     * Creates a simple MeterRegistry for metrics collection. This is a basic in-memory registry suitable for testing and
     * logging metrics.
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

}
