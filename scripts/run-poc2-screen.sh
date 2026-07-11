#!/usr/bin/env bash
# POC 2 (compose-first) real-screen reproduction, wrapping the harness in compose-first/docker/.
# navigation-compose cannot render offscreen, so this builds and runs the JVM app on Linux arm64 under
# Xvfb (software GL) inside a JDK+gradle container and drives it with xdotool: home, increment
# (Count: 2 + Positive), detail (navigation), and persistence across a process restart.
# Usage: scripts/run-poc2-screen.sh
# Heavier than the other runners (the app is built inside the container; first run is slow).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/compose-first"
mkdir -p docker/out

echo "[poc2] building the real-screen harness image (JDK + gradle + Xvfb; first run is slow)..."
docker build --platform linux/arm64 -f docker/Dockerfile -t poc2-linux . >/dev/null

echo "[poc2] running compose-first on a virtual display (home, increment, detail, persistence)..."
docker run --rm --platform linux/arm64 -v "$PWD/docker/out:/out" poc2-linux

echo "[poc2] done. PNGs in $ROOT/compose-first/docker/out/"
ls -la docker/out/*.png 2>/dev/null || true
