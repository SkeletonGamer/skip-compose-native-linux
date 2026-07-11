#!/usr/bin/env bash
# Runs the windowed CMP app on a real Xvfb X server, captures the framebuffer, drives navigation
# with xdotool, and checks persistence across a process restart. Writes PNGs to /out.
set -u

export DISPLAY=:99
Xvfb :99 -screen 0 1280x900x24 >/tmp/xvfb.log 2>&1 &
sleep 2

# Reset persisted counter so the run starts from a known state.
echo 'java.util.prefs.Preferences.userRoot().node("dev/skeletongamer/composefirst").clear();' | jshell -q - 2>/dev/null || true

launch_app() {
    ( cd /app/compose-first && gradle --no-daemon run >/tmp/app.log 2>&1 & echo $! >/tmp/app.gradle.pid )
    for _ in $(seq 1 90); do
        if xdotool search --name "Compose-first" >/dev/null 2>&1; then return 0; fi
        sleep 1
    done
    echo "!! window never appeared"; tail -20 /tmp/app.log; return 1
}

kill_app() {
    pkill -f "app.MainKt" 2>/dev/null || true
    sleep 2
}

shot() { import -window root "/out/$1" && echo ">> wrote /out/$1"; }

# ---- Phase 1: render + interactivity + navigation ----
launch_app || exit 1
WID=$(xdotool search --name "Compose-first" | head -1)
xdotool windowmove "$WID" 0 0 2>/dev/null || true
sleep 3
shot poc2-linux-home.png                       # Count: 0

xdotool mousemove "${PLUS_X:-116}" "${PLUS_Y:-80}" click 1 ; sleep 1
xdotool mousemove "${PLUS_X:-116}" "${PLUS_Y:-80}" click 1 ; sleep 1
shot poc2-linux-incremented.png                # Count: 2 + Positive

xdotool mousemove "${DETAILS_X:-80}" "${DETAILS_Y:-144}" click 1 ; sleep 2
shot poc2-linux-detail.png                     # Detail screen with < Back

# ---- Phase 2: persistence across a process restart ----
kill_app
launch_app || exit 1
WID=$(xdotool search --name "Compose-first" | head -1)
xdotool windowmove "$WID" 0 0 2>/dev/null || true
sleep 3
shot poc2-linux-persisted.png                  # fresh process should show Count: 2

kill_app
echo ">> done"
