# scripts

Helpers to rebuild and re-run the POCs. All paths are relative to the repo root.

## Coverage (POC 1 to 6)

| POC | What | Directory | Command |
|-----|------|-----------|---------|
| 1 | Skip output on CMP Desktop (JVM) | `desktop-witness` | `scripts/run-jvm.sh desktop-witness` |
| 2 | Compose-first sample (JVM) | `compose-first` | `scripts/run-poc2-screen.sh` (real screen) |
| 3 | Skia + runtime + GLFW on K/N Linux | `poc3-native` | `scripts/run-native.sh poc3-native` |
| 4 | Minimal ui-glfw, interactive | `poc4-native` | `scripts/run-native.sh poc4-native` |
| 5 | Real material3 on K/N Linux | `poc5-native` | `scripts/run-native.sh poc5-native` |
| 6 | Transpiled SkipUI, native (no JVM) | `poc6-native` | `scripts/run-native.sh poc6-native` |
| 6 | Transpiled SkipUI on CMP Desktop (JVM) | `poc6-skip-cmp` | `scripts/run-jvm.sh poc6-skip-cmp` |

## Fresh clone

```bash
scripts/setup.sh
```

Regenerates every git-ignored build input in one go, so all six POCs build afterwards:
1. `scripts/fetch-deps.sh` -> `./.cmc` + `*/native/glfw` (below).
2. `skip export` -> `./export` (needs the [Skip](https://skip.dev) CLI). It transpiles both witness
   screens: POC 6 renders `MinimalContentView`, POC 1's richer `ContentView` rides along.
3. `scripts/patch-export.sh` -> de-Android-ify `./export` so the POC 6 builds go green.

The patch (`scripts/export.patch`, applied by `patch-export.sh`) is the broad `external fun Swift_*` stub
transform plus ~20 SkipLib/Foundation/Model/UI edits. It holds only the changed lines (no Skip source
context) and targets the pinned Skip version; a different Skip version may need it regenerated (see
`FINDINGS-POC6.md`).

## One-time setup (native POCs 3 to 6)

```bash
scripts/fetch-deps.sh
```

Fetches the git-ignored build dependencies:
- `./.cmc` : a shallow checkout of `JetBrains/compose-multiplatform-core` (branch `jb-main`), the
  compose source the native POCs compile from.
- `*/native/glfw/` : the Linux/arm64 native libraries + headers (GLFW, GL/EGL, fontconfig, freetype),
  extracted from a Debian arm64 container.

Not fetched (licensing): `./export`, Skip's transpiled Kotlin output (needed by POC 6). Generate it with
`skip export` on a SwiftUI witness app (see the top-level README, "Reproduce (POC 6)").

## Native POCs (Kotlin/Native Linux, no JVM): POC 3 to 6

```bash
scripts/run-native.sh poc6-native            # release binary, then render in Docker/Xvfb
scripts/run-native.sh poc6-native debug      # faster link, larger binary
scripts/run-native.sh poc5-native            # the material3 sample
scripts/run-native.sh poc3-native            # skia + GLFW window
```

Builds the `linuxArm64` binary and renders it in a Linux arm64 container under Xvfb (software GL), from a
single shared runner image (`scripts/docker/Dockerfile.run`). PNG captures land in `<poc>/out/`. The
produced binary has no JVM. Override the compose checkout with `CMC_ROOT=/abs/path scripts/run-native.sh ...`
or the build's `-PcmcRoot=`.

## Is the window toolkit baked in? (POC 5, Jalon 13)

```bash
scripts/test-embedders.sh                    # GTK4 and Qt6 embedders, same compose klib
```

Builds two more embedders that drive the very same compose klib and the same 42 Linux actuals as the GLFW
build, and neither links `-lglfw`. It then checks, with `readelf` rather than by assertion in prose, that the
GTK and Qt binaries need **zero** GLFW symbols from the system, that the GLFW build **does** need them (the
control: without it the check would prove nothing), and that each app renders material3 and a click on the
`Button` increments the counter.

This is the answer to Jake Wharton's objection that `expect/actual` "assumes there is only a single,
canonical UI toolkit for each build target [...] If you actualize to GTK then it would be impossible to use
for Qt." Needs `fetch-deps.sh` first: it stages GTK4 and compiles the Qt C++ shim (Qt is C++, and cinterop
binds C only).

## JVM POCs (CMP Desktop, offscreen render): POC 1 and POC 6

```bash
scripts/run-jvm.sh desktop-witness           # POC 1  -> docs/desktop-witness*.png (incl. a click test)
scripts/run-jvm.sh poc6-skip-cmp             # POC 6  -> poc6-skip-cmp/out/poc6-skipui-render.png
```

Renders offscreen to a PNG via `gradle renderPng`. `poc6-skip-cmp` needs `./export` and a local
`android.jar` (compile-time only); the build finds `android.jar` via `ANDROID_JAR`, then `ANDROID_HOME` /
`ANDROID_SDK_ROOT`, then the usual per-OS SDK locations (macOS `~/Library/Android/sdk`, Linux
`~/Android/Sdk`, Windows `%LOCALAPPDATA%\Android\Sdk`), picking the highest installed platform.

## POC 2 (compose-first): real screen

POC 2 uses `navigation-compose`, which cannot render offscreen (`LocalLifecycleOwner not present`) and
needs a real window. That is a POC 2 finding, not a bug, so it has its own harness:

```bash
scripts/run-poc2-screen.sh
```

It wraps the harness in `compose-first/docker/`: builds and runs the JVM app on Linux arm64 under Xvfb
(software GL) inside a JDK+gradle container, drives real clicks through navigation-compose, and screenshots
home, increment (`Count: 2` + `Positive`), detail, and persistence across a restart to
`compose-first/docker/out/` (`poc2-linux-*.png`). Heavier than the other runners (the app is built inside
the container). See `FINDINGS-POC2.md`.
