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
 * Reactive test application.
 * <p>
 * Reactor IS on classpath. Uses reactive EventBus subscription.
 * <p>
 * Run with: {@code cd lettuce-test-reactive && mvn spring-boot:run}
 */
@SpringBootApplication
public class ReactiveTestApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReactiveTestApplication.class);

    private final ReactiveWorkloadRunner workloadRunner;

    private final ConfigurableApplicationContext context;

    public ReactiveTestApplication(ReactiveWorkloadRunner workloadRunner, ConfigurableApplicationContext context) {
        this.workloadRunner = workloadRunner;
        this.context = context;
    }

    public static void main(String[] args) {
        // Set system property for metrics detection (detectRunnerMode in MetricsReporter)
        System.setProperty("reactive", "true");
        SpringApplication.run(ReactiveTestApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running in REACTIVE mode (with Reactor) ===");
        log.info("Running REACTIVE workload (ReactiveStringRedisTemplate)");
        log.info("This exercises the reactor-optional path in SDR + Lettuce");

        workloadRunner.run();

        log.info("=== Workload complete ===");
        context.close();
    }

}

