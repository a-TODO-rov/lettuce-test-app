package io.lettuce.test;

import io.lettuce.test.reactive.ReactiveWorkloadRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Standard reactive test application.
 * <p>
 * Uses OFFICIAL Lettuce and Spring Data Redis (not the reactor-optional forks).
 * Uses ReactiveStringRedisTemplate for reactive operations.
 * <p>
 * Run with: {@code cd lettuce-test-standard-reactive && mvn spring-boot:run}
 */
@SpringBootApplication
public class StandardReactiveTestApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StandardReactiveTestApplication.class);

    private final ReactiveWorkloadRunner workloadRunner;

    private final ConfigurableApplicationContext context;

    public StandardReactiveTestApplication(ReactiveWorkloadRunner workloadRunner, ConfigurableApplicationContext context) {
        this.workloadRunner = workloadRunner;
        this.context = context;
    }

    public static void main(String[] args) {
        // Set system properties for metrics detection
        System.setProperty("standard", "true");
        System.setProperty("reactive", "true");
        SpringApplication.run(StandardReactiveTestApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running in STANDARD REACTIVE mode (official Lettuce/SDR) ===");
        log.info("Running REACTIVE workload (ReactiveStringRedisTemplate) with official dependencies");

        workloadRunner.run();

        log.info("=== Workload complete ===");
        context.close();
    }

}

