package io.lettuce.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Lettuce Test Application - validates Lettuce reactor-optional changes with Spring Data Redis.
 * <p>
 * Run modes:
 * <ul>
 * <li>--mode=sync (default) - Uses synchronous StringRedisTemplate</li>
 * <li>--mode=reactive - Uses ReactiveStringRedisTemplate (requires Reactor on classpath)</li>
 * </ul>
 */
@SpringBootApplication
public class LettuceTestApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LettuceTestApplication.class);

    private final LettuceWorkloadRunner syncRunner;

    private final ObjectProvider<Runnable> reactiveRunnerProvider;

    private final ConfigurableApplicationContext context;

    @Value("${runner.mode:sync}")
    private String runMode;

    public LettuceTestApplication(LettuceWorkloadRunner syncRunner, ObjectProvider<Runnable> reactiveRunnerProvider,
            ConfigurableApplicationContext context) {
        this.syncRunner = syncRunner;
        this.reactiveRunnerProvider = reactiveRunnerProvider;
        this.context = context;
    }

    public static void main(String[] args) {
        SpringApplication.run(LettuceTestApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        // Check for --mode argument
        String mode = runMode;
        if (args.containsOption("mode")) {
            mode = args.getOptionValues("mode").get(0);
        }

        log.info("=== Running in {} mode ===", mode.toUpperCase());

        switch (mode.toLowerCase()) {
            case "sync" -> {
                log.info("Running SYNC workload (StringRedisTemplate)");
                syncRunner.run();
            }
            case "reactive" -> {
                Runnable reactiveRunner = reactiveRunnerProvider.getIfAvailable();
                if (reactiveRunner == null) {
                    log.error("Reactive mode requested but Reactor is not on the classpath!");
                    log.error("Add io.projectreactor:reactor-core dependency to use reactive mode.");
                    throw new IllegalStateException("Reactor not available - cannot run reactive mode");
                }
                log.info("Running REACTIVE workload (ReactiveStringRedisTemplate)");
                log.info("This exercises the reactor-optional path in SDR + Lettuce");
                reactiveRunner.run();
            }
            default -> {
                log.error("Unknown mode: {}. Use: sync or reactive", mode);
                throw new IllegalArgumentException("Unknown mode: " + mode);
            }
        }

        log.info("=== Workload complete ===");
        // Exit cleanly after workload completes
        context.close();
    }

}
