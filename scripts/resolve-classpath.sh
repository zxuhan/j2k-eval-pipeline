#!/usr/bin/env bash
# Resolves OkHttp 3.14.9's runtime classpath via the helper Gradle project in
# scripts/classpath-resolver/ and writes it to build/classpath.txt (one jar per line).
# Consumed by evaluator's CompileAnalyzer.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build"
RESOLVER="$ROOT/scripts/classpath-resolver"

mkdir -p "$BUILD"

# Use the parent wrapper with -p so we don't need a second gradlew.
# The resolver's settings.gradle.kts in $RESOLVER makes it a standalone build root.
"$ROOT/gradlew" -p "$RESOLVER" dumpClasspath -q --console=plain

cp "$RESOLVER/classpath.txt" "$BUILD/classpath.txt"
echo "wrote $(wc -l < "$BUILD/classpath.txt" | tr -d ' ') entries to $BUILD/classpath.txt"
