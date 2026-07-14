#!/usr/bin/env bash
# Wayland acceptance test: the app must run NATIVELY on Wayland, not through XWayland.
#
# Runs inside the container. Starts sway with the headless wlroots backend (the same wlroots family that
# labwc uses, which is what Budgie 10.10 moved to), then runs the app with only WAYLAND_DISPLAY set and no
# DISPLAY at all. If GLFW fell back to X11 there would be no X server to fall back TO, so the app would die:
# reaching a rendered frame is itself the proof that the Wayland backend works.
#
# Input is injected with wtype (the virtual-keyboard Wayland protocol). xdotool cannot be used here: Wayland
# forbids one client from injecting events into another, which is exactly the security model X11 lacked.
set -uo pipefail

APP=/app/poc5-native.kexe
RESULT=/out/wayland-result.txt
LOG=/out/wayland-app.log
: > "$RESULT"

pass() { echo "PASS: $1" | tee -a "$RESULT"; }
fail() { echo "FAIL: $1" | tee -a "$RESULT"; }

mkdir -p "$XDG_RUNTIME_DIR" && chmod 700 "$XDG_RUNTIME_DIR"

# Start the compositor. WLR_BACKENDS=headless + pixman renderer: no GPU, no seat, no DRM node needed.
sway --config /dev/null > /out/sway.log 2>&1 &
SWAY_PID=$!

# Wait for the compositor's socket rather than sleeping blindly.
for _ in $(seq 1 60); do
  SOCK=$(ls "$XDG_RUNTIME_DIR"/wayland-* 2>/dev/null | grep -v '\.lock$' | head -1)
  [ -n "$SOCK" ] && break
  sleep 0.5
done
[ -n "${SOCK:-}" ] || { fail "the compositor never came up"; cat /out/sway.log; exit 1; }
export WAYLAND_DISPLAY=$(basename "$SOCK")
pass "wlroots compositor up (WAYLAND_DISPLAY=$WAYLAND_DISPLAY)"

# The point of the test: NO DISPLAY. There is no X server here, so nothing to fall back to.
unset DISPLAY

POC5_RUN_SECONDS=30 timeout 90 "$APP" > "$LOG" 2>&1 &
APP_PID=$!
sleep 6

# --- 1. Did GLFW actually pick Wayland?
grep -m1 "POC5: glfw init ok" "$LOG" || true
if grep -q "platform = Wayland" "$LOG"; then
  pass "GLFW selected the Wayland backend at runtime (no XWayland, no X server present)"
else
  fail "GLFW did not select Wayland ($(grep -m1 'platform =' "$LOG"))"
fi
if grep -q "wayland supported = true, x11 supported = true" "$LOG"; then
  pass "one binary, both backends available (GLFW 3.4 runtime selection)"
else
  fail "the binary does not carry both backends ($(grep -m1 'supported' "$LOG"))"
fi

# --- 2. Did it actually render? A GL surface on Wayland means EGL, not GLX.
if grep -q "skiko GL surface ok" "$LOG"; then
  pass "skiko created a GL surface on Wayland (EGL path)"
else
  fail "no GL surface on Wayland"
fi

# --- 3. Keyboard, through the Wayland virtual-keyboard protocol.
# Each wtype invocation creates a throwaway virtual keyboard, sends its OWN keymap, then types. The first
# keystroke of every invocation is lost while the client reloads that keymap (measured: a separate warm-up
# call produced zero events, and "wayland" always arrived as "ayland"). So the sacrificial character goes
# INSIDE the same string: "." is eaten, "wayland" arrives whole.
wtype -d 120 ".wayland" 2>/dev/null || echo "(wtype failed)"
sleep 4

wait "$APP_PID" 2>/dev/null || true
sleep 1

if grep -q "typed codepoint=119" "$LOG" && grep -q "typed codepoint=97" "$LOG"; then
  pass "keyboard: real Wayland key events reached Compose (w, a, ...)"
else
  fail "keyboard: no key events arrived over Wayland"
fi

if grep -q "POC5: exiting after" "$LOG"; then
  pass "the app ran and exited cleanly on Wayland"
else
  fail "the app did not survive on Wayland"
fi

[ -f /out/poc5-final.png ] && pass "rendered a frame on Wayland (screenshot written)" || fail "no frame rendered"

kill "$SWAY_PID" 2>/dev/null || true

echo
echo "---- app log ----"; cat "$LOG"
echo
echo "==== SUMMARY ===="
cat "$RESULT"
grep -q "^FAIL" "$RESULT" && exit 1
exit 0
