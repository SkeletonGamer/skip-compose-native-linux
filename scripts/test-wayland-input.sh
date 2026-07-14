#!/usr/bin/env bash
# Wayland input automation: the X11 checks (test-input.sh), driven through Wayland protocols.
#
# Why this looks nothing like the X11 suite: Wayland FORBIDS a client from injecting events into another
# client. There is no xdotool equivalent. Every input needs an explicit protocol AND a compositor that
# cooperates:
#
#   keyboard  -> wtype     (zwp_virtual_keyboard_v1)      WORKS headless
#   pointer   -> wlrctl    (zwlr_virtual_pointer_v1)      DOES NOT WORK headless, see below
#   clipboard -> wl-paste  (wl_data_device)               needs a pointer/keyboard focus to be useful
#   geometry  -> swaymsg   (the client cannot resize itself; the compositor owns geometry)
#
# THE POINTER LIMIT, measured, not assumed:
#   `swaymsg -t get_seats` reports "capabilities": 0 on the headless wlroots backend. No input device is
#   attached, so the seat advertises neither pointer nor keyboard, so a client never creates a wl_pointer,
#   so virtual-pointer events (wlrctl) and sway's own cursor commands are both delivered to nobody. This was
#   verified directly: five wlrctl move+click rounds and swaymsg cursor set/press both produced ZERO pointer
#   events in the app.
#
#   This is a property of the HEADLESS COMPOSITOR, not of the app. Pointer handling in the mediator is the
#   same code on both platforms (GLFW callbacks; the mediator has no X11/Wayland branch at all), and it is
#   proven under X11 by test-input.sh. So pointer checks are reported as SKIP here rather than faked.
#
#   To close this properly you need a seat with a real input device: run the app against a compositor with
#   a real pointer (a desktop session), or give wlroots a libinput device (uinput, which needs a privileged
#   container). Neither belongs in this test harness.
set -uo pipefail

APP=/app/poc5-native.kexe
LOG=/out/wl-input-app.log
RESULT=/out/wl-input-result.txt
TAGS=/out/tags.txt
: > "$RESULT"
rm -f "$TAGS"

pass() { echo "PASS: $1" | tee -a "$RESULT"; }
fail() { echo "FAIL: $1" | tee -a "$RESULT"; }
skip() { echo "SKIP: $1" | tee -a "$RESULT"; }

mkdir -p "$XDG_RUNTIME_DIR" && chmod 700 "$XDG_RUNTIME_DIR"

sway --config /dev/null > /out/sway-input.log 2>&1 &
SWAY_PID=$!
for _ in $(seq 1 60); do
  SOCK=$(ls "$XDG_RUNTIME_DIR"/wayland-* 2>/dev/null | grep -v '\.lock$' | head -1)
  [ -n "$SOCK" ] && break
  sleep 0.5
done
[ -n "${SOCK:-}" ] || { fail "compositor never came up"; cat /out/sway-input.log; exit 1; }
export WAYLAND_DISPLAY=$(basename "$SOCK")
# swaymsg needs its IPC socket. Without SWAYSOCK it silently fails ("Unable to retrieve socket path"), and
# an earlier version of this test reported resize as PASS while swaymsg had never run: what actually
# resized the window was sway's own tiling at startup. Export it, and assert on the size we asked for.
export SWAYSOCK=$(ls "$XDG_RUNTIME_DIR"/sway-ipc.*.sock 2>/dev/null | head -1)
[ -n "$SWAYSOCK" ] || { fail "no sway IPC socket"; exit 1; }
pass "wlroots compositor up ($WAYLAND_DISPLAY, IPC ok)"

# No X server at all: nothing to fall back to, so whatever works here is genuinely Wayland.
unset DISPLAY

POC5_RUN_SECONDS=40 timeout 110 "$APP" > "$LOG" 2>&1 &
APP_PID=$!

for _ in $(seq 1 60); do [ -s "$TAGS" ] && break; sleep 0.5; done
[ -s "$TAGS" ] || { fail "the app never exported its semantics tags"; cat "$LOG"; exit 1; }
echo "tags:"; cat "$TAGS"
pass "semantics: tags exported under Wayland ($(wc -l < "$TAGS" | tr -d ' ') tags)"

