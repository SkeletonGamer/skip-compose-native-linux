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

# --- 2. native libs + headers, per Linux architecture ---
# arm64 is the default target of every native POC; x64 exists so POC 5 can prove the compose stack is
# arch-neutral (same sources, both Linux architectures). Headers are arch-independent: staged once.
# $1 = docker platform, $2 = Debian multiarch triplet, $3 = stage dir
extract_native() {
  local platform="$1" triplet="$2" stage="$3"
  [ -f "$stage/lib/libglfw.so.3" ] && return 0
  echo "[native] extracting GLFW/GL/EGL/fontconfig/freetype for $platform (docker)..."
  rm -rf "$stage"; mkdir -p "$stage/lib" "$stage/include"
  # trixie, not bookworm: it ships GLFW 3.4, which selects X11 or Wayland AT RUNTIME (glfwGetPlatform,
  # backends dlopen'd, so neither appears in the binary's NEEDED). GLFW 3.3 picks its backend at compile
  # time and Debian ships two incompatible packages, so a 3.3 binary is X11-only. Wayland is not optional:
  # Ubuntu Budgie 26.04 ships no X11 session at all.
  docker run --rm --platform "$platform" -v "$stage:/stage" -e TRIPLET="$triplet" \
    debian:trixie-slim bash -lc '
    set -e
    # Some Docker/network setups break apt gpgv on the release signatures even though the download itself
    # is intact (verified: wget fetches the InRelease fine). Mark the repos Trusted so apt skips the gpg
    # step and relies on the package checksums instead. We only extract stock .so files; nothing runs here.
    cat > /etc/apt/sources.list.d/debian.sources <<EOF
Types: deb
URIs: http://deb.debian.org/debian
Suites: trixie trixie-updates
Components: main
Trusted: yes

Types: deb
URIs: http://deb.debian.org/debian-security
Suites: trixie-security
Components: main
Trusted: yes
EOF
    apt-get update -qq || { sleep 3; apt-get update -qq; }
    apt-get install -y --no-install-recommends \
      libglfw3 libglfw3-dev \
      libgl1-mesa-dri libglx-mesa0 libgl1 libegl1 libgles2 \
      libfontconfig1 libfontconfig-dev libfreetype6 libfreetype-dev >/dev/null
    cp -aL /usr/lib/$TRIPLET/libglfw.so* /stage/lib/ 2>/dev/null || true
    # GLESv2 as well as GL: the binary links against GLESv2, not desktop GL. Desktop libGL carries GLX and
    # therefore drags libX11 in, which was the last X11 tie in an otherwise Wayland-capable binary.
    cp -aL /usr/lib/$TRIPLET/libGL.so* /usr/lib/$TRIPLET/libEGL.so* /usr/lib/$TRIPLET/libGLESv2.so* /stage/lib/ 2>/dev/null || true
    # The linker resolves -lGLESv2 through the unversioned soname, which the runtime package does not ship.
    ln -sf libGLESv2.so.2 /stage/lib/libGLESv2.so 2>/dev/null || true
    ln -sf libEGL.so.1 /stage/lib/libEGL.so 2>/dev/null || true
    cp -aL /usr/lib/$TRIPLET/libfontconfig.so* /usr/lib/$TRIPLET/libfreetype.so* /stage/lib/ 2>/dev/null || true
    # The 3.4 headers matter as much as the library: glfwGetPlatform / GLFW_PLATFORM do not exist in 3.3.
    cp -aL /usr/include/GLFW/glfw3.h /usr/include/GLFW/glfw3native.h /stage/include/ 2>/dev/null || true
  ' || true
  # Do not fail silently: if the apt step could not produce the libs, say so with a concrete hint.
  if [ ! -f "$stage/lib/libglfw.so.3" ]; then
    echo "[native] ERROR: could not extract the Linux native libs for $platform via Docker." >&2
    echo "[native] The Debian apt step failed (often a transient Docker network / apt-signature glitch)." >&2
    echo "[native] Fix: make sure Docker is running, then re-run scripts/fetch-deps.sh." >&2
    echo "[native] If it persists: 'docker pull debian:bookworm-slim' and retry, or check your network/proxy." >&2
    exit 1
  fi
}

