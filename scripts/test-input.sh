#!/usr/bin/env bash
# Lot 1 acceptance test: drive the running Compose K/N app with REAL X11 input and check what happened.
#
# Runs INSIDE the container (see scripts/run-input-test.sh, which builds and launches it). The app runs
# under Xvfb with POC5_RUN_SECONDS set so it stays alive; xdotool then sends actual key, wheel and pointer
# events, and xclip reads the real system clipboard. Nothing is simulated in-process: if the GLFW callbacks
# or the compose actuals are not wired, these checks fail.
#
# Two things the first version of this script got wrong, worth keeping in mind:
#  - xdotool --window sends SYNTHETIC events (XSendEvent). GLFW delivers the wheel that way but the pointer
#    never actually moves, so Compose scrolled at (0,0). Driving without --window uses XTEST: a real pointer.
#  - The X11 clipboard is owned by a LIVE client. Reading it after the app exits always returns empty, so
#    the clipboard must be read WHILE the app is still running.
set -uo pipefail

APP=/app/poc5-native.kexe
LOG=/out/app.log
RESULT=/out/result.txt
: > "$RESULT"

pass() { echo "PASS: $1" | tee -a "$RESULT"; }
fail() { echo "FAIL: $1" | tee -a "$RESULT"; }

export POC5_RUN_SECONDS=30
"$APP" > "$LOG" 2>&1 &
APP_PID=$!

# Wait for the window to be mapped rather than sleeping blindly.
for _ in $(seq 1 60); do
  WID=$(xdotool search --name "POC5" 2>/dev/null | head -1) && [ -n "$WID" ] && break
  sleep 0.5
done
[ -n "${WID:-}" ] || { echo "FAIL: window never appeared" | tee -a "$RESULT"; cat "$LOG"; exit 1; }
pass "window mapped (id=$WID)"

xdotool windowactivate --sync "$WID" 2>/dev/null || true
xdotool windowfocus --sync "$WID" 2>/dev/null || true
# Window geometry, so we can aim at widgets in absolute screen coordinates (XTEST needs absolute).
eval "$(xdotool getwindowgeometry --shell "$WID")"   # sets X, Y, WIDTH, HEIGHT
echo "window at ${X},${Y} size ${WIDTH}x${HEIGHT}"
sleep 1

# Widget coordinates inside the 640x480 window (see App() in main.kt).
click_at() { xdotool mousemove --sync $((X + $1)) $((Y + $2)); sleep 0.3; xdotool click 1; sleep 0.5; }

# --- 1. KEYBOARD: click into the text field, then type. Proves char callback -> sendKeyEvent -> Compose.
click_at 320 90
xdotool type --delay 60 "hello"
sleep 1.5

# --- 2. SCROLL: real pointer over the LazyColumn, then wheel down (button 5).
xdotool mousemove --sync $((X + 320)) $((Y + 300))
sleep 0.5
for _ in 1 2 3 4 5; do xdotool click 5; sleep 0.2; done
sleep 1.5

# --- 3. CLIPBOARD: press "copy", which writes to the real X11 clipboard through GLFW.
click_at 172 172
sleep 1.5

# Read the clipboard WHILE the app still owns the selection.
CLIP=$(timeout 5 xclip -selection clipboard -o 2>/dev/null || echo "")

# --- 4. HOVER: move over the count button; Compose asks for a cursor via PlatformContext.setPointerIcon.
xdotool mousemove --sync $((X + 60)) $((Y + 172))
sleep 1

# --- 5. RESIZE: the surface, the render target and the scene size must all follow the window.
xdotool set_window --overrides 0 "$WID" 2>/dev/null || true
xdotool windowsize --sync "$WID" 800 600
sleep 2

# Let the app hit its own deadline and write the final frame.
wait "$APP_PID" 2>/dev/null || true
sleep 1

echo "---- app log ----"
cat "$LOG"
echo "-----------------"

# ---------------- assertions ----------------

if grep -q "typed codepoint=104" "$LOG" && grep -q "typed codepoint=101" "$LOG"; then
  pass "keyboard: real X11 key events reached Compose (h, e, ...)"
else
  fail "keyboard: no char events reached Compose"
fi

if grep -q "POC5: scroll dy=" "$LOG"; then
  pass "scroll: wheel events reached the mediator"
else
  fail "scroll: no wheel events reached the mediator"
fi

# The app prints the LazyColumn's first visible index into the UI; it also logs scrolls with the pointer
# position. A scroll at (0,0) means the pointer never moved -> Compose had nothing under the cursor.
if grep -q "POC5: scroll dy=.* at (0.0,0.0)" "$LOG"; then
  fail "scroll: events arrived at (0,0) -- the pointer never actually moved"
elif grep -q "POC5: scroll dy=" "$LOG"; then
  pass "scroll: events carried a real pointer position"
fi

if [ -n "$CLIP" ]; then
  pass "clipboard: system clipboard = '$CLIP'"
  case "$CLIP" in
    copied:hello) pass "clipboard: exactly 'copied:hello' -- typing AND copy-to-X11 both work" ;;
    copied:*)     pass "clipboard: app published '$CLIP'" ;;
    *)            fail "clipboard: unexpected content '$CLIP'" ;;
  esac
else
  fail "clipboard: empty (nothing was published to the X11 selection)"
fi

if grep -q "POC5: resized to 800x600" "$LOG"; then
  pass "resize: window resize rebuilt the surface and the scene (800x600)"
else
  fail "resize: the app never saw the resize"
fi

# The mediator logs the cursor shape Compose asked for. Over a text field Compose wants an I-beam; if
# setPointerIcon is never called, the cursor stays an arrow forever.
if grep -q "POC5: cursor ->" "$LOG"; then
  pass "cursor: Compose requested a cursor shape ($(grep -o 'POC5: cursor -> [A-Za-z]*' "$LOG" | sort -u | tr '\n' ' '))"
else
  fail "cursor: setPointerIcon was never called"
fi

[ -f /out/poc5-final.png ] && pass "final screenshot written" || fail "no final screenshot"

echo
echo "==== SUMMARY ===="
cat "$RESULT"
grep -q "^FAIL" "$RESULT" && exit 1
exit 0
