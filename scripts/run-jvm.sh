#!/usr/bin/env bash
# Render a JVM Compose Multiplatform Desktop POC offscreen to a PNG (the "with JVM" POCs).
# Usage: scripts/run-jvm.sh <desktop-witness|poc6-skip-cmp>
#   desktop-witness  POC 1  Skip output rendered on CMP Desktop (self-contained; also a click test)
#   poc6-skip-cmp    POC 6  the real transpiled SkipUI on CMP Desktop (needs ./export + an android.jar)
#
# POC 2 (compose-first) is deliberately NOT here: it uses navigation-compose, which cannot render
# offscreen ("LocalLifecycleOwner not present") and needs a real screen. That is a POC 2 finding, not a
# bug; reproduce it on a real display (Xvfb on Linux, or a desktop) with `gradle run`. See FINDINGS-POC2.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
POC="${1:?usage: run-jvm.sh <desktop-witness|poc6-skip-cmp>}"

if [ "$POC" = "compose-first" ]; then
  echo "POC 2 (compose-first) cannot render offscreen: navigation-compose needs a real window"
  echo "(LocalLifecycleOwner not present). Use the real-screen harness instead:"
  echo "    scripts/run-poc2-screen.sh"
  echo "It builds + runs the app on Linux arm64 under Xvfb and screenshots home + detail."
  exit 2
fi
[ -d "$ROOT/$POC" ] || { echo "unknown POC dir: $POC (use desktop-witness or poc6-skip-cmp)"; exit 1; }

# Only poc6-skip-cmp consumes Skip's transpiled output; desktop-witness is self-contained.
if [ "$POC" = "poc6-skip-cmp" ] && [ ! -d "$ROOT/export/Witness-project/Witness/SkipUI/src/main/kotlin" ]; then
  echo "Missing ./export (Skip transpiled output). Run 'skip export' on a SwiftUI witness app first (see README)."
  exit 1
fi

cd "$ROOT/$POC"
echo "[$POC] rendering offscreen via Compose Multiplatform Desktop (JVM)..."
gradle renderPng --console=plain
case "$POC" in
  poc6-skip-cmp) echo "[$POC] done. PNG in $ROOT/$POC/out/"; ls -la out/*.png 2>/dev/null || true ;;
  *)             echo "[$POC] done. PNG(s) in $ROOT/docs/" ;;
esac