STAGE="$ROOT/.native-stage"
STAGE_X64="$ROOT/.native-stage-x64"
extract_native linux/arm64 aarch64-linux-gnu "$STAGE"
extract_native linux/amd64 x86_64-linux-gnu  "$STAGE_X64"

# --- 3. Wayland text-input-v3: headers + protocol bindings (the IME path) ---
# Under Wayland the app speaks zwp_text_input_v3 to the COMPOSITOR, which relays to the input method; it
# does not talk to IBus. The protocol code is GENERATED from the XML by wayland-scanner, so none of it is
# vendored here: headers, the generated .c/.h and libwayland-client are all produced/fetched at setup time.
WL="$ROOT/poc5-native/native/wayland"
if [ ! -f "$WL/text-input-v3-client-protocol.h" ]; then
  echo "[wayland] generating text-input-v3 bindings + staging wayland headers (docker)..."
  mkdir -p "$WL"
  docker run --rm --platform linux/arm64 -v "$WL:/out" debian:trixie-slim bash -lc '
    set -e
    apt-get update -qq >/dev/null 2>&1
    apt-get install -y --no-install-recommends libwayland-dev wayland-protocols >/dev/null 2>&1
    XML=/usr/share/wayland-protocols/unstable/text-input/text-input-unstable-v3.xml
    wayland-scanner client-header "$XML" /out/text-input-v3-client-protocol.h
    wayland-scanner private-code  "$XML" /out/text-input-v3-protocol.c
    cp /usr/include/wayland-client.h /usr/include/wayland-client-core.h \
       /usr/include/wayland-client-protocol.h /usr/include/wayland-util.h \
       /usr/include/wayland-version.h /out/
    cp -aL /usr/lib/aarch64-linux-gnu/libwayland-client.so* /out/
  ' || { echo "[wayland] ERROR: could not generate the text-input-v3 bindings." >&2; exit 1; }
fi

# The IME test harness needs input-method-v2, which is a wlroots protocol: NOT in wayland-protocols, and not
# packaged by Debian. It lives in the wlroots repo. Only the test-side fake IME uses it.
PROTO="$ROOT/scripts/docker/protocols"
if [ ! -f "$PROTO/input-method-unstable-v2.xml" ]; then
  echo "[wayland] fetching the input-method-v2 protocol (wlroots)..."
  mkdir -p "$PROTO"
  curl -sSL --max-time 30 -o "$PROTO/input-method-unstable-v2.xml" \
    "https://raw.githubusercontent.com/swaywm/wlroots/master/protocol/input-method-unstable-v2.xml"
  grep -q "zwp_input_method_v2" "$PROTO/input-method-unstable-v2.xml" || {
    echo "[wayland] ERROR: the downloaded input-method XML looks wrong (an HTML error page?)." >&2; exit 1; }
fi

# Distribute to each native POC that links against GLFW: lib/ = arm64, lib-x64/ = x86_64.
for p in poc3-native poc4-native poc5-native poc6-native; do
  [ -d "$ROOT/$p" ] || continue
  mkdir -p "$ROOT/$p/native/glfw/lib" "$ROOT/$p/native/glfw/lib-x64" "$ROOT/$p/native/glfw/include/GLFW"
  cp -a "$STAGE/lib/." "$ROOT/$p/native/glfw/lib/" 2>/dev/null || true
  cp -a "$STAGE_X64/lib/." "$ROOT/$p/native/glfw/lib-x64/" 2>/dev/null || true
  cp -a "$STAGE/include/glfw3.h" "$STAGE/include/glfw3native.h" \
     "$ROOT/$p/native/glfw/include/GLFW/" 2>/dev/null || true
done

# libwayland-client goes next to the other libs, so the linker finds -lwayland-client.
cp -a "$WL"/libwayland-client.so* "$ROOT/poc5-native/native/glfw/lib/" 2>/dev/null || true
ln -sf libwayland-client.so.0 "$ROOT/poc5-native/native/glfw/lib/libwayland-client.so" 2>/dev/null || true

echo "[done] .cmc + native libs ready. For POC 6 also run 'skip export' (see README)."
