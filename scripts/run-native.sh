#!/usr/bin/env bash
# Build, link and render a Kotlin/Native Linux POC (no JVM) in a Linux arm64 container under Xvfb.
# Usage: scripts/run-native.sh <poc3-native|poc4-native|poc5-native|poc6-native> [debug|release]
# Prereqs: scripts/fetch-deps.sh (compose checkout + native libs); POC 6 also needs ./export (skip export).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
POC="${1:?usage: run-native.sh <poc*-native> [debug|release]}"
MODE="${2:-release}"
CMC="${CMC_ROOT:-$ROOT/.cmc}"
[ -d "$ROOT/$POC" ] || { echo "unknown POC: $POC"; exit 1; }

# Gradle task name: linkReleaseExecutableLinuxArm64 / linkDebugExecutableLinuxArm64.
TASK="link$(tr '[:lower:]' '[:upper:]' <<<"${MODE:0:1}")${MODE:1}ExecutableLinuxArm64"
cd "$ROOT/$POC"

echo "[$POC] linking ($MODE)..."
gradle "$TASK" --console=plain -PcmcRoot="$CMC"
KEXE="build/bin/linuxArm64/${MODE}Executable/${POC}.kexe"
[ -f "$KEXE" ] || { echo "no binary at $KEXE"; exit 1; }
echo "[$POC] binary: $(ls -lh "$KEXE" | awk '{print $5}')"

echo "[$POC] rendering in a Linux arm64 container (Xvfb, software GL)..."
mkdir -p out
# One shared, POC-agnostic runner image for every native POC (run-native.sh supplies the .kexe below).
docker build --platform linux/arm64 -f "$ROOT/scripts/docker/Dockerfile.run" -t poc-native-run "$ROOT/scripts/docker" >/dev/null
docker run --rm --platform linux/arm64 \
  -v "$PWD/$KEXE:/app/${POC}.kexe:ro" \
  -v "$PWD/out:/out" poc-native-run \
  bash -lc "timeout 60 xvfb-run -a -s '-screen 0 1024x768x24' /app/${POC}.kexe"

echo "[$POC] done. PNG captures in $ROOT/$POC/out/"
ls -la out/*.png 2>/dev/null || true
