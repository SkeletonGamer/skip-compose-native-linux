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

export POC5_RUN_SECONDS=60
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

# Widgets are located by TAG, not by pixel. The app walks Compose's semantics tree (what Modifier.testTag
# writes into) and dumps "tag x y w h" per line to /out/tags.txt. Hardcoded coordinates used to break
# silently whenever the UI gained a line: the clicks landed on labels, and the tests still "passed" because
# the events did reach Compose, just not the widget.
TAGS=/out/tags.txt
for _ in $(seq 1 40); do [ -s "$TAGS" ] && break; sleep 0.25; done
[ -s "$TAGS" ] || { echo "FAIL: the app never exported its semantics tags" | tee -a "$RESULT"; exit 1; }
echo "tags:"; cat "$TAGS"
pass "semantics: the app exported its tagged widgets ($(wc -l < "$TAGS" | tr -d ' ') tags)"

# centre_of <tag> -> "cx cy", the centre of that widget in window coordinates. awk, not python: the image's
# python3-minimal has no json module, and it failed silently -- every click went to the screen centre.
centre_of() { awk -v t="$1" '$1 == t { print $2 + $4 / 2, $3 + $5 / 2; found = 1 } END { exit !found }' "$TAGS"; }
move_to_tag() {
  local coords cx cy
  coords=$(centre_of "$1") || { fail "tag '$1' not found in the semantics dump"; return 1; }
  cx=${coords% *}; cy=${coords#* }
  [ -n "$cx" ] && [ -n "$cy" ] || { fail "tag '$1' has no coordinates"; return 1; }
  xdotool mousemove --sync $((X + cx)) $((Y + cy))
}
click_tag() { move_to_tag "$1" || return; sleep 0.3; xdotool click 1; sleep 0.5; }
hover_tag() { move_to_tag "$1" || return; sleep 0.5; }

# --- 1. KEYBOARD: click into the text field, then type. Proves char callback -> sendKeyEvent -> Compose.
click_tag field
xdotool type --delay 60 "hello"
sleep 1.5

# --- 2. SCROLL: real pointer over the LazyColumn, then wheel down (button 5).
hover_tag list
for _ in 1 2 3 4 5; do xdotool click 5; sleep 0.2; done
sleep 1.5

# --- 3. CLIPBOARD: press "copy", which writes to the real X11 clipboard via GLFW.
click_tag copy
sleep 1.5

# Read the clipboard WHILE the app still owns the selection.
CLIP=$(timeout 5 xclip -selection clipboard -o 2>/dev/null || echo "")

# --- 4. HOVER: move over the text field; Compose asks for an I-beam via PlatformContext.setPointerIcon.
hover_tag field
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

# Reaching Compose is not enough: the text must land in the FIELD, which needs the click to have focused
# it. The clipboard content below is the end-to-end proof, since the copy button copies the typed text.

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
