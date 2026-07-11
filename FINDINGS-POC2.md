# FINDINGS: POC 2: Compose-first native + mobile-ARM reality

> English is the canonical version (this file). French working copy:
> [`FINDINGS-POC2.fr.md`](./FINDINGS-POC2.fr.md). POC 1 (Skip→CMP transpiler, closed) lives in
> [`FINDINGS.md`](./FINDINGS.md). Do not overwrite it.

Journal of POC 2: real versions, commands actually run, raw errors, fixes, captures per surface,
runtime-weight numbers, official CMP-target status with sources, time spent, and the per-target
go/no-go decision.

## Executive summary

**Decision: GO Compose-first for the target device's UI, target-aware.** A 100% Kotlin/Compose app (no Skip, no
SwiftUI) renders, stays interactive, **navigates** (navigation-compose, a confirmed CMP artifact) and
**persists** on a **real Linux arm64 screen** (Xvfb) and on macOS, see
[`poc2-linux-home.png`](./docs/poc2-linux-home.png) →
[`…-incremented.png`](./docs/poc2-linux-incremented.png) →
[`…-detail.png`](./docs/poc2-linux-detail.png) →
[`…-persisted.png`](./docs/poc2-linux-persisted.png).

The catch is **runtime weight**, not capability. Desktop CMP is **JVM-based** (JetBrains, official);
there is **no JVM-free desktop/Linux target**. On Linux arm64 the app image is **137 MB**, idle RAM
**~224 MB**, cold start **460 ms**. So, per target:

- **Android / iOS** → GO; CMP runs on native runtimes (ART / Kotlin-Native), the JVM figures don't apply.
- **Appliance / tablet (not tightly constrained)** → GO; the embedded jlink JVM is comfortable.
- **Constrained mobile Linux-ARM running the *desktop* build** → **ALERT**: 137 MB / 224 MB is heavy
  and there is no supported JVM-free path: prefer the native Android target or budget the JVM.

Together with POC 1 (Skip→CMP transpiler: **NO-GO**), the pair settles the target device's UI layer: **don't
transpile Skip, write Compose Multiplatform**, and choose the CMP target by device weight.

## The two questions

- **A**: Does Compose-first hold on a base close to the target device? A floor CMP app, 100% Kotlin/Compose (zero
  Skip, zero SwiftUI), rendering + interactive + persistent, on a minimal Linux image, **on a real
  screen**.
- **B**: The "mobile ARM" reality beyond headless JVM: distributable size, idle RAM, cold start on
  Linux arm64; and is there a CMP target without an embedded JVM?

## Three test surfaces (kept distinct)

1. macOS arm64 real screen, quick interactivity check, **not** the target device (Metal/CoreGraphics).
2. **Linux arm64 real screen**: the judge. Here via **Xvfb** (a real X server / virtual framebuffer)
   in Docker, captured with ImageMagick, driven with xdotool. Distinct from POC 1's offscreen render.
3. Linux arm64 software headless, already done in POC 1, not repeated.

---

## Step 1: CMP Kotlin-pure project (2026-07-11)

### Toolchain (reused from POC 1, verified)

- JDK 21 (Temurin) / Gradle 9.6.1 / Compose Multiplatform plugin `org.jetbrains.compose` 1.9.0,
  Kotlin 2.3.0 + `org.jetbrains.kotlin.plugin.compose`.

### Navigation with a confirmed CMP artifact (not guessed)

POC 1 established `androidx.navigation3` has **no** CMP artifact. Verified on Maven Central that the
JetBrains multiplatform navigation port **does** exist and has a **stable** release:

```
org.jetbrains.androidx.navigation:navigation-compose  → stable 2.9.2 (latest 2.10.0-alpha02)
```

Adopted **2.9.2**. Project (`compose-first/`) compiles: `compose.desktop.currentOs` +
`compose.material3` + `navigation-compose:2.9.2` all resolve. `BUILD SUCCESSFUL`.

## Step 2: Floor app, 100% Kotlin/Compose

`compose-first/src/main/kotlin/app/Main.kt`, a two-screen app, no Skip, no SwiftUI:

