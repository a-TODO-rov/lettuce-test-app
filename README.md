# Lettuce Test App

A benchmarking and testing application for comparing **reactor-optional** vs **standard** Lettuce Redis client configurations.

## Overview

This project validates the "reactor-optional" architecture using custom forks of Lettuce and Spring Data Redis:

- **[a-TODO-rov/lettuce](https://github.com/a-TODO-rov/lettuce/tree/reactor-optional)** - Lettuce fork with reactor-optional support
- **[a-TODO-rov/spring-data-redis](https://github.com/a-TODO-rov/spring-data-redis/tree/reactor-optional)** - Spring Data Redis fork with reactor-optional support

These forks allow applications to use Lettuce **without requiring Project Reactor on the classpath**.

## Architecture

This is a **multi-module Maven project** that physically isolates different dependency profiles:

```
lettuce-test-app/
├── lettuce-test-common/           # Shared code: workloads, config, metrics
├── lettuce-test-sync/             # reactor-optional fork, NO Reactor on classpath
├── lettuce-test-reactive/         # reactor-optional fork, WITH Reactor
├── lettuce-test-standard-sync/    # Official Lettuce, sync workload
└── lettuce-test-standard-reactive/ # Official Lettuce, reactive workload
```

The multi-module approach **physically enforces** classpath isolation, proving that:
- `lettuce-test-sync` runs **without** Reactor (validates reactor-optional works)
- `lettuce-test-standard-sync` **requires** Reactor (official Lettuce dependency)

## Test Scenarios

| # | Module | Command | Reactor | Description |
|---|--------|---------|---------|-------------|
| 1 | `lettuce-test-sync` | `cd lettuce-test-sync && mvn spring-boot:run` | ❌ No | Fork without Reactor |
| 2 | `lettuce-test-reactive` | `cd lettuce-test-reactive && mvn spring-boot:run` | ✅ Yes | Fork with Reactor |
| 3 | `lettuce-test-standard-sync` | `cd lettuce-test-standard-sync && mvn spring-boot:run` | ✅ Yes* | Official (sync workload) |
| 4 | `lettuce-test-standard-reactive` | `cd lettuce-test-standard-reactive && mvn spring-boot:run` | ✅ Yes | Official (reactive workload) |

\* *Standard Lettuce always requires Reactor, even for sync operations*

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Redis server running on `localhost:6379`

### Build All Modules

```bash
mvn clean install -DskipTests
```

### Run Individual Scenarios

```bash
# Scenario 1: reactor-optional + sync (NO Reactor on classpath!)
cd lettuce-test-sync && mvn spring-boot:run

# Scenario 2: reactor-optional + reactive
cd lettuce-test-reactive && mvn spring-boot:run

# Scenario 3: standard + sync (Reactor required by official Lettuce)
cd lettuce-test-standard-sync && mvn spring-boot:run

# Scenario 4: standard + reactive
cd lettuce-test-standard-reactive && mvn spring-boot:run
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
lettuce-test-app/
├── pom.xml                            # Parent POM (multi-module)
├── lettuce-test-common/               # Shared module
│   ├── pom.xml                        # JitPack forks (reactor-optional)
│   └── src/main/java/io/lettuce/test/
│       ├── config/                    # Redis & workload configuration
│       ├── metrics/                   # MetricsReporter, event listeners
│       ├── workloads/                 # Sync workload implementations
│       └── LettuceWorkloadRunner.java # Sync workload executor
├── lettuce-test-sync/                 # reactor-optional + sync
│   ├── pom.xml                        # NO Reactor dependency
│   └── src/.../SyncTestApplication.java
├── lettuce-test-reactive/             # reactor-optional + reactive
│   ├── pom.xml                        # Adds reactor-core dependency
│   └── src/.../reactive/              # ReactiveWorkloadRunner, workloads
├── lettuce-test-standard-sync/        # Official Lettuce + sync
│   ├── pom.xml                        # Uses spring-boot-starter-data-redis
│   └── src/.../StandardSyncTestApplication.java
├── lettuce-test-standard-reactive/    # Official Lettuce + reactive
│   ├── pom.xml                        # Uses spring-boot-starter-data-redis
│   └── src/.../StandardReactiveTestApplication.java
├── scripts/
│   ├── benchmark.sh                   # Run all scenarios
│   └── analyze-results.py             # Generate comparison report
└── logs/
    └── test-run-summary.json          # Latest run results
```

## Dependencies

### Modules Using Reactor-Optional Fork

`lettuce-test-common`, `lettuce-test-sync`, `lettuce-test-reactive` use JitPack forks:

| Dependency | Fork Repository | Maven Coordinate |
|------------|-----------------|------------------|
| Lettuce | [a-TODO-rov/lettuce](https://github.com/a-TODO-rov/lettuce/tree/reactor-optional) | `com.github.a-TODO-rov:lettuce` |
| Spring Data Redis | [a-TODO-rov/spring-data-redis](https://github.com/a-TODO-rov/spring-data-redis/tree/reactor-optional) | `com.github.a-TODO-rov:spring-data-redis` |

### Modules Using Official Dependencies

`lettuce-test-standard-sync`, `lettuce-test-standard-reactive` use official releases:

| Dependency | Official Repository | Maven Coordinate |
|------------|---------------------|------------------|
| Lettuce | [lettuce-io/lettuce-core](https://github.com/lettuce-io/lettuce-core) | `io.lettuce:lettuce-core` |
| Spring Data Redis | [spring-projects/spring-data-redis](https://github.com/spring-projects/spring-data-redis) | `org.springframework.data:spring-data-redis` |

## Build

```bash
# Build all modules
mvn clean install -DskipTests

# Build specific module
mvn clean install -DskipTests -pl lettuce-test-sync -am

# Format code
mvn formatter:format
```

## Verify Classpath Isolation

Check that Reactor is NOT on the classpath for the sync module:

```bash
# Should show Reactor
cd lettuce-test-reactive && mvn dependency:tree | grep -i reactor
# Output: io.projectreactor:reactor-core:jar:3.x.x

# Should show NO Reactor
cd lettuce-test-sync && mvn dependency:tree | grep -i reactor
# Output: (empty - no matches!)
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
