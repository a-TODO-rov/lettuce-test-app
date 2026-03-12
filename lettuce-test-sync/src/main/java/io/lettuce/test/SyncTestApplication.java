package io.lettuce.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Sync/callback-based test application.
 * <p>
 * NO Reactor on classpath. Uses callback-based EventBus subscription.
 * <p>
 * Run with: {@code cd lettuce-test-sync && mvn spring-boot:run}
 */
@SpringBootApplication
public class SyncTestApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SyncTestApplication.class);

    private final LettuceWorkloadRunner workloadRunner;

    private final ConfigurableApplicationContext context;

    public SyncTestApplication(LettuceWorkloadRunner workloadRunner, ConfigurableApplicationContext context) {
        this.workloadRunner = workloadRunner;
        this.context = context;
    }

    public static void main(String[] args) {
        SpringApplication.run(SyncTestApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running in SYNC mode (no Reactor) ===");
        log.info("Running SYNC workload (StringRedisTemplate)");

        workloadRunner.run();

        log.info("=== Workload complete ===");
        context.close();
    }

}

