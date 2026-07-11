#!/usr/bin/env bash
# One-command setup for a fresh clone: regenerate every git-ignored build input the POCs need.
# Usage: scripts/setup.sh
#
#   1. scripts/fetch-deps.sh   -> ./.cmc (compose source checkout) + */native/glfw (Linux arm64 libs)
#   2. skip export             -> ./export (Skip's transpiled Gradle project), if the skip CLI is present
#   3. scripts/patch-export.sh -> de-Android-ify ./export so the POC 6 builds go green
#
# After this, all six POCs build/run via scripts/run-native.sh, scripts/run-jvm.sh and
# scripts/run-poc2-screen.sh. Needs the Skip toolchain (https://skip.dev) for step 2; steps 1 and 3 are
# self-contained. `skip export` transpiles BOTH witness screens: POC 6 renders MinimalContentView, POC 1's
# richer ContentView rides along. The patch targets the pinned Skip version (see FINDINGS-POC6).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== 1/3 dependencies (.cmc + native libs) =="
scripts/fetch-deps.sh

echo "== 2/3 Skip transpiled export (./export) =="
if [ -d "export/Witness-project/Witness/SkipUI/src/main/kotlin" ]; then
  echo "  ./export already present, leaving it as is."
elif ! command -v skip >/dev/null 2>&1; then
  echo "  skip CLI not found. Install it, then re-run scripts/setup.sh:"
  if command -v brew >/dev/null 2>&1; then
    echo "      brew tap skiptools/skip"
    echo "      brew install skip"
  else
    echo "      # Homebrew first:"
    echo '      /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
    echo "      # then:"
    echo "      brew tap skiptools/skip"
    echo "      brew install skip"
  fi
  echo "  (.cmc and native libs are ready; only ./export is missing.)"
  echo "== setup done (without ./export) =="
  exit 0
else
  echo "  running 'skip export'..."
  # A stale SwiftPM cache pins the old repo path and breaks the export; a fresh clone has none.
  rm -rf witness-app/.build
  ( cd witness-app && skip export --debug --no-ios -d ../export )
  # skip export drops a Witness-project.zip (+ apk/aab we don't need); unpack the project, drop the rest.
  if [ -f export/Witness-project.zip ]; then
    ( cd export && rm -rf Witness-project && unzip -q Witness-project.zip -d Witness-project )
  fi
  rm -f export/Witness-project.zip export/Witness-debug.apk export/Witness-debug.aab

  echo "== 3/3 de-Android-ify ./export =="
  scripts/patch-export.sh
fi

echo "== setup done. Build a POC, e.g. scripts/run-native.sh poc6-native =="
