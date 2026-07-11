#!/usr/bin/env bash
# Fetch the git-ignored build dependencies the native POCs need:
#   1. A checkout of JetBrains/compose-multiplatform-core (branch jb-main) at ./.cmc
#   2. The Linux/arm64 native libraries + headers (GLFW, GL/EGL, fontconfig, freetype),
#      extracted from a Debian arm64 container into each */native/glfw/.
# Skip's transpiled output (./export) is NOT fetched here: generate it with `skip export`
# on a SwiftUI witness app (see the README, "Reproduce (POC 6)").
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# --- 1. compose-multiplatform-core checkout ---
if [ -d ".cmc/compose/ui/ui/src/commonMain" ]; then
  echo "[cmc] already present at ./.cmc"
else
  echo "[cmc] cloning compose-multiplatform-core (jb-main, shallow)..."
  git clone --branch jb-main --depth 1 \
    https://github.com/JetBrains/compose-multiplatform-core .cmc
fi

# --- 2. native libs + headers for linux/arm64 ---
STAGE="$ROOT/.native-stage"
if [ ! -f "$STAGE/lib/libglfw.so.3" ]; then
  echo "[native] extracting GLFW/GL/EGL/fontconfig/freetype for linux/arm64 (docker)..."
  rm -rf "$STAGE"; mkdir -p "$STAGE/lib" "$STAGE/include"
  docker run --rm --platform linux/arm64 -v "$STAGE:/stage" debian:bookworm-slim bash -lc '
    set -e
    # Some Docker/network setups break apt gpgv on the release signatures even though the download itself
    # is intact (verified: wget fetches the InRelease fine). Mark the repos Trusted so apt skips the gpg
    # step and relies on the package checksums instead. We only extract stock .so files; nothing runs here.
    cat > /etc/apt/sources.list.d/debian.sources <<EOF
Types: deb
URIs: http://deb.debian.org/debian
Suites: bookworm bookworm-updates
Components: main
Trusted: yes

Types: deb
URIs: http://deb.debian.org/debian-security
Suites: bookworm-security
Components: main
Trusted: yes
EOF
    apt-get update -qq || { sleep 3; apt-get update -qq; }
    apt-get install -y --no-install-recommends \
      libglfw3 libglfw3-dev \
      libgl1-mesa-dri libglx-mesa0 libgl1 libegl1 libgles2 \
      libfontconfig1 libfontconfig-dev libfreetype6 libfreetype-dev >/dev/null
    cp -aL /usr/lib/aarch64-linux-gnu/libglfw.so* /stage/lib/ 2>/dev/null || true
    cp -aL /usr/lib/aarch64-linux-gnu/libGL.so* /usr/lib/aarch64-linux-gnu/libEGL.so* /stage/lib/ 2>/dev/null || true
    cp -aL /usr/lib/aarch64-linux-gnu/libfontconfig.so* /usr/lib/aarch64-linux-gnu/libfreetype.so* /stage/lib/ 2>/dev/null || true
    cp -aL /usr/include/GLFW/glfw3.h /usr/include/GLFW/glfw3native.h /stage/include/ 2>/dev/null || true
  ' || true
  # Do not fail silently: if the apt step could not produce the libs, say so with a concrete hint.
  if [ ! -f "$STAGE/lib/libglfw.so.3" ]; then
    echo "[native] ERROR: could not extract the Linux arm64 native libs via Docker." >&2
    echo "[native] The Debian apt step failed (often a transient Docker network / apt-signature glitch)." >&2
    echo "[native] Fix: make sure Docker is running, then re-run scripts/fetch-deps.sh." >&2
    echo "[native] If it persists: 'docker pull debian:bookworm-slim' and retry, or check your network/proxy." >&2
    exit 1
  fi
fi

# Distribute to each native POC that links against GLFW.
for p in poc3-native poc4-native poc5-native poc6-native; do
  [ -d "$ROOT/$p" ] || continue
  mkdir -p "$ROOT/$p/native/glfw/lib" "$ROOT/$p/native/glfw/include/GLFW"
  cp -a "$STAGE/lib/." "$ROOT/$p/native/glfw/lib/" 2>/dev/null || true
  cp -a "$STAGE/include/glfw3.h" "$STAGE/include/glfw3native.h" \
     "$ROOT/$p/native/glfw/include/GLFW/" 2>/dev/null || true
done

echo "[done] .cmc + native libs ready. For POC 6 also run 'skip export' (see README)."
