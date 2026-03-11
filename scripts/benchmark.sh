#!/bin/bash
#
# Benchmark script for comparing Lettuce test scenarios
#
# Usage: ./scripts/benchmark.sh [RUNS]
#   RUNS: Number of runs per scenario (default: 3)
#
# Scenarios:
#   1. reactor-optional + sync
#   2. reactor-optional + reactive
#   3. standard + sync
#   4. standard + reactive
#

set -e

RUNS=${1:-3}
COOLDOWN=10
RESULTS_DIR="benchmark-results/$(date +%Y%m%d-%H%M%S)"
JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")}

# JVM options for memory tracking
export MAVEN_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Scenarios: name:maven-flags
declare -a SCENARIOS=(
  "reactor-optional-sync:"
  "reactor-optional-reactive:-Dreactive -Dinclude-reactor-dep"
  "standard-sync:-Dstandard"
  "standard-reactive:-Dstandard -Dreactive"
)

echo "========================================"
echo "Lettuce Benchmark Suite"
echo "========================================"
echo "Runs per scenario: $RUNS"
echo "Results directory: $RESULTS_DIR"
echo "JAVA_HOME: $JAVA_HOME"
echo "MAVEN_OPTS: $MAVEN_OPTS"
echo "========================================"
echo ""

# Check Java version
if [ -n "$JAVA_HOME" ]; then
  export JAVA_HOME
  JAVA_VERSION=$($JAVA_HOME/bin/java -version 2>&1 | head -1)
  echo "Java: $JAVA_VERSION"
else
  JAVA_VERSION=$(java -version 2>&1 | head -1)
  echo "Java: $JAVA_VERSION"
fi
echo ""

# Create results directory
mkdir -p "$RESULTS_DIR"

# Save environment info using Python for proper JSON escaping
python3 -c "
import json
env = {
    'timestamp': '$(date -Iseconds)',
    'java_version': '''$JAVA_VERSION''',
    'maven_opts': '$MAVEN_OPTS',
    'runs_per_scenario': $RUNS,
    'cooldown_seconds': $COOLDOWN,
    'hostname': '$(hostname)',
    'os': '$(uname -s) $(uname -r)'
}
with open('$RESULTS_DIR/environment.json', 'w') as f:
    json.dump(env, f, indent=2)
"

# Run benchmarks
total_scenarios=${#SCENARIOS[@]}
current=0

for scenario_def in "${SCENARIOS[@]}"; do
  name="${scenario_def%%:*}"
  flags="${scenario_def##*:}"
  ((current++)) || true
  
  echo ""
  echo "========================================"
  echo "Scenario $current/$total_scenarios: $name"
  echo "Maven flags: ${flags:-<none>}"
  echo "========================================"
  
  for run in $(seq 1 $RUNS); do
    echo ""
    echo "--- Run $run/$RUNS ---"
    
    # Clean compile for first run of each scenario
    if [ "$run" -eq 1 ]; then
      echo "Cleaning and compiling..."
      mvn clean compile $flags -q 2>/dev/null || mvn clean compile $flags
    fi
    
    # Run the test
    echo "Running test..."
    start_time=$(date +%s)
    
    if mvn spring-boot:run $flags -q 2>&1 | tee "$RESULTS_DIR/${name}-run${run}.log" | grep -E "FINAL TEST RESULTS|total_commands|median_latency|success_rate"; then
      end_time=$(date +%s)
      duration=$((end_time - start_time))
      echo "Completed in ${duration}s"
      
      # Copy results
      if [ -f "logs/test-run-summary.json" ]; then
        cp "logs/test-run-summary.json" "$RESULTS_DIR/${name}-run${run}.json"
        echo "Results saved to $RESULTS_DIR/${name}-run${run}.json"
      else
        echo "WARNING: No test-run-summary.json found!"
      fi
    else
      echo "ERROR: Test failed!"
      exit 1
    fi
    
    # Cooldown between runs (except after last run of last scenario)
    if [ "$run" -lt "$RUNS" ] || [ "$current" -lt "$total_scenarios" ]; then
      echo "Cooling down for ${COOLDOWN}s..."
      sleep $COOLDOWN
    fi
  done
done

echo ""
echo "========================================"
echo "Benchmark Complete!"
echo "========================================"
echo "Results saved to: $RESULTS_DIR"
echo ""

# Run analysis
if [ -f "scripts/analyze-results.py" ]; then
  echo "Running analysis..."
  python3 scripts/analyze-results.py "$RESULTS_DIR"
else
  echo "Run analysis with: python3 scripts/analyze-results.py $RESULTS_DIR"
fi

