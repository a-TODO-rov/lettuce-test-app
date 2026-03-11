# Lettuce Test App

A benchmarking and testing application for comparing **reactor-optional** vs **standard** Lettuce Redis client configurations.

## Overview

This project validates the "reactor-optional" architecture using custom forks of Lettuce and Spring Data Redis:

- **[a-TODO-rov/lettuce](https://github.com/a-TODO-rov/lettuce)** - Lettuce fork with reactor-optional support
- **[a-TODO-rov/spring-data-redis](https://github.com/a-TODO-rov/spring-data-redis)** - Spring Data Redis fork with reactor-optional support

These forks allow applications to use Lettuce **without requiring Project Reactor on the classpath**.

## Test Scenarios

The application supports 4 test scenarios:

| # | Scenario | Command | Description |
|---|----------|---------|-------------|
| 1 | reactor-optional + sync | `mvn spring-boot:run` | Fork without Reactor on classpath |
| 2 | reactor-optional + reactive | `mvn spring-boot:run -Dreactive -Dinclude-reactor-dep` | Fork with Reactor enabled |
| 3 | standard + sync | `mvn spring-boot:run -Dstandard` | Official Lettuce (sync workload) |
| 4 | standard + reactive | `mvn spring-boot:run -Dstandard -Dreactive` | Official Lettuce (reactive workload) |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Redis server running on `localhost:6379`

### Run a Single Scenario

```bash
# Scenario 1: reactor-optional + sync (default)
mvn spring-boot:run

# Scenario 2: reactor-optional + reactive
mvn spring-boot:run -Dreactive -Dinclude-reactor-dep

# Scenario 3: standard + sync
mvn spring-boot:run -Dstandard

# Scenario 4: standard + reactive
mvn spring-boot:run -Dstandard -Dreactive
```

### Run Full Benchmark

```bash
# Run all 4 scenarios with 3 runs each
./scripts/benchmark.sh 3

# Analyze results
python3 scripts/analyze-results.py benchmark-results/<timestamp>/
```

### Sample Benchmark Output

```
================================================================================
BENCHMARK RESULTS
================================================================================

reactor-optional-sync:
  Throughput:         12,693 ±    766 cmd/s
  Latency (p50):       0.070 ±  0.002 ms
  Heap Used:              54 MB
  Reactor: ❌

reactor-optional-reactive:
  Throughput:         18,823 ±     40 cmd/s
  Latency (p50):       0.084 ms
  Heap Used:              85 MB
  Reactor: ✅

standard-sync:
  Throughput:         12,807 ±     25 cmd/s
  Latency (p50):       0.072 ms
  Heap Used:              89 MB
  Reactor: ✅

standard-reactive:
  Throughput:         18,767 ±    175 cmd/s
  Latency (p50):       0.084 ms
  Heap Used:             104 MB
  Reactor: ✅

================================================================================
COMPARISONS
================================================================================
reactor-optional sync vs standard sync: -0.9% throughput
reactor-optional reactive vs standard reactive: +0.3% throughput
Reactive vs Sync: +48% throughput (better pipelining)
Memory: reactor-optional sync uses ~40% less heap (no Reactor loaded)
```

## Project Structure

```
├── src/main/java/io/lettuce/test/
│   ├── LettuceTestApplication.java    # Main application
│   ├── metrics/
│   │   ├── eventbus/                  # EventBus subscriber factory
│   │   ├── MetricsReporter.java       # Generates test-run-summary.json
│   │   └── LettuceEventListener.java  # Connection event tracking
│   ├── reactive/                      # Reactive workload (excluded in sync mode)
│   └── workloads/                     # Test workload implementations
├── scripts/
│   ├── benchmark.sh                   # Run all scenarios
│   └── analyze-results.py             # Generate comparison report
├── logs/
│   └── test-run-summary.json          # Latest run results
└── benchmark-results/                 # Historical benchmark data
```

## Dependencies

### Reactor-Optional Profile (default)

Uses JitPack to pull the reactor-optional forks:

| Dependency | Fork Repository | Maven Coordinate |
|------------|-----------------|------------------|
| Lettuce | [a-TODO-rov/lettuce](https://github.com/a-TODO-rov/lettuce) | `com.github.a-TODO-rov:lettuce` |
| Spring Data Redis | [a-TODO-rov/spring-data-redis](https://github.com/a-TODO-rov/spring-data-redis) | `com.github.a-TODO-rov:spring-data-redis` |

### Standard Profile (`-Dstandard`)

Uses official releases from Maven Central for comparison:

| Dependency | Official Repository | Maven Coordinate |
|------------|---------------------|------------------|
| Lettuce | [lettuce-io/lettuce-core](https://github.com/lettuce-io/lettuce-core) | `io.lettuce:lettuce-core` |
| Spring Data Redis | [spring-projects/spring-data-redis](https://github.com/spring-projects/spring-data-redis) | `org.springframework.data:spring-data-redis` |

## Build

```bash
# Build with default profile (reactor-optional)
mvn clean compile

# Build with standard profile
mvn clean compile -Dstandard

# Format code
mvn formatter:format
```

## Configuration

All settings are in `runner-config.yaml`:

```yaml
runner:
  redis:
    host: localhost
    port: 6379
    timeout: PT0.200S        # Connection timeout

  test:
    workload:
      type: get_set          # Workload type (see below)
      maxDuration: PT10S     # Max test duration
      options:
        getSetRatio: 0.5     # 0.5 = 50% GET, 50% SET
        valueSize: 100       # Payload size in bytes
        iterationCount: 100  # Operations per run
```

Override any setting via command line: `--runner.test.workload.type=redis_commands`

> **Note:** Runner mode (sync/reactive) is controlled via Maven profiles, not config file. Use `-Dreactive` for reactive mode.

## Workloads (this branch)

This branch supports the following workloads for reactor-optional validation:

| Mode | Workload Type | Description |
|------|---------------|-------------|
| **Sync** | `get_set` | Mix of GET/SET operations (default) |
| **Sync** | `redis_commands` | GET, SET, DEL, INCR, LPUSH, LRANGE |
| **Reactive** | `reactive_get_set` | Same as `get_set` but using `ReactiveStringRedisTemplate` |

**Key options:**
- `getSetRatio` - Ratio of SET operations (0.5 = 50% SET, 50% GET)
- `valueSize` - Payload size in bytes
- `iterationCount` - Number of operations per test run

## Metrics

Metrics are collected using Micrometer and logged to files. A JSON summary is generated at the end of each run.

### Output Files

| File | Description |
|------|-------------|
| `logs/test-run-summary.json` | Final JSON summary with throughput, latency percentiles, success rate |
| `logs/lettuce-test-app.log` | Periodic metrics snapshots during the run |

### Collected Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `redis.operation.duration` | Timer | Command latency (p50, p95, p99) |
| `redis.operations.count` | Counter | Commands by status (success/error) |
| `redis.reconnect.attempts` | Counter | Reconnection attempts |
| `redis.reconnect.failures` | Counter | Failed reconnections |
| `redis.connection.events` | Counter | Connection lifecycle events |
| `redis.pool.*` | Gauge | Connection pool stats (active, idle, waiting) |

### Configuration

Metrics dump rate in `application.properties`:
```properties
metrics.dump.rate=PT5S  # Dump every 5 seconds
```
