#!/bin/bash

# Script to build lettuce-test-app multi-module project
# Usage: ./scripts/build.sh
#
# Modules:
#   - lettuce-test-common: Shared code (no Reactor)
#   - lettuce-test-sync: Sync module (no Reactor)
#   - lettuce-test-reactive: Reactive module (with Reactor)

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Get lettuce version from pom.xml
LETTUCE_VERSION=$(mvn -f "$PROJECT_ROOT/pom.xml" help:evaluate -Dexpression=lettuce.version -q -DforceStdout 2>/dev/null)

echo "=========================================="
echo "Building lettuce-test-app (multi-module)"
echo "=========================================="
echo "Lettuce version: $LETTUCE_VERSION (from pom.xml)"
echo "Project root: $PROJECT_ROOT"
echo "=========================================="

# Step 1: Check code formatting
echo "Step 1: Checking code formatting..."
cd "$PROJECT_ROOT"
mvn formatter:validate

# Step 2: Build all modules
echo "Step 2: Building all modules..."
mvn clean install -B

# Step 3: Run tests
echo "Step 3: Running tests..."
mvn test

echo "=========================================="
echo "✅ Successfully built lettuce-test-app"
echo "=========================================="
echo ""
echo "To run the applications:"
echo "  Sync (no Reactor):     cd lettuce-test-sync && mvn spring-boot:run"
echo "  Reactive (w/ Reactor): cd lettuce-test-reactive && mvn spring-boot:run"
echo "=========================================="
