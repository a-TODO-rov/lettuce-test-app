#!/usr/bin/env python3
"""
Analyze benchmark results and generate comparison report.

Usage: python3 scripts/analyze-results.py <results_directory>
"""

import json
import sys
import statistics
from pathlib import Path
from collections import defaultdict


def load_results(results_dir: Path) -> dict:
    """Load all JSON result files grouped by scenario."""
    scenarios = defaultdict(list)
    
    for file in sorted(results_dir.glob("*-run*.json")):
        # Extract scenario name (e.g., "reactor-optional-sync" from "reactor-optional-sync-run1.json")
        scenario = file.stem.rsplit("-run", 1)[0]
        try:
            with open(file) as f:
                data = json.load(f)
                data["_file"] = file.name
                scenarios[scenario].append(data)
        except json.JSONDecodeError as e:
            print(f"WARNING: Failed to parse {file}: {e}")
    
    return dict(scenarios)


def calc_stats(values: list) -> dict:
    """Calculate mean and stdev for a list of values."""
    if len(values) == 0:
        return {"mean": 0, "stdev": 0, "min": 0, "max": 0}
    if len(values) == 1:
        return {"mean": values[0], "stdev": 0, "min": values[0], "max": values[0]}
    return {
        "mean": statistics.mean(values),
        "stdev": statistics.stdev(values),
        "min": min(values),
        "max": max(values)
    }


def analyze_scenario(runs: list) -> dict:
    """Analyze runs for a single scenario."""
    throughputs = []
    latencies_median = []
    latencies_p99 = []
    heap_used = []

    for run in runs:
        duration = run.get("duration_seconds", 10)
        commands = run.get("total_commands_count", 0)
        throughputs.append(commands / duration)
        latencies_median.append(run.get("median_latency_ms", 0))
        latencies_p99.append(run.get("p99_latency_ms", 0))
        if "jvm_heap_used_mb" in run:
            heap_used.append(run.get("jvm_heap_used_mb", 0))

    return {
        "runs": len(runs),
        "dependency_profile": runs[0].get("dependency_profile", "unknown"),
        "runner_mode": runs[0].get("runner_mode", "unknown"),
        "reactor_available": runs[0].get("reactor_available", False),
        "throughput": calc_stats(throughputs),
        "latency_median": calc_stats(latencies_median),
        "latency_p99": calc_stats(latencies_p99),
        "heap_used_mb": calc_stats(heap_used) if heap_used else None,
        "success_rate": runs[0].get("success_rate", "N/A")
    }


def print_report(scenarios: dict, results_dir: Path):
    """Print formatted benchmark report."""
    # Load environment info if available
    env_file = results_dir / "environment.json"
    if env_file.exists():
        try:
            with open(env_file) as f:
                env = json.load(f)
            print(f"\nEnvironment: {env.get('os', 'N/A')}")
            print(f"Java: {env.get('java_version', 'N/A')}")
            print(f"Runs per scenario: {env.get('runs_per_scenario', 'N/A')}")
        except json.JSONDecodeError:
            print("\nEnvironment: (could not parse environment.json)")
    
    print("\n" + "=" * 80)
    print("BENCHMARK RESULTS")
    print("=" * 80)
    
    analyses = {}
    for scenario, runs in sorted(scenarios.items()):
        analysis = analyze_scenario(runs)
        analyses[scenario] = analysis
        
        tp = analysis["throughput"]
        lat_med = analysis["latency_median"]
        lat_p99 = analysis["latency_p99"]
        
        print(f"\n{scenario}:")
        print(f"  Profile: {analysis['dependency_profile']}, Mode: {analysis['runner_mode']}, Reactor: {analysis['reactor_available']}")
        print(f"  Throughput:     {tp['mean']:>10,.0f} ± {tp['stdev']:>6,.0f} cmd/s")
        print(f"  Latency (p50):  {lat_med['mean']:>10.3f} ± {lat_med['stdev']:>6.3f} ms")
        print(f"  Latency (p99):  {lat_p99['mean']:>10.3f} ± {lat_p99['stdev']:>6.3f} ms")
        if analysis.get("heap_used_mb"):
            heap = analysis["heap_used_mb"]
            print(f"  Heap Used:      {heap['mean']:>10.0f} ± {heap['stdev']:>6.0f} MB")
        print(f"  Success Rate:   {analysis['success_rate']}")
    
    # Comparisons
    print("\n" + "=" * 80)
    print("COMPARISONS")
    print("=" * 80)
    
    if "reactor-optional-sync" in analyses and "standard-sync" in analyses:
        ro_sync = analyses["reactor-optional-sync"]["throughput"]["mean"]
        std_sync = analyses["standard-sync"]["throughput"]["mean"]
        diff = ((ro_sync - std_sync) / std_sync) * 100
        print(f"\nreactor-optional sync vs standard sync:")
        print(f"  Throughput: {ro_sync:,.0f} vs {std_sync:,.0f} cmd/s ({diff:+.1f}%)")
    
    if "reactor-optional-reactive" in analyses and "standard-reactive" in analyses:
        ro_react = analyses["reactor-optional-reactive"]["throughput"]["mean"]
        std_react = analyses["standard-reactive"]["throughput"]["mean"]
        diff = ((ro_react - std_react) / std_react) * 100
        print(f"\nreactor-optional reactive vs standard reactive:")
        print(f"  Throughput: {ro_react:,.0f} vs {std_react:,.0f} cmd/s ({diff:+.1f}%)")
    
    if "reactor-optional-sync" in analyses and "reactor-optional-reactive" in analyses:
        sync = analyses["reactor-optional-sync"]["throughput"]["mean"]
        react = analyses["reactor-optional-reactive"]["throughput"]["mean"]
        diff = ((react - sync) / sync) * 100
        print(f"\nreactor-optional: reactive vs sync:")
        print(f"  Throughput: {react:,.0f} vs {sync:,.0f} cmd/s ({diff:+.1f}%)")

    # Memory comparison
    print("\n" + "-" * 40)
    print("MEMORY USAGE")
    print("-" * 40)

    if "reactor-optional-sync" in analyses and "standard-sync" in analyses:
        ro_heap = analyses["reactor-optional-sync"].get("heap_used_mb")
        std_heap = analyses["standard-sync"].get("heap_used_mb")
        if ro_heap and std_heap:
            diff = ((ro_heap["mean"] - std_heap["mean"]) / std_heap["mean"]) * 100
            print(f"\nreactor-optional sync vs standard sync:")
            print(f"  Heap: {ro_heap['mean']:.0f} vs {std_heap['mean']:.0f} MB ({diff:+.1f}%)")
            print(f"  (reactor-optional runs WITHOUT Reactor on classpath)")

    # Save analysis to JSON
    output_file = results_dir / "analysis.json"
    with open(output_file, "w") as f:
        json.dump(analyses, f, indent=2)
    print(f"\n\nAnalysis saved to: {output_file}")


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <results_directory>")
        sys.exit(1)
    
    results_dir = Path(sys.argv[1])
    if not results_dir.exists():
        print(f"ERROR: Directory not found: {results_dir}")
        sys.exit(1)
    
    scenarios = load_results(results_dir)
    if not scenarios:
        print(f"ERROR: No result files found in {results_dir}")
        sys.exit(1)
    
    print_report(scenarios, results_dir)


if __name__ == "__main__":
    main()

