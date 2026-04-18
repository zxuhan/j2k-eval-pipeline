#!/usr/bin/env bash
# Thin orchestrator around :j2k-runner:runIde. Defaults wire through the paths
# scripts/fetch-okhttp.sh sets up. Override positional args to convert a different
# directory (useful for edge-cases).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build"

INPUT="${1:-$BUILD/input}"
OUTPUT="${2:-$BUILD/converted}"
DIAGNOSTICS="${3:-$BUILD/diagnostics.json}"

# Resolve to absolute paths — the forked IDEA JVM runs with a different cwd.
[[ "$INPUT" = /* ]] || INPUT="$ROOT/$INPUT"
[[ "$OUTPUT" = /* ]] || OUTPUT="$ROOT/$OUTPUT"
[[ "$DIAGNOSTICS" = /* ]] || DIAGNOSTICS="$ROOT/$DIAGNOSTICS"

mkdir -p "$OUTPUT" "$(dirname "$DIAGNOSTICS")"

echo "==> j2k: $INPUT -> $OUTPUT (diag: $DIAGNOSTICS)"

"$ROOT/gradlew" :j2k-runner:runIde \
    --args "j2k-batch --input $INPUT --output $OUTPUT --diagnostics $DIAGNOSTICS" \
    --console=plain

echo "==> converted: $(find "$OUTPUT" -name '*.kt' -type f 2>/dev/null | wc -l | tr -d ' ') .kt files"