grep -q "platform = Wayland" "$LOG" && pass "GLFW is on the Wayland backend (no X server present)" \
  || fail "not on Wayland ($(grep -m1 'platform =' "$LOG"))"

# Does the seat have any input capability at all? This decides what can be tested.
CAPS=$(swaymsg -t get_seats 2>/dev/null | grep -m1 '"capabilities"' | grep -oE '[0-9]+' || echo 0)
echo "seat capabilities: $CAPS"

# --- 1. KEYBOARD. Works even with capabilities 0, because wtype attaches a virtual keyboard itself.
# Every wtype invocation loses its FIRST keystroke (it sends its own keymap and the first key is dropped
# while the client reloads it), so a sacrificial character goes inside the same string.
wtype -d 120 ".hello" 2>/dev/null || echo "(wtype failed)"
sleep 2

# --- 2. POINTER: measured as not deliverable on this compositor (see the header). Prove that claim rather
# than asserting it, then skip the pointer-dependent checks instead of faking them.
wlrctl pointer move -9999 -9999 2>/dev/null; wlrctl pointer move 320 140 2>/dev/null
wlrctl pointer click left 2>/dev/null
sleep 1.5

# --- 3. RESIZE: the compositor owns geometry under Wayland; the client cannot resize itself. The window
# must be floating first: while it is tiled, sway's layout dictates the size and `resize set` is ignored
# (that is why an earlier run stayed at the tiled 1276x693 instead of the size we asked for).
swaymsg floating enable >/dev/null 2>&1 || true
sleep 0.5
swaymsg -- resize set width 900 px height 700 px >/dev/null 2>&1 || true
sleep 2

wait "$APP_PID" 2>/dev/null || true
sleep 1
kill "$SWAY_PID" 2>/dev/null || true

echo "---- app log ----"; grep -vE "^wrote /out" "$LOG"; echo "-----------------"

# ---------------- assertions ----------------

if grep -q "typed codepoint=104" "$LOG" && grep -q "typed codepoint=101" "$LOG"; then
  pass "keyboard: real Wayland key events reached Compose (h, e, ...)"
else
  fail "keyboard: no char events reached Compose"
fi

if grep -q "POC5: cursor ->" "$LOG"; then
  # If this ever fires, the compositor grew a pointer: turn the skips below into real checks.
  pass "pointer: events WERE delivered (seat capabilities=$CAPS) -- enable the pointer checks"
else
  skip "pointer: not deliverable on a headless wlroots seat (capabilities=$CAPS, no input device attached)."
  skip "  -> click, scroll, hover and clipboard-via-button are therefore NOT covered under Wayland."
  skip "  -> they ARE covered under X11 (test-input.sh), and the mediator has no X11/Wayland branch:"
  skip "     pointer input is the same GLFW callback code on both platforms."
fi

# The client surface is SMALLER than the window we asked for: sway subtracts its decorations (borders and
# title bar), so `resize set 900x700` reaches the app as ~896x673. Assert on the surface being close to what
# we asked, not equal to it -- the exact inset depends on the compositor's theme.
LAST=$(grep -o "resized to [0-9]*x[0-9]*" "$LOG" | tail -1 | grep -o "[0-9]*x[0-9]*")
LW=${LAST%x*}; LH=${LAST#*x}
if [ -n "$LAST" ] && [ "${LW:-0}" -ge 860 ] && [ "${LW:-0}" -le 900 ] \
                  && [ "${LH:-0}" -ge 640 ] && [ "${LH:-0}" -le 700 ]; then
  pass "resize: compositor resize honoured (asked 900x700, surface $LAST after sway's decorations)"
elif [ -n "$LAST" ]; then
  fail "resize: last surface size was $LAST, nowhere near the 900x700 we asked for"
else
  fail "resize: the app never saw a resize"
fi

echo
echo "==== SUMMARY ===="
cat "$RESULT"
grep -q "^FAIL" "$RESULT" && exit 1
exit 0
