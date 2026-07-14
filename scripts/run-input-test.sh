#!/usr/bin/env bash
# Build + link poc5-native, then run the Lot 1 acceptance test (scripts/test-input.sh) in the container.
# Usage: scripts/run-input-test.sh [arm64|x64]
# Unlike run-native.sh (which just renders and screenshots), this drives the app with real X11 input.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ARCH="${1:-arm64}"
CMC="${CMC_ROOT:-$ROOT/.cmc}"
POC=poc5-native

case "$ARCH" in
  arm64) KTARGET="LinuxArm64"; KDIR="linuxArm64"; PLATFORM="linux/arm64" ;;
  x64)   KTARGET="LinuxX64";   KDIR="linuxX64";   PLATFORM="linux/amd64" ;;
  *) echo "unknown arch: $ARCH"; exit 1 ;;
esac

cd "$ROOT/$POC"
echo "[$POC] linking (debug, $ARCH)..."
gradle "linkDebugExecutable${KTARGET}" --console=plain -PcmcRoot="$CMC"
KEXE="build/bin/${KDIR}/debugExecutable/${POC}.kexe"
[ -f "$KEXE" ] || { echo "no binary at $KEXE"; exit 1; }

mkdir -p out
# BUILDKIT=0: buildkit re-resolves the base image against the registry even when it is already local,
# and times out on a slow/offline network. The legacy builder uses the cached image.
DOCKER_BUILDKIT=0 docker build --platform "$PLATFORM" -f "$ROOT/scripts/docker/Dockerfile.run" \
  -t "poc-native-run-$ARCH" "$ROOT/scripts/docker" >/dev/null

echo "[$POC] running the input acceptance test under Xvfb ($ARCH)..."
docker run --rm --platform "$PLATFORM" \
  -v "$PWD/$KEXE:/app/${POC}.kexe:ro" \
  -v "$ROOT/scripts/test-input.sh:/app/test-input.sh:ro" \
  -v "$PWD/out:/out" "poc-native-run-$ARCH" \
  bash -lc "timeout 180 xvfb-run -a -s '-screen 0 1024x768x24' bash /app/test-input.sh"
