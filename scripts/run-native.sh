#!/usr/bin/env bash
# Build, link and render a Kotlin/Native Linux POC (no JVM) in a Linux container under Xvfb.
# Usage: scripts/run-native.sh <poc3-native|poc4-native|poc5-native|poc6-native> [debug|release] [arm64|x64]
# arm64 is the default; x64 is only wired for poc5-native (proves the compose stack is arch-neutral) and
# runs under qemu emulation on an ARM host, so it is much slower -- hence the larger timeout below.
# Prereqs: scripts/fetch-deps.sh (compose checkout + native libs); POC 6 also needs ./export (skip export).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
POC="${1:?usage: run-native.sh <poc*-native> [debug|release] [arm64|x64]}"
MODE="${2:-release}"
ARCH="${3:-arm64}"
CMC="${CMC_ROOT:-$ROOT/.cmc}"
[ -d "$ROOT/$POC" ] || { echo "unknown POC: $POC"; exit 1; }

case "$ARCH" in
  arm64) KTARGET="LinuxArm64"; KDIR="linuxArm64"; PLATFORM="linux/arm64"; TIMEOUT=60 ;;
  x64)   KTARGET="LinuxX64";   KDIR="linuxX64";   PLATFORM="linux/amd64"; TIMEOUT=300 ;;
  *) echo "unknown arch: $ARCH (expected arm64 or x64)"; exit 1 ;;
esac

# Gradle task name: linkReleaseExecutableLinuxArm64 / linkDebugExecutableLinuxX64 / ...
TASK="link$(tr '[:lower:]' '[:upper:]' <<<"${MODE:0:1}")${MODE:1}Executable${KTARGET}"
cd "$ROOT/$POC"

echo "[$POC] linking ($MODE, $ARCH)..."
gradle "$TASK" --console=plain -PcmcRoot="$CMC"
KEXE="build/bin/${KDIR}/${MODE}Executable/${POC}.kexe"
[ -f "$KEXE" ] || { echo "no binary at $KEXE"; exit 1; }
echo "[$POC] binary: $(ls -lh "$KEXE" | awk '{print $5}')"

echo "[$POC] rendering in a Linux $ARCH container (Xvfb, software GL)..."
mkdir -p out
# One shared, POC-agnostic runner image for every native POC (run-native.sh supplies the .kexe below).
docker build --platform "$PLATFORM" -f "$ROOT/scripts/docker/Dockerfile.run" -t "poc-native-run-$ARCH" "$ROOT/scripts/docker" >/dev/null
docker run --rm --platform "$PLATFORM" \
  -v "$PWD/$KEXE:/app/${POC}.kexe:ro" \
  -v "$PWD/out:/out" "poc-native-run-$ARCH" \
  bash -lc "timeout $TIMEOUT xvfb-run -a -s '-screen 0 1024x768x24' /app/${POC}.kexe"

echo "[$POC] done. PNG captures in $ROOT/$POC/out/"
ls -la out/*.png 2>/dev/null || true