- **Home**: `Column { Text("Count: $count"); Row { Button("-"); Button("+") }; if (count>0) Text("Positive"); Button("Details") }`, state via `remember { mutableStateOf(...) }`.
- **Detail**: reached via `NavHost` + `rememberNavController().navigate("detail")`; a `< Back` button (`popBackStack`) and `Text("Detail for ...")`.
- **Persistence**: `CounterStore` on `java.util.prefs.Preferences`, the counter survives process restarts.

## Step 3: macOS real screen (surface 1) ✅

`gradle run` opens a **native window** (JVM/Skia, macOS arm64). Confirmed **interactively by the
operator** (screenshots): Count 0 → `+` → Count 1 with "Positive" → "Details" → detail screen
("Detail for 1", "< Back"). Rendering, interactivity, the conditional view and **navigation
(navigation-compose CMP)** all work. This is the fast sanity check, **not** the target device.

### Finding: offscreen render is not enough here

An `ImageComposeScene` (POC 1's offscreen trick) throws `CompositionLocal LocalLifecycleOwner not
present`: navigation-compose needs a `LifecycleOwner`, which a real `application { Window { … } }`
provides but a bare offscreen scene does not. This is concrete evidence for the POC 2 rule that the
**real screen is required**: offscreen literally cannot host navigation here.

### Time spent

- Steps 1-3: ~40 min.

## Step 4: Linux arm64 real screen (Xvfb), the judge ✅

The app runs on a **real X server** (Xvfb, virtual framebuffer) inside a Linux **arm64** container,
a display server, not POC 1's offscreen scene. Windowed app under Xvfb, framebuffer captured with
ImageMagick, clicks driven with `xdotool`. Reusable harness:
[`compose-first/docker/`](./compose-first/docker/) (`Dockerfile` + `run-xvfb.sh`).

Full chain proven (captures in `docs/`):

- **Render**, [`poc2-linux-home.png`](./docs/poc2-linux-home.png): Count 0, `-`/`+`, Details.
- **Interactivity**, [`poc2-linux-incremented.png`](./docs/poc2-linux-incremented.png): two `+`
  clicks → Count 2 + "Positive".
- **Navigation** (navigation-compose CMP), [`poc2-linux-detail.png`](./docs/poc2-linux-detail.png):
  Details → "< Back" + "Detail for 2".
- **Persistence**, [`poc2-linux-persisted.png`](./docs/poc2-linux-persisted.png): kill + relaunch
  (fresh process) → Count 2 survives (`java.util.prefs`).

Gotchas (mechanical): same `libGL.so.1` need as POC 1 (`libgl1`), plus `xvfb x11-utils xdotool
imagemagick libx11-6 libxext6 libxrender1 libxi6 libxtst6` and DejaVu fonts; `SKIKO_RENDER_API=SOFTWARE`.
No window manager under bare Xvfb, so the window maps at (0,0) with no title bar, click coordinates
are content-relative. **Question A answered: yes, Compose-first renders, stays interactive,
navigates and persists on a real Linux arm64 screen.**

### Time spent

- Step 4: ~40 min (Docker image + Xvfb harness + click calibration).

## Step 5: Runtime weight on Linux arm64 (Question B)

Measured **inside the Linux arm64 container** (`uname -m` = `aarch64`, Temurin 21), on the jpackage
app image produced by Compose's `createDistributable` (jlink'd JRE + app jars). Harness:
[`compose-first/docker/measure.sh`](./compose-first/docker/measure.sh).

| Metric | Value | Note |
|---|---|---|
| Distributable (app image) | **137 MB** | total on disk |
| - bundled JRE (jlink) | **87 MB** | the embedded Java runtime |
| - app jars (`lib/app`) | **49 MB** | skiko + Compose + Kotlin stdlib + navigation |
| Cold start → first window | **460 ms** | launcher to window mapped, under Xvfb |
| Idle RSS | **224 MB** | resident memory of the JVM at rest |

Reading: cold start (460 ms) is fine anywhere. **The weight is the JVM+Skia footprint**: a 137 MB
image and ~224 MB idle RAM are comfortable for an **appliance/tablet**, but heavy for a **constrained
mobile** device. (RSS is untuned: default heap; a real product would cap `-Xmx` and could trim the
jlink image, but the skiko native lib + Compose baseline set a floor.) This is exactly the
appliance-vs-mobile split the brief asked to keep separate.

### Time spent

- Step 5: ~30 min (jpackage image + measurement harness + fixes).

## Step 6: Official CMP target status (Question B)

**Desktop Compose Multiplatform is JVM-based.** From the official JetBrains repo
(github.com/JetBrains/compose-multiplatform):

> "Compose Multiplatform targets **the JVM** and supports high-performance hardware-accelerated UI
> rendering on all major desktop platforms, macOS, Windows, and Linux."

Target-by-target (official support):

- **Desktop** (Windows/macOS/**Linux**): **JVM** + Skia (skiko). No Kotlin/Native desktop target.
- **Android**: Jetpack Compose on the Android runtime (ART), not a separately-embedded JVM.
- **iOS**: **Kotlin/Native** (compiles native, renders via skiko/Metal). No JVM.
- **Web**: Kotlin/**Wasm** (Beta). No JVM, but a browser target, not a native Linux window.

**Is there a CMP target without an embedded JVM, for a native Linux app? No, not officially.** The
only JVM-free Compose targets are iOS (Kotlin/Native) and Web (Wasm); neither is a native Linux/desktop
app. skiko itself has Kotlin/Native targets and the Compose runtime demonstrably runs native on iOS,
so a Kotlin/Native Linux desktop build is *conceivable*, but JetBrains ships **no supported
configuration** for it. So shipping CMP UI on a native Linux ARM appliance today means **embedding a
(jlink-trimmed) JVM**: the 137 MB / ~224 MB measured in step 5. Feasibility statement only; not
implemented here.

### Time spent

- Step 6: ~15 min (sources).

## Step 7: Decision (per target)

### Questions, answered

- **A. Compose-first holds on a base close to the target device: YES.** A 100% Kotlin/Compose floor app renders, stays
  interactive, navigates (navigation-compose CMP), and persists (`java.util.prefs`) on a **real Linux
  arm64 screen** (Xvfb), and on macOS. Zero Skip, zero SwiftUI.
- **B. mobile-ARM reality: measured.** Desktop CMP is JVM-based; on Linux arm64 the app image is
  **137 MB**, idle RSS **224 MB**, cold start **460 ms**. No supported JVM-free desktop/Linux target.

### Decision, split by target (as the brief requires)

- **The target device as an Android / iOS app → GO Compose-first, no desktop-JVM weight.** On these targets CMP
  uses the *native* runtimes (Android ART; iOS Kotlin/Native): the 137 MB / 224 MB above are the
  **desktop-JVM** figures and do **not** apply. This is the cleanest path.
- **The target device as an appliance / tablet (Linux ARM or x86, not tightly constrained) → GO.** The Compose
  Desktop build with an embedded jlink JVM is comfortable there: 137 MB on disk, ~224 MB idle RAM,
  sub-second start.
- **The target device as a *constrained* mobile Linux-ARM running the Compose *Desktop* build → ALERT.** The
  JVM+Skia footprint (137 MB / ~224 MB idle) is heavy for a tight device, and there is **no supported
  JVM-free desktop/Linux CMP target**. On such a device, prefer the **native Android target** (CMP
  without a separate JVM) or budget explicitly for the JVM; do not assume the desktop build is free.

### Recommendation

**Compose-first is validated for the target device's UI**, confirming POC 1's steer. Pick the CMP *target* by
device: native mobile (Android/iOS) where possible; the JVM desktop build only where the device can
carry ~137 MB / ~224 MB. The one thing to not do blindly is run the **JVM desktop** build on a
tightly-constrained mobile: there measure first (step 5's numbers) rather than assume.

### Context note: "Skip is free" (2026, blog) does not change this

Skip 1.7 open-sourced its engine (`skipstone`); still **iOS + Android only, no CMP mention**. Forking
Skip to add CMP is *possible* but does not shortcut POC 1's cost: the gap is the Android-coupled
**runtime** (SkipUI/SkipFoundation, `navigation3` with no CMP artifact), not the now-open transpiler.
That would be a large upstream contribution needing maintainer buy-in: a different goal from shipping
the target device. Reinforces Compose-first.

### Meta / time-box

POC 2 done in **~2h30** (well inside the 1-day box): project + app, macOS real screen, Linux arm64
real screen (Xvfb), runtime weight, official target status. Together, **POC 1 (Skip→CMP: NO-GO) +
POC 2 (Compose-first: GO, target-aware)** give the full picture for the target device's UI layer.

### Time spent

- Step 7: ~20 min.
