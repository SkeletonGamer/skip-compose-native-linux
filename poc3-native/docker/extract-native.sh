#!/usr/bin/env bash
# Extract the arm64 GLFW/GL/EGL headers + shared libs the Kotlin/Native cross-link needs, from a
# Linux arm64 container into native/glfw/ (git-ignored). Run once before building the windowed path.
# Requires the poc3-native image (docker build -f docker/Dockerfile -t poc3-native .).
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$here/native/glfw/include" "$here/native/glfw/lib"

docker run --rm -v "$here/native:/native" poc3-native bash -lc '
  apt-get update >/dev/null 2>&1
  apt-get install -y --no-install-recommends libglfw3-dev libegl1-mesa-dev >/dev/null 2>&1
  cp -r /usr/include/GLFW /native/glfw/include/
  for l in libglfw.so libglfw.so.3 libGL.so libGL.so.1 libEGL.so libEGL.so.1; do
    f=$(find /usr/lib/aarch64-linux-gnu -name "$l" 2>/dev/null | head -1)
    [ -n "$f" ] && cp -L "$f" /native/glfw/lib/
  done
  echo "extracted:"; ls /native/glfw/include/GLFW/ | head; ls /native/glfw/lib/
'
echo ">> done: native/glfw/{include,lib}"
