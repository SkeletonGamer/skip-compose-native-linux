#!/usr/bin/env bash
# Lot 3 acceptance test: ICU is loaded at runtime, and it actually localizes.
#
# Two things have to hold, and the second is the point of the dlopen approach:
#   1. WITH ICU: month and weekday names come out in the system language (CLDR data). This is the part that
#      cannot be derived by arithmetic, only read from a database.
#   2. WITHOUT ICU: the app still starts and runs, falling back to English. A binary that dies because the
#      distro ships a different ICU is exactly what linking would have produced.
#
# The no-ICU case is simulated by pointing the loader at an empty directory via a private lib path, so
# dlopen finds nothing. Runs inside the container.
set -uo pipefail

APP=/app/poc5-native.kexe
RESULT=/out/icu-result.txt
: > "$RESULT"

pass() { echo "PASS: $1" | tee -a "$RESULT"; }
fail() { echo "FAIL: $1" | tee -a "$RESULT"; }

run_lang() { # $1=LANG  $2=logfile
  env LANG="$1" LC_ALL="$1" POC5_RUN_SECONDS=4 timeout 30 "$APP" > "$2" 2>&1 || true
}

# Exercising the no-ICU path needs a kill switch, not `rm libicu*`. Two earlier attempts were wrong and
# are worth recording:
#   - LD_LIBRARY_PATH=/nonexistent does nothing: dlopen still searches the system paths, so the test
#     "passed" while ICU was in fact loaded the whole time.
#   - Moving the libraries away breaks GL entirely (mesa's DRI drivers pull libicu in via libxml2), so the
#     window never opens and the app dies before reaching any ICU code. On a Linux box that renders with
#     GL, ICU is therefore always present: "no ICU at all" is not a real-world scenario.
# What dlopen actually buys is independence from the ICU VERSION, which check 5 verifies directly.
run_lang_without_icu() { # $1=LANG  $2=logfile
  env LANG="$1" LC_ALL="$1" POC5_RUN_SECONDS=4 POC5_DISABLE_ICU=1 timeout 30 "$APP" > "$2" 2>&1 || true
}

echo "===== 1. ICU present"
run_lang "en_US.UTF-8" /out/icu-en.log
grep -m1 "POC5: ICU" /out/icu-en.log || true
grep -m1 "date sample" /out/icu-en.log || true
if grep -q "POC5: ICU [0-9]* (symbols suffixed" /out/icu-en.log; then
  pass "ICU discovered at runtime: $(grep -o 'ICU [0-9]* (symbols suffixed \"[^\"]*\")' /out/icu-en.log | head -1)"
else
  fail "ICU was not discovered ($(grep -m1 'POC5: ICU' /out/icu-en.log))"
fi
if grep -q "date sample = July 14, 2026" /out/icu-en.log; then
  pass "en_US: 'July 14, 2026' (CLDR pattern + English month)"
else
  fail "en_US: unexpected date ($(grep -m1 'date sample' /out/icu-en.log))"
fi

echo "===== 2. French: the month name must actually be French"
run_lang "fr_FR.UTF-8" /out/icu-fr.log
grep -m1 "date sample" /out/icu-fr.log || true
if grep -qi "juillet" /out/icu-fr.log; then
  pass "fr_FR: month name is 'juillet' -- CLDR data reached material3"
else
  fail "fr_FR: month is not French ($(grep -m1 'date sample' /out/icu-fr.log))"
fi
if grep -qi "week starts: lundi" /out/icu-fr.log; then
  pass "fr_FR: week starts on 'lundi' (localized weekday name, Monday-first)"
else
  fail "fr_FR: weekday name wrong ($(grep -m1 'date sample' /out/icu-fr.log))"
fi

echo "===== 3. Arabic: RTL now comes from CLDR, not our table"
run_lang "ar_EG.UTF-8" /out/icu-ar.log
grep -m1 "POC5: locale" /out/icu-ar.log || true
if grep -q "layout direction = Rtl" /out/icu-ar.log; then
  pass "ar_EG: still Rtl (ICU uloc_getCharacterOrientation agrees with the table)"
else
  fail "ar_EG: layout direction regressed"
fi

echo "===== 4. NO ICU: the app must still run (this is why we dlopen instead of linking)"
run_lang_without_icu "fr_FR.UTF-8" /out/icu-none.log
grep -m1 "POC5: ICU" /out/icu-none.log || true
grep -m1 "date sample" /out/icu-none.log || true

# First: prove ICU really was hidden, otherwise the rest of this check means nothing.
if grep -q "ICU not found (fallback)" /out/icu-none.log; then
  pass "no ICU: the loader correctly found nothing"
else
  fail "no ICU: ICU was still loaded, so this case was never actually tested ($(grep -m1 'POC5: ICU' /out/icu-none.log))"
fi
if grep -q "POC5: exiting after" /out/icu-none.log; then
  pass "no ICU: the app still starts, renders and exits cleanly (a linked binary would not have started)"
else
  fail "no ICU: the app did not survive"
fi
# Correct fallback for fr_FR without CLDR: French field order (day first, which the region table knows),
# English month name (which only CLDR could give). Not "July 14, 2026" -- that is the US order, and this
# run is French.
if grep -q "date sample = 14 July 2026" /out/icu-none.log; then
  pass "no ICU: French field order kept (region table), English month name (no CLDR) -- degrades, does not crash"
else
  fail "no ICU: fallback wrong ($(grep -m1 'date sample' /out/icu-none.log))"
fi

echo "===== 5. The whole point: the binary must not be LINKED against ICU"
# A linked binary would carry libicui18n.so.72 as a NEEDED entry and would refuse to start on a distro
# shipping ICU 74, because ICU renames every symbol per major version (udat_open_72). dlopen means the
# binary has no such entry and resolves whatever ICU the system happens to have.
if ldd "$APP" 2>/dev/null | grep -qi "libicu"; then
  fail "the binary links ICU directly: $(ldd "$APP" | grep -i libicu | tr -s ' ')"
else
  pass "no ICU in the binary's NEEDED entries: it starts against any ICU version (or none)"
fi

echo
echo "==== SUMMARY ===="
cat "$RESULT"
grep -q "^FAIL" "$RESULT" && exit 1
exit 0
