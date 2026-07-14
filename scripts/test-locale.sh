#!/usr/bin/env bash
# Lot 2 acceptance test: the app must follow the POSIX locale environment.
#
# Runs the app three times under Xvfb with different LANG values and checks what it reports. The RTL case
# is the one that matters: with an Arabic locale the scene's LayoutDirection must flip, which mirrors the
# whole UI. Locale used to be hardcoded to en-US and isRtl() always returned false, so none of this moved.
set -uo pipefail

APP=/app/poc5-native.kexe
RESULT=/out/locale-result.txt
: > "$RESULT"

pass() { echo "PASS: $1" | tee -a "$RESULT"; }
fail() { echo "FAIL: $1" | tee -a "$RESULT"; }

# Run the app briefly with a given LANG and return its startup log.
run_with_lang() {
  local lang="$1" out="$2"
  LANG="$lang" LC_ALL="$lang" POC5_RUN_SECONDS=4 timeout 30 "$APP" > "$out" 2>&1 || true
  cp /out/poc5-final.png "/out/locale-${3}.png" 2>/dev/null || true
}

echo "--- en_US.UTF-8"
run_with_lang "en_US.UTF-8" /out/lang-en.log en
grep -m1 "POC5: locale" /out/lang-en.log || true
if grep -q "locale = en-US, layout direction = Ltr" /out/lang-en.log; then
  pass "en_US: locale read from environment, Ltr"
else
  fail "en_US: unexpected ($(grep -m1 'POC5: locale' /out/lang-en.log))"
fi

echo "--- fr_FR.UTF-8"
run_with_lang "fr_FR.UTF-8" /out/lang-fr.log fr
grep -m1 "POC5: locale" /out/lang-fr.log || true
if grep -q "locale = fr-FR, layout direction = Ltr" /out/lang-fr.log; then
  pass "fr_FR: locale follows LANG (not hardcoded en-US), Ltr"
else
  fail "fr_FR: unexpected ($(grep -m1 'POC5: locale' /out/lang-fr.log))"
fi

echo "--- ar_EG.UTF-8 (right-to-left)"
run_with_lang "ar_EG.UTF-8" /out/lang-ar.log ar
grep -m1 "POC5: locale" /out/lang-ar.log || true
if grep -q "locale = ar-EG, layout direction = Rtl" /out/lang-ar.log; then
  pass "ar_EG: RTL locale flips the scene to Rtl (the UI mirrors)"
else
  fail "ar_EG: layout direction did not flip ($(grep -m1 'POC5: locale' /out/lang-ar.log))"
fi

echo
echo "==== SUMMARY ===="
cat "$RESULT"
grep -q "^FAIL" "$RESULT" && exit 1
exit 0
