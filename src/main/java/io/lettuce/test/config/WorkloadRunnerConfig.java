package io.lettuce.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "runner")
@PropertySource(value = "classpath:runner-config-defaults.yaml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "file:${runner.config:runner-config.yaml}", factory = YamlPropertySourceFactory.class, ignoreResourceNotFound = true)
public class WorkloadRunnerConfig {

    private RedisConfig redis;

    private PoolConfig pool;

    private TestConfig test;

    WorkloadRunnerConfig() {

    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public PoolConfig getPool() {
        return pool;
    }

    public void setPool(PoolConfig pool) {
        this.pool = pool;
    }

    public TestConfig getTest() {
        return test;
    }

    public void setTest(TestConfig test) {
        this.test = test;
    }

    @Override
    public String toString() {
        return "WorkloadRunnerConfig{" + "redis=" + redis + ", pool=" + pool + ", test=" + test + '}';
    }

    public static class RedisConfig {

        private String host = "localhost";

        private int port = 6379;

        private int database = 0;

        private String username;

        private String password;

        private boolean useTls = false;

        private String clientName = "lettuce-test-app";

        private Duration timeout = Duration.ofMillis(200);

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isUseTls() {
            return useTls;
        }

        public void setUseTls(boolean useTls) {
            this.useTls = useTls;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        @Override
        public String toString() {
            return "RedisConfig{" + "host='" + host + '\'' + ", port=" + port + ", database=" + database + ", username='"
                    + username + '\'' + ", password='"
                    + Optional.ofNullable(password).map(p -> "*".repeat(p.length())).orElse("") + '\'' + ", useTls=" + useTls
                    + ", clientName='" + clientName + '\'' + ", timeout=" + timeout + '}';
        }

    }

    public static class PoolConfig {

        private int maxActive = 8;

        private int maxIdle = 8;

        private int minIdle = 0;

        private Duration maxWait = Duration.ofMillis(-1);

        public int getMaxActive() {
            return maxActive;
        }

        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public Duration getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }

        @Override
        public String toString() {
            return "PoolConfig{" + "maxActive=" + maxActive + ", maxIdle=" + maxIdle + ", minIdle=" + minIdle + ", maxWait="
                    + maxWait + '}';
        }

    }

    public static class TestConfig {

        private String mode = "standalone";

        private int concurrentWorkers = 1;

        private WorkloadConfig workload;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getConcurrentWorkers() {
            return concurrentWorkers;
        }

        public void setConcurrentWorkers(int concurrentWorkers) {
            this.concurrentWorkers = concurrentWorkers;
        }

        public WorkloadConfig getWorkload() {
            return workload;
        }

        public void setWorkload(WorkloadConfig workload) {
            this.workload = workload;
        }

        @Override
        public String toString() {
            return "TestConfig{" + "mode='" + mode + '\'' + ", concurrentWorkers=" + concurrentWorkers + ", workload="
                    + workload + '}';
        }

    }

    public static class WorkloadConfig {

        private String type = "get_set";

        private Duration maxDuration = Duration.ofSeconds(60);

        private Map<String, String> options;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Duration getMaxDuration() {
            return maxDuration;
        }

        public void setMaxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

        @Override
        public String toString() {
            return "WorkloadConfig{" + "type='" + type + '\'' + ", maxDuration=" + maxDuration + ", options=" + options + '}';
        }

    }

}
