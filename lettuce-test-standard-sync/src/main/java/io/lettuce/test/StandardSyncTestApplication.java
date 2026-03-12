package io.lettuce.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Standard sync test application.
 * <p>
 * Uses OFFICIAL Lettuce and Spring Data Redis (not the reactor-optional forks).
 * Reactor IS on the classpath (required by official Lettuce).
 * <p>
 * Run with: {@code cd lettuce-test-standard-sync && mvn spring-boot:run}
 */
@SpringBootApplication
public class StandardSyncTestApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StandardSyncTestApplication.class);

    private final LettuceWorkloadRunner workloadRunner;

    private final ConfigurableApplicationContext context;

    public StandardSyncTestApplication(LettuceWorkloadRunner workloadRunner, ConfigurableApplicationContext context) {
        this.workloadRunner = workloadRunner;
        this.context = context;
    }

    public static void main(String[] args) {
        // Set system property for metrics detection
        System.setProperty("standard", "true");
        SpringApplication.run(StandardSyncTestApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running in STANDARD SYNC mode (official Lettuce/SDR) ===");
        log.info("Running SYNC workload (StringRedisTemplate) with official dependencies");
        log.info("Note: Reactor IS on classpath (required by official Lettuce)");

        workloadRunner.run();

        log.info("=== Workload complete ===");
        context.close();
    }

}

