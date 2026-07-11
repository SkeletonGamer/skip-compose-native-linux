#!/usr/bin/env bash
# De-Android-ify a fresh `skip export` output so the POC 6 builds (JVM + Kotlin/Native Linux) go green.
# Applies scripts/export.patch to ./export/Witness-project/Witness. Run after `skip export` (or via
# scripts/setup.sh, which chains fetch-deps -> skip export -> this).
#
# The patch holds only the changed lines (no Skip source context): the broad `external fun Swift_*` ->
# stub transform plus the ~20 SkipLib/Foundation/Model/UI edits that swap the Android surface for the
# shims. It targets the exact transpiler output of the pinned Skip version; a different Skip version may
# need it regenerated (see FINDINGS-POC6). The witness screens are transpiled as-is: POC 6 renders
# `MinimalContentView`; POC 1's richer `ContentView` rides along untouched.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$ROOT/export/Witness-project/Witness"
PATCH="$ROOT/scripts/export.patch"

[ -d "$MOD/SkipUI/src/main/kotlin" ] || {
  echo "No ./export found. Run 'skip export' (or scripts/setup.sh) first."; exit 1;
}
[ -f "$PATCH" ] || { echo "Missing $PATCH"; exit 1; }

# Already patched? The stub transform removes every `external fun Swift_`.
if ! grep -rq "external fun Swift_" "$MOD" 2>/dev/null; then
  echo "[patch-export] ./export already looks patched (no 'external fun Swift_'); nothing to do."
  exit 0
fi

echo "[patch-export] applying de-Android-ification patch to ./export ..."
# ./export is git-ignored, so a plain `git apply` run inside this repo would skip every file. Cap the
# repo search at ./export so git apply treats the tree as standalone and patches it purely textually.
( cd "$MOD" && GIT_CEILING_DIRECTORIES="$ROOT/export" git apply -p1 --unidiff-zero "$PATCH" )
echo "[patch-export] done. ./export now builds for the POCs (see scripts/run-jvm.sh / run-native.sh)."
