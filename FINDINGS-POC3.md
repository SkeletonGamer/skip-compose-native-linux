# FINDINGS. POC 3: Compose Multiplatform on Kotlin/Native Linux desktop (no JVM)

> English canonical (this file). French copy: [`FINDINGS-POC3.fr.md`](./FINDINGS-POC3.fr.md).
> POC 1 (`FINDINGS.md`) and POC 2 (`FINDINGS-POC2.md`) are closed: do not overwrite them.

R&D probe, not an afternoon one: building a **native Linux desktop backend** for Compose to drop the
JVM (remove POC 2's 87 MB JRE + JVM overhead behind the 137 MB / 224 MB figures). Milestone-based;
each milestone can kill/redefine the project cheaply. **Only Jalon 1 is time-boxed (~3 days).**

## Executive summary (interim: session 1, Jalons 0-4 probed)

**The foundation exists and works; the framing premise was outdated.** The plan feared the low-level
wall was "skiko doesn't build for K/N Linux." It does. In one session, on Linux arm64, with **no JVM**:

- **Jalon 0**: `skiko` ships an official K/N `linuxArm64` klib (0.150.0), added by the merged PR
  skiko#1051. The framing premise ("skiko has no K/N Linux") is outdated for arm64. Note: SKIKO-611
  itself (K/N Windows/Linux x86_64) is still open and community-driven; the arm64 target is separate.
- **Jalon 1** ✅ (killer signal): a **Skia rectangle renders in Kotlin/Native Linux arm64** (offscreen
  raster, [`docs/poc3-skia-knative-arm64.png`](./docs/poc3-skia-knative-arm64.png)), cross-compiled
  from the macOS host (K/N has **no linux-aarch64 host** compiler).
- **Jalon 2** ✅: `compose.runtime` runs in K/N Linux (snapshot state), but `compose.ui`,
  `foundation`, `material3` are **not published** for K/N Linux. **That is the whole gap.**
- **Jalon 3**: the **windowing brick works**: skiko renders via GL into a real **GLFW window** in K/N
  Linux (300 frames, no JVM). Remaining chasm = wiring `ComposeScene` to it (events/fonts/density),
  the unpublished `compose.ui` Linux backend = the multi-week work.
- **Jalon 4**: **21.5 MB** self-contained binary vs **137 MB** JVM image (no 87 MB JRE); RAM only
  modestly lower (Skia dominates).

**Verdict so far: GO to continue.** The killer signal passed decisively: the rendering + runtime +
windowing bricks all exist natively for K/N Linux today. What's left is not research into *whether* it
can work, but engineering the `compose.ui` Linux backend (`ComposeScene`↔GLFW: input, fonts, hidpi).
That is bounded and known, but it is weeks, not an afternoon. Detail per jalon below.

## Goal

A Compose UI rendering on Linux via **Kotlin/Native** (no JVM), replicating what the iOS path already
does natively (Skia + native windowing) but for Linux, the layer JetBrains has not shipped.

## Starting reality (state of the art, to verify in Jalon 0)

- skiko's Kotlin/Native targets are iOS + macOS; Linux/Windows are JVM-only. SKIKO-611 tracks K/N
  Windows/Linux support: open, undelivered.
- The hard part is windowing/events/fonts (ex-AWT, JVM-only), not Compose↔skiko (already native on
  iOS/macOS). skiko can be fed a GL context created by GLFW/SDL/Winit.

---

## Jalon 0: Reconnaissance ✅ (a key surprise)

**skiko already ships a Kotlin/Native Linux target.** The premise carried into this POC ("skiko K/N
doesn't support Linux; SKIKO-611 open") is **outdated**. Verified on Maven Central:

```
org.jetbrains.skiko:skiko-linuxarm64:0.150.0  → skiko-linuxarm64-0.150.0.klib
org.jetbrains.skiko:skiko-linuxx64:0.150.0    → (exists too)
```

The `.module` (Gradle metadata) confirms it is a **Kotlin/Native klib**, not a JVM jar:

```
"org.gradle.usage": "kotlin-api" / "kotlin-runtime"
"org.jetbrains.kotlin.native.target": "linux_arm64"
```

So the "brick that doesn't exist" (a K/N Linux Skia renderer) **exists as an official artifact**
(skiko 0.150.0; alongside `iosarm64`, `macosarm64`, etc.). `mingwx64` is still absent (Windows K/N
not published), but **Linux arm64 and x64 K/N are**. This collapses the low-level-wall risk that
framed the milestone plan: Jalon 1 can be attempted against an official klib rather than by building
skiko from source.

**Caveat (to prove in Jalon 1, not assume):** the klib existing ≠ it builds/links/renders. What it
exposes (rendering only, or windowing too?), whether it needs a GL context vs software raster, and the
native lib/`libGL` dependencies, are exactly what Jalon 1 must exercise empirically.

### Time spent

- Jalon 0: ~15 min.

## Jalon 1: Killer signal ✅. Skia renders in Kotlin/Native Linux arm64, no JVM

**Kill condition ("skiko doesn't build in K/N Linux") is disproven: it builds, links, and renders.**

### Host-arch finding (a real constraint, worked around)

The Kotlin/Native **compiler has no `linux-aarch64` host** build. Published hosts:
`linux-x86_64`, `macos-aarch64`, `macos-x86_64`, `windows-x86_64` (checked on Maven Central:
`kotlin-native-prebuilt-2.3.0-linux-aarch64.tar.gz` → HTTP 404). So the compiler cannot run inside the
Linux arm64 container. Worked around by the actual machine: **compile on the macOS arm64 host,
cross-compiling to the `linuxArm64` target**, then **run the ELF in the Linux arm64 container**.

### Build + run

- `poc3-native/`, a `kotlin("multiplatform")` project, single `linuxArm64` target, executable,
  depending on `org.jetbrains.skiko:skiko:0.150.0` (resolves the linuxArm64 K/N klib).
- Kotlin `2.1.21` failed to resolve the K/N toolchain under Gradle 9.6.1; **Kotlin 2.3.0** works.
- `gradle linkDebugExecutableLinuxArm64` on macOS → `BUILD SUCCESSFUL`. Output:
  `poc3-native.kexe` = **ELF 64-bit ARM aarch64**, dynamically linked, `/lib/ld-linux-aarch64.so.1`.
  **No JVM**: this is a native binary; the JVM was build-time only.
- Ran the ELF in the Linux arm64 container → wrote a valid PNG of a purple Skia rectangle
  ([`docs/poc3-skia-knative-arm64.png`](./docs/poc3-skia-knative-arm64.png)), exit 0. Offscreen raster
  (`Surface.makeRasterN32Premul` → `canvas.drawRect` → `encodeToData(PNG)`), all `org.jetbrains.skia.*`.

### Runtime deps + size (Jalon 4 preview)

- **Dynamic deps: glibc only** (`libc/libm/libpthread/libdl/libgcc_s/libcrypt/libresolv/librt`).
  **No `libGL`, no fontconfig, no freetype** for the raster path: Skia is statically linked. Much
  leaner than the JVM path, which needed `libGL.so.1` even in software (POC 1/2).
- **Binary size: 21.5 MB release** (self-contained, Skia included), 25.5 MB debug, **vs 137 MB** for
  the JVM app image (POC 2), and **no separate 87 MB JRE**. First hard evidence that dropping the JVM
  collapses the footprint.

### Scope honesty

This proves the **rendering brick + toolchain** (Skia in K/N Linux arm64, no JVM): the low-level wall
the plan feared. It is **offscreen raster**, not yet **on screen**: opening a window (GLFW + a GL
context + events) is the windowing layer = Jalon 3, the real chasm. But the foundation is green: build
nothing-above was the rule, and the something-below now exists.

### Time spent

- Jalon 1: ~50 min (incl. host-arch dead-end + K/N toolchain download + cross-compile).

## Jalon 2: Compose runtime in K/N ✅ (nearly free, as predicted). And the gap is now pinpointed

### Maven recon: what Compose publishes for K/N Linux

| Artifact (linuxArm64 K/N klib) | Published? |
|---|---|
| `org.jetbrains.skiko:skiko` (Skia rendering) | ✅ (Jalon 1) |
| `org.jetbrains.compose.runtime:runtime` | ✅ |
| `org.jetbrains.compose.ui:ui` | ❌ 404 |
| `org.jetbrains.compose.foundation:foundation` | ❌ 404 |
| `org.jetbrains.compose.material3:material3` | ❌ 404 |

(For contrast, `runtime`, `ui`, `foundation`, `material3` all publish `iosarm64`/`macosarm64` klibs.)
So **rendering (skiko) and the composition engine (compose.runtime) exist for K/N Linux; the widget +
windowing layer (ui/foundation/material3) does not.** That is exactly the "windowing/events/fonts,
not Compose↔skiko" boundary the research pointed at, now confirmed as a hard packaging line.

### Empirical confirmation

Added `org.jetbrains.compose.runtime:runtime:1.9.0` to the linuxArm64 target and exercised snapshot
state (the reactive core), no JVM, same binary as the Skia rectangle:

```
compose.runtime in K/N Linux: count=21 derived(doubled)=42
```

`mutableStateOf` + `derivedStateOf` + `Snapshot.withMutableSnapshot` compile and run natively.
`BUILD SUCCESSFUL`, ran in the Linux arm64 container. Jalon 2 is green with a one-line dependency:
"nearly free," as forecast.

### What this means for the milestones

The POC 3 gap is now precise: **not** the toolchain (Jalon 1), **not** the runtime (Jalon 2), but
**`compose.ui` for K/N Linux**, which JetBrains does not publish: the desktop-Linux native windowing
/ event / font / skiko-GL integration backend. That is **Jalon 3**, and it is the whole remaining cost.

### Time spent

- Jalon 2: ~15 min.

## Jalon 3: The windowing brick works; wiring Compose is the remaining chasm

**Signal: skiko renders via OpenGL into a real GLFW window in K/N Linux arm64, no JVM.**

Built a `linuxArm64` binary that opens a **GLFW** window (C interop), makes a GL context current,
creates a skiko **GL** surface (`DirectContext.makeGL()` + `BackendRenderTarget.makeGL` +
`Surface.makeFromBackendRenderTarget`), and runs a render loop drawing a Skia rectangle. Under Xvfb in
the Linux arm64 container it printed:

```
renderInWindow: drew 300 frames of a skiko rectangle in a real GLFW window (K/N Linux, no JVM)
```

### Cross-link plumbing that was actually needed (the friction is real)

- **GLFW cinterop**: `-DGLFW_INCLUDE_NONE` in the `.def` (GLFW's header pulls `GL/gl.h`, absent; skiko
  brings its own GL).
- **Native libs for a Linux target cross-compiled from macOS**: extracted arm64 `libglfw.so.3`,
  `libGL.so.1`, `libEGL.so.1` (+ GLFW headers) from the container into the project; linked
  `-lglfw -lGL -lEGL`.
- **`--allow-shlib-undefined`**: the Ubuntu arm64 libs reference newer glibc symbol versions
  (`@GLIBC_2.34/2.27`) than K/N's bundled linux sysroot; without it the link fails on
  `pthread_setspecific@GLIBC_2.34`, `powf@GLIBC_2.27`, etc. Deferred to runtime.
- **Runtime**: `XDG_RUNTIME_DIR` must be set or GLFW/EGL aborts; `LIBGL_ALWAYS_SOFTWARE=1` +
  `GALLIUM_DRIVER=llvmpipe` for software GL (no GPU in the container). Runtime libs: `libglfw3`,
  `libgl1`, `libegl1`, `libglx-mesa0`, `libgl1-mesa-dri`, `libx11-6`.

### What is proven vs not

- **Proven**: the *windowing brick* (GLFW window + skiko GL context + render loop) runs natively in
  K/N Linux arm64 with no JVM. The rendering foundation (Jalon 1) and windowing foundation both exist.
- **Visual of the windowed GL render**: captured by **reading the GL window surface back** into a
  raster `Bitmap` (`surface.readPixels` → `Image.makeFromBitmap` → PNG), the purple rectangle skiko
  drew via OpenGL into the GLFW window's framebuffer, in K/N Linux arm64, no JVM:
  [`docs/poc3-window-gl-arm64.png`](./docs/poc3-window-gl-arm64.png). (A first attempt at
  `makeImageSnapshot().encodeToData()` returned null: a GPU-backed image must be made raster first;
  `readPixels` into a Bitmap is the working path. `import -window root` under headless Xvfb stays empty
  because there is no compositor presenting the EGL surface to the X root, but that is an X-presentation
  artifact, not a rendering one.)
- **The real remaining chasm**: `compose.ui` for K/N Linux is **not published** (Jalon 2). Turning
  this brick into a Compose app means wiring `ComposeScene` to the GLFW window: event dispatch
  (mouse/keyboard/resize/scroll), font loading (fontconfig), density/hidpi, the AWT-equivalent layer
  JetBrains only ships for JVM (desktop) and Kotlin/Native (iOS/macOS), not Linux K/N. **That is the
  multi-week work**, and it is where "the weeks live," exactly as forecast.

### Scoping the remaining work (Jalon 3-proper)

Confirmed by search (JetBrains/kotlinlang, DeepWiki): Compose desktop is **JVM-based**, where Skiko
provides "rendering, event handling **and window management**" via the AWT-hosted path; there is **no
official Kotlin/Native Linux desktop Compose**. `compose.ui` K/N backends exist for **iOS/macOS**
(native windowing) but **not Linux** (Jalon 2's 404s). So the remaining work is **net-new for Linux**:
write a Linux-K/N `compose.ui` backend wiring `ComposeScene` to a window (GLFW/Winit): input, fonts
(fontconfig), density/hidpi. The *pattern* exists (the iOS/macOS K/N backends), which de-risks it, but
it is new code, not a config flag: the multi-week estimate stands.

**Source-level confirmation** (`JetBrains/compose-multiplatform-core`, `jb-main`, `compose/ui/ui/build.gradle`):
the ui module's source sets are `commonMain / skikoMain / nativeMain / iosMain / desktopMain / …`. The
**`nativeMain` source set depends on `:compose:ui:ui-uikit`** and the native config is
`configureDarwinFlags()`, i.e. **compose.ui's Kotlin/Native support is Darwin/UIKit (iOS) only; there
is no `linuxMain` and no Linux native target.** The good news: the **`skikoMain`** source set (shared
skiko-backed rendering) would be reused; the net-new piece is a **Linux platform backend**
(the equivalent of `ui-uikit`, e.g. `ui-glfw`) plus adding the `linuxArm64`/`linuxX64` targets to the
ui module. That is the concrete shape of the multi-week job: bounded, and now precisely located.

### Time spent

- Jalon 3 (windowing brick): ~45 min.

## Jalon 4: Weight. K/N native vs JVM (137 MB / 224 MB)

Measured on Linux arm64:

| Metric | K/N native (POC 3) | JVM Compose Desktop (POC 2) |
|---|---|---|
| Distributable / binary | **21.5 MB** (release, self-contained, Skia included) | 137 MB app image (87 MB JRE + 49 MB jars) |
| Separate JRE | **none** | 87 MB |
| Runtime deps (raster path) | **glibc only** | needs `libGL.so.1` even in software |
| RSS | **177 MB** peak while rendering (incl. Mesa llvmpipe software GL, debug binary) | 224 MB idle |

**Reading, honest:** the clear win is **footprint**: a **21.5 MB** self-contained binary vs a 137 MB
image, and **no 87 MB embedded JRE**. **RAM is not a big win**: Skia + GL framebuffers dominate, so
peak-rendering RSS (177 MB, software GL) is only modestly below the JVM's idle 224 MB, and it is not
apples-to-apples (peak-rendering-with-llvmpipe vs JVM-idle; debug binary). On real GPU hardware and a
release binary the RAM picture would differ. **Bottom line: dropping the JVM slashes disk/distribution
and removes JVM warmup, but does not slash RAM: Skia is the floor.**

### Time spent

- Jalon 4: ~15 min.
