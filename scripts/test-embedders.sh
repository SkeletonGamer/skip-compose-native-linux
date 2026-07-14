#!/usr/bin/env bash
# Jalon 13: prove the window toolkit is not baked into Compose.
#
# Builds the GTK4 and Qt6 embedders, which drive the SAME compose klib and the SAME 42 Linux actuals as the
# GLFW build, and checks two things that a human cannot fake:
#
#   1. the binaries need ZERO glfw symbols from the system (readelf, not a guess), so nothing inside Compose
#      is nailed to a toolkit any more;
#   2. each one actually renders material3 and a click on the Button increments the counter.
#
# Background: Jake Wharton's objection on the Kotlin Slack was that expect/actual "assumes there is only a
# single, canonical UI toolkit for each build target [...] If you actualize to GTK then it would be
# impossible to use for Qt". This script is the answer, run rather than argued.
#
# Usage: scripts/test-embedders.sh
# Prereqs: scripts/fetch-deps.sh (stages GTK4, builds the Qt C++ shim).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CMC="${CMC_ROOT:-$ROOT/.cmc}"
OUT="$(mktemp -d)"; chmod 777 "$OUT"
PASS=0; FAIL=0
ok()   { echo "PASS: $1"; PASS=$((PASS+1)); }
bad()  { echo "FAIL: $1"; FAIL=$((FAIL+1)); }

cd "$ROOT/poc5-native"
echo "[embedders] linking the GTK4 and Qt6 embedders (neither links -lglfw)..."
gradle linkGtkDebugExecutableLinuxArm64 linkQtDebugExecutableLinuxArm64 \
  --console=plain -PcmcRoot="$CMC" -q

BIN="$ROOT/poc5-native/build/bin/linuxArm64"
[ -f "$BIN/gtkDebugExecutable/gtk.kexe" ] || { echo "FAIL: no GTK binary"; exit 1; }
[ -f "$BIN/qtDebugExecutable/qt.kexe" ]   || { echo "FAIL: no Qt binary"; exit 1; }

# --- 1. What does each binary actually demand from the system? ---
echo "[embedders] reading the binaries (readelf, inside Linux)..."
SYMS=$(docker run --rm --platform linux/arm64 -v "$BIN:/b" debian:trixie-slim bash -lc '
  apt-get update -qq >/dev/null 2>&1
  apt-get install -y --no-install-recommends binutils >/dev/null 2>&1
  for t in gtkDebugExecutable/gtk.kexe qtDebugExecutable/qt.kexe debugExecutable/poc5-native.kexe; do
    n=$(readelf -sW "/b/$t" | awk "\$7==\"UND\"" | grep -ci glfw || true)
    echo "$t $n"
  done')
echo "$SYMS" | sed 's/^/   /'

gtk_n=$(echo "$SYMS" | awk '/gtk.kexe/{print $2}')
qt_n=$(echo "$SYMS"  | awk '/qt.kexe/{print $2}')
glfw_n=$(echo "$SYMS"| awk '/poc5-native.kexe/{print $2}')

[ "$gtk_n" = "0" ] && ok "GTK binary needs no GLFW symbol from the system (0)" \
                   || bad "GTK binary still needs $gtk_n GLFW symbols"
[ "$qt_n" = "0" ]  && ok "Qt binary needs no GLFW symbol from the system (0)" \
                   || bad "Qt binary still needs $qt_n GLFW symbols"
# The control: the GLFW build obviously DOES need them. If this is 0 too, the check above proves nothing.
[ "$glfw_n" -gt 0 ] && ok "control: the GLFW build does need GLFW ($glfw_n symbols), so the check is real" \
                    || bad "control failed: the GLFW build needs no GLFW either, the test is measuring nothing"

# --- 2. Do they render, and does a click reach the material3 Button? ---
run_embedder() {
  local name="$1" dir="$2" exe="$3" pkgs="$4" env="$5"
  echo "[embedders] running the $name app under Xvfb..."
  docker run --rm --platform linux/arm64 \
    -v "$BIN/$dir:/app" -v "$ROOT/poc5-native/native/qt/lib:/qtlib" -v "$OUT:/out" \
    debian:trixie-slim bash -lc "
    apt-get update -qq >/dev/null 2>&1
    apt-get install -y --no-install-recommends $pkgs libgles2 libegl1 libegl-mesa0 \
      libgl1-mesa-dri xvfb fontconfig fonts-dejavu-core >/dev/null 2>&1
    Xvfb :99 -screen 0 1024x768x24 >/dev/null 2>&1 & sleep 3
    export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 $env
    timeout 90 /app/$exe 2>&1 | grep -viE 'gtk-warning|dbus|locale'
  " | tee "$OUT/$name.log" | sed 's/^/   /'

  grep -q "clicks=1" "$OUT/$name.log" \
    && ok "$name: material3 rendered and a click on the Button incremented the counter" \
    || bad "$name: the click never reached Compose (see $OUT/$name.log)"
}

run_embedder gtk gtkDebugExecutable gtk.kexe "libgtk-4-1" "GDK_BACKEND=x11"
run_embedder qt  qtDebugExecutable  qt.kexe  "libqt6gui6 libqt6core6t64 qt6-qpa-plugins" \
             "QT_QPA_PLATFORM=xcb LD_LIBRARY_PATH=/qtlib"

echo
echo "==== SUMMARY ===="
echo "$PASS passed, $FAIL failed"
echo "screenshots: $OUT"
[ "$FAIL" -eq 0 ] || exit 1
