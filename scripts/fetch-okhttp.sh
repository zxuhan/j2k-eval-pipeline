#!/usr/bin/env bash
# Fetches OkHttp 3.14.9 (target) and 4.12.0 (reference) into build/.
# Idempotent: re-running with existing clones skips the network round-trip.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/build"
mkdir -p "$BUILD"

fetch_tag() {
  local tag="$1" clone_dir="$2" src_rel="$3" dest="$4"
  if [[ -d "$clone_dir/.git" ]]; then
    echo "==> $clone_dir exists, skipping clone"
  else
    echo "==> cloning square/okhttp@$tag -> $clone_dir"
    git clone --depth 1 --branch "$tag" --single-branch https://github.com/square/okhttp.git "$clone_dir"
  fi
  if [[ ! -d "$clone_dir/$src_rel" ]]; then
    echo "ERROR: $clone_dir/$src_rel is missing" >&2
    exit 1
  fi
  rm -rf "$dest"
  mkdir -p "$(dirname "$dest")"
  cp -R "$clone_dir/$src_rel" "$dest"
  echo "    $(find "$dest" -type f | wc -l | tr -d ' ') files -> $dest"
}

fetch_tag parent-3.14.9 "$BUILD/okhttp-3.14.9" "okhttp/src/main/java"   "$BUILD/input"
fetch_tag parent-4.12.0 "$BUILD/okhttp-4.12.0" "okhttp/src/main/kotlin" "$BUILD/reference"

echo "done."
