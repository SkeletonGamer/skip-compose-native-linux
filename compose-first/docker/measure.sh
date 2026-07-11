#!/usr/bin/env bash
# Runtime-weight measurements on Linux arm64 (Question B): distributable size (jpackage/jlink app
# image + bundled JRE), cold start to first window, idle RSS. Prints raw numbers.
set -u
export DISPLAY=:99
Xvfb :99 -screen 0 1280x900x24 >/tmp/xvfb.log 2>&1 &
sleep 2
cd /app/compose-first

echo "== arch =="
uname -m
java -version 2>&1 | head -1

echo "== building app image (jpackage + jlink) =="
gradle --no-daemon createDistributable >/tmp/dist.log 2>&1 || { echo "BUILD FAILED"; tail -40 /tmp/dist.log; exit 1; }
APPDIR=$(find build/compose/binaries/main/app -maxdepth 1 -mindepth 1 -type d | head -1)
echo "APPDIR=$APPDIR"

echo "== distributable size =="
echo -n "app_image_total: "; du -sh "$APPDIR" | cut -f1
echo -n "bundled_runtime : "; du -sh "$APPDIR/lib/runtime" 2>/dev/null | cut -f1
echo -n "app_jars_lib    : "; du -sh "$APPDIR/lib/app" 2>/dev/null | cut -f1

BIN=$(find "$APPDIR/bin" -maxdepth 1 -type f | head -1)
echo "launcher=$BIN"

echo "== cold start to first window =="
T0=$(date +%s%N)
"$BIN" >/tmp/app.log 2>&1 &
WID=""
for _ in $(seq 1 200); do WID=$(xdotool search --name "Compose-first" 2>/dev/null | head -1); [ -n "$WID" ] && break; sleep 0.05; done
T1=$(date +%s%N)
echo "cold_start_ms=$(( (T1 - T0) / 1000000 ))"

echo "== idle RSS after 4s =="
sleep 4
# jpackage renames the process to the app name, so resolve the PID from the window.
PID=$(xdotool getwindowpid "$WID" 2>/dev/null)
if [ -n "$PID" ]; then
    ps -o rss= -p "$PID" | awk '{printf "idle_rss_mb=%.1f\n", $1/1024}'
else
    echo "idle_rss_mb=UNKNOWN (window pid not found)"
fi

kill "$PID" 2>/dev/null || pkill -f "compose-first" 2>/dev/null || true
echo "== done =="
