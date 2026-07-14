# FINDINGS: POC 5: build the REAL `compose.ui` for Kotlin/Native Linux (Path A, GLFW)

> English canonical. French: [`FINDINGS-POC5.fr.md`](./FINDINGS-POC5.fr.md). POC 1-4 are closed. POC 4
> proved a *minimal* hand-written `ui-glfw` (no material3). POC 5 is Path A: compile JetBrains'
> **real** `compose.ui`/`foundation`/`material3` for Linux K/N + wire a real `ui-glfw` backend, so
> actual Compose apps (material3 widgets) run on K/N Linux, no JVM.

**This is a multi-session build effort.** Milestone-based; this session takes the first bounded step.

## Jalon 0: Scoping ✅

- **The whole compose UI stack is unpublished for Linux K/N** (all 404): `ui-unit`, `ui-geometry`,
  `ui-graphics`, `ui-text`, `ui-util`, `ui`, `ui-backhandler`, `foundation-layout`, `foundation`,
  `material`, `material3`. So the full stack must be built from source.
- Source lives in **`JetBrains/compose-multiplatform-core`** (`jb-main`), the **AndroidX monorepo
  fork** (`build-fork.gradle`). Its native targets are enabled only for ios/macos; adding Linux across
  its build convention + running the androidx build is heavy (JDK-pinned, prebuilts, GBs).
- **Structurally clean** (POC 4 Jalon 0): `compose.ui`'s `nativeMain` is platform-agnostic
  (`ui-uikit` isolated to `iosMain`); the shared-native deps (runtime, lifecycle, coroutines, compose
  annotation/collection internals) all have Linux K/N klibs.

**Two build routes:** (A1) fork the monorepo and add Linux targets to its build (heavy, upstream-able);
(A2) **extract the module sources and compile them in a plain KMP `linuxArm64` project** against the
published deps (the POC-1 method; avoids the androidx build system). This session tests **A2** on the
smallest leaves.

## Jalon 1: Leaf modules of the real compose.ui compile for Linux K/N ✅

Sparse-cloned `compose/ui/{ui-util,ui-geometry,ui-unit}` from `compose-multiplatform-core` and compiled
them for `linuxArm64` in a plain KMP project (`poc5-native/`), against **published** deps
(`compose.runtime`, `compose.collection-internal`, `compose.annotation-internal`).

- First attempt (all source sets flattened into one): ~40 errors, all from breaking KMP's
  expect/actual (expect+actual in the same source set) and `@OptionalExpectation`.
- Fix: **preserve the KMP hierarchy**, `commonMain` gets the modules' `commonMain` (the `expect`s),
  `linuxArm64Main` gets `nonJvmMain`/`nonAndroidMain` (the `actual`s). → down to **2 errors**.
- Those 2: `ui-util`'s `trace`/`traceValue` expects had no Native actual (Android systrace has no Linux
  equivalent). Provided a **3-line no-op actual**. → **`BUILD SUCCESSFUL`.**

**So JetBrains' real compose.ui foundational modules port to Linux K/N essentially unchanged**: the
only platform-specific code needed for these leaves was a trivial trace no-op. This validates route A2:
the compose.ui source is portable; the work is (a) preserving source-set structure, (b) providing the
Native platform `actual`s. The bulk of (b) for the upper modules IS the `ui-glfw` backend
(ComposeScene ↔ window/input/fonts).

## Jalon 2: Scaling up toward ui:ui / ComposeScene 🟡 (real progress + friction mapped)

Scaled route A2 up the dependency chain. **`ui-graphics`, the skiko-backed graphics layer (103 files:
Canvas, Paint, Path, ImageBitmap, GraphicsLayer…), compiles for Linux K/N** on top of the leaves.
Now `ui-util + ui-geometry + ui-unit + ui-graphics` all build (`BUILD SUCCESSFUL`, `poc5-native/`).

What it took (all mechanical, no fundamental blockers):

- **A proper KMP source-set hierarchy**, not a flat one: `commonMain` (expects) → intermediate
  `skikoMain` (shared non-JVM/skiko) → `linuxArm64Main` (native actuals). Flattening breaks
  expect/actual ("declared in the same module").
- Mapping the module source sets onto it: `commonMain`→common; `nonJvmMain`/`skikoMain`→skikoMain;
  `nativeMain`/**`skikoExcludingWebMain`** (skiko desktop+native, not web)→native. `darwinMain` (Apple)
  is excluded.
- Added the `org.jetbrains.compose.runtime:runtime-saveable` dep (the `Saver` API).

**Two friction types surfaced at `ui-text`, both now diagnosed:**

1. **Source-set hierarchy (fixed).** The real jb-main hierarchy is
   `commonMain → skikoMain → nonJvmMain → nativeMain`, I initially had `skikoMain`/`nonJvmMain`
   **inverted** (so `nonJvmMain`'s `CharHelpers` couldn't see `CodePoint`/`StrongDirectionType` defined
   in `skikoMain`). Reading `ui-text/build.gradle`'s `dependsOn` graph and matching it dropped `ui-text`
   from 699 → 11 → **4 errors**.
2. **Skiko version alignment (the last 4 errors).** With skiko pinned to **0.150.1** (jb-main's catalog
   value) and the hierarchy correct, `ui-text` compiles *except* `NativeFont.native.kt`: it calls
   `org.jetbrains.skia.FontStyle(weight = FontWeight(..), width = FontWidth.NORMAL, slant = ..)`, but
   public skiko 0.150.1's `FontStyle` constructor is `(Int, Int, FontSlant)`. So **jb-main HEAD targets
   a skiko newer than the published 0.150.1** (a dev build with the `FontWeight/FontWidth` overload).
   A version-alignment detail in **one font file**, not a portability blocker.

So the whole **graphics + text** stack is ~4 skiko-API lines from compiling for Linux K/N. `ui:ui`
(where `ComposeScene` lives, ~349 files) is gated on a fully-green `ui-text`. Reaching it, and
enumerating its platform `expect`s (the exact `ui-glfw` backend surface), is the next step, once the
exact (dev) skiko build is pinned.

### `ui-text` now compiles: first Linux platform-backend pieces written

Rather than hunt the unreleased skiko, **shimmed the one `FontStyle` line** (a patched
`NativeFont.native.kt`, the HEAD original excluded in Gradle) to the public `FontStyle(Int, Int,
FontSlant)` API. That surfaced **5 platform `expect`s with no Linux actual**: the darwin equivalents
live in `darwinMain` (NSLocale/CoreText). Provided minimal Linux actuals:

- `Locale` (expect class) + `createPlatformLocaleDelegate()` + `Locale.isRtl()`: i18n/locale;
- `ActualStringDelegate()`: platform string ops (upper/lower/(de)capitalize);
- `createPlatformResolveInterceptor()`: font-resolution hook (no-op).

→ **`ui-text` compiles for Linux K/N.** Five real compose.ui modules now build:
`ui-util + ui-geometry + ui-unit + ui-graphics + ui-text`. These actuals (+ `trace`, + the FontStyle
shim) are the **first concrete pieces of the Linux platform backend**: the `ui-glfw`/text spec,
enumerated by the compiler, not guessed.

### `ui:ui` (ComposeScene) reached: surface enumerated

Added `ui:ui` (244 commonMain + 92 skikoMain + …). **`ComposeScene` itself lives in `skikoMain`**
(`scene/ComposeScene.skiko.kt`), platform-agnostic, compilable; only the **mediator**
(`ComposeSceneMediator.ios.kt` / `.desktop.kt`, window+input) is platform-specific = the `ui-glfw`
backend. Compiling `ui:ui` gave **847 errors, but dominated by missing *dependencies*, not platform
gaps**: `androidx.navigationevent`, more `savedstate`/`lifecycle-viewmodel-savedstate`, a `retain`
API. Same pattern as `runtime-saveable`: add the published deps → the count collapses → the platform
`expect` surface (the mediator = `ui-glfw`) remains. **No fundamental wall at ComposeScene.**

### Dep-hunt result: route A2 converges to route A1 at `ui:ui`

Chasing `ui:ui`'s deps reveals the terminal insight: some are **not resolvable against published
artifacts**.

- `lifecycle-viewmodel-savedstate`, `savedstate-compose`: published for linux K/N ✅.
- **`androidx.navigationevent`** (+ `navigationevent-compose`): imported by `ui:ui`, **no linux-K/N
  klib published** (404). It *is* in the monorepo, so buildable via A2, but it's yet another module.
- **`androidx.compose.runtime.retain.*`** (`RetainedValuesStore`, `retain`, `RetainObserver`, …): a
  **HEAD-era compose.runtime API not in the published 1.9.0 runtime.** Using it needs a newer
  (unpublished-for-linux-K/N) runtime, or building runtime from source.

So `ui:ui` HEAD's dependency graph **outruns the published linux-K/N artifacts**: it needs HEAD-only
APIs (`retain`) and in-monorepo-but-unpublished modules (`navigationevent`). Route A2 (against published
deps) therefore **doesn't terminate at `ui:ui`: it converges to route A1** (build the monorepo from
HEAD), because **jb-main HEAD is the only source with linux-K/N support and it is ahead of releases.**

**Key observation: this is an official rollout in progress.** JetBrains has *already published*
linux-K/N klibs for the **foundation** (skiko, runtime, runtime-saveable, lifecycle, savedstate,
collection, annotation) but **not yet** `ui`/`foundation`/`material3`/`navigationevent`. The partial
publication is the signature of an **in-flight rollout**: the UI layer for Compose-on-K/N-Linux is
coming from JetBrains. So the pragmatic options are (1) build from HEAD now (route A1, the whole
monorepo, all deps aligned), or (2) wait for the official linux-K/N `ui`/`material3` artifacts.

### Assessment

The real compose.ui stack **ports to Linux K/N by mechanical work**: source-set mapping, a skiko
version shim, published-dep additions, and a growing set of small native actuals (the Linux platform
backend). **No fundamental blocker found across five modules (incl. the skiko graphics + text layers),
and `ComposeScene` is in shared skiko code.** The remaining cost is (a) volume, dep hunting + finishing
`ui:ui`/`foundation`/`material3`, and (b) the real `ui-glfw` platform backend (the `ComposeScene`
mediator: window, input, fonts, density), which the compiler is now enumerating for us.

### BREAKTHROUGH: `ui:ui` + `ComposeScene` compile for linuxArm64 K/N (route A2, no monorepo build)

The "wall" described above (the `retain` API needs an unpublished runtime, `navigationevent` unpublished)
**fell.** Route A2 does terminate at `ui:ui`: `BUILD SUCCESSFUL`, `ui:ui` **and `ComposeScene` compile to
a Kotlin/Native linuxArm64 klib**. Two levers cleared the block:

1. **Bump the published deps to their newest linux-K/N versions.** The linux-K/N klibs only exist at the
   rollout's leading-edge versions, not the stable ones I was pinning: `compose.runtime` **1.12.0-beta01**
   (vs 1.9.0 → provides the HEAD APIs `HostDefaultKey`, `compositionLocalWithHostDefaultOf`,
   `CancellationHandle`, `scheduleFrameEndCallback`), `lifecycle` **2.11.0-rc01** (lifecycle's linux-K/N only
   starts there), `savedstate` 1.3.0-alpha06, `savedstate-compose` 1.4.0. The skew was not HEAD-vs-published,
   it was published-stable-vs-published-leading-edge.
2. **Compile 3 small unpublished modules from source** (`runtime-retain`, `navigationevent`,
   `navigationevent-compose`) via the same A2 mechanic (source sets pointing at the monorepo). None depend
   on `android.*`.

A count of **795 errors then collapsed to 4 and to 0** with two pure-config moves:

- **765 of the 795 were an opt-in lint** ("Unstable API for use only between compose-ui modules sharing the
  same version", i.e. `@InternalComposeUiApi`), not real errors. The real `ui:ui` build opts in **globally**
  to these; replicating that (`languageSettings.optIn(...)` for `InternalComposeUiApi`,
  `ExperimentalComposeUiApi`, `InternalComposeApi`, `ExperimentalComposeRuntimeApi`, `InternalTextApi`)
  removes all 765.
- The last 4 unresolved came from a missing module, `lifecycle-viewmodel-compose:2.11.0-rc01` (package
  `androidx.lifecycle.viewmodel.compose`: `LocalViewModelStoreOwner`, `ViewModelStoreOwnerHostDefaultKey`).

#### The mediator surface is bounded: 20 platform actuals, enumerated

With deps settled, `ui:ui` only wanted **20 `expect` declarations with no `actual`** for the native target.
That is **exactly the mediator boundary** (what POC 3/4 de-risked), not diffuse `android.*`. Real-work
breakdown:

- **5 files reused verbatim from `macosMain`** (native K/N, no Apple API): `InteropView` (=`Any`),
  `DragAndDrop` (stub classes), `Focusability` (`systemDefinedCanFocus = true`), `PlatformVelocityTracker`
  (`= Lsq2VelocityTracker()`), and notably **`Key.macos.kt`** (582 lines of key-code table, pure Kotlin).
- **3 backhandler `expect` actuals** (`BackHandler`, `PredictiveBackHandler`, `BackEventCompat`) provided by
  `ui-backhandler`'s **shared `jbMain`** source set (no rewrite).
- **Only 3 files touching a platform API**, stubbed for Linux (~60 lines total): `PointerIcon` (marker cursor
  objects), `PlatformClipboard` (in-memory clipboard), `PlatformUriHandler` (no-op, `xdg-open` TODO).

In other words, the "platform-specific" part of `ui:ui` for Linux reduces to **3 small stubs**; everything
else is either shared skiko code or reusable from the existing Apple-native actuals.

#### Proof

`klib dump-metadata` on the produced linuxArm64 klib lists, under `builtins_platform=NATIVE`,
`compiler_version=2.3.0`, target `linuxArm64`:

```
interface androidx/compose/ui/scene/ComposeScene
abstract class androidx/compose/ui/scene/BaseComposeScene : ComposeScene
final class androidx/compose/ui/scene/CanvasLayersComposeSceneImpl : BaseComposeScene, ComposeSceneContext
class androidx/compose/ui/scene/PlatformLayersComposeScene ...
```

**Direct decision impact.** Route A2 **did NOT need the monorepo build** (route A1), contrary to what the
previous section concluded. `ComposeScene` runs on shared skiko code compiled for linux K/N, against
**published** artifacts + 3 source modules + a handful of actuals. The only remaining piece to *display* a
window is the **`ui-glfw` mediator** (wire `ComposeScene` to a GLFW window: window, input, fonts, density),
whose GLFW + skiko plumbing was already proven at POC 3/4. **Clear GO signal: the gap is not "diffuse
android.*", it is a bounded, enumerable mediator.**

### Time spent

- Jalon 2 (this session): ~40 min (recon + leaf compiles).
- Jalon 2 (extension, this session): ~50 min (`ui:ui` + `ComposeScene` green for linux K/N).

## Jalon 3: `foundation` + `material3` compile for linuxArm64 K/N

`gradle compileKotlinLinuxArm64` → **BUILD SUCCESSFUL, 0 errors.** The entire Compose UI stack now compiles
to a Kotlin/Native linuxArm64 klib: `ui` (Jalon 2) **plus** `foundation`, `foundation-layout`, `animation`,
`animation-core`, `material-ripple`, `graphics-shapes`, and `material3`. `klib dump-metadata` exports the
real composables: **`Button`, `Text`, `Card`, `Checkbox`, `Scaffold`, `Slider`, `OutlinedTextField`,
`DatePicker`** (material3) and **`LazyColumn`, `LazyRow`, `BasicTextField`, `Canvas`, `Image`** (foundation),
50 `material3`/`foundation`/`animation` packages.

### Recipe (deltas from Jalon 2)

- **Modules added from source** (none published for linux K/N, all 404): `animation-core`, `animation` (with
  its `nonAndroidMain` set), `foundation-layout`, `foundation`, `material-ripple` (with `nonAndroidMain`),
  `graphics-shapes`, `material3`.
- **Published deps added / bumped** (rule: target the newest linux-K/N versions, alphas included, because
  linux K/N only exists at the rollout's leading edge): `kotlinx-datetime:0.7.1` (DatePicker),
  `collection`/`annotation` **1.12.0-alpha02** (vs 1.10.0).
- **Global opt-ins** extended: `ExperimentalFoundationApi`, `InternalFoundationApi`, `ExperimentalLayoutApi`,
  `ExperimentalAnimationApi`, `ExperimentalMaterial3Api`, `ExperimentalMaterial3ExpressiveApi`,
  `InternalMaterial3Api`, `ExperimentalGraphicsShapesApi`.
- **Gradle/Kotlin heap** raised to 8 GB (`gradle.properties`): at this scale (metadata ~393k lines) the
  daemon GC-thrashed at 512 MB.

### Platform surface: still bounded, mostly free

- **`foundation`: 44 `expect` actuals**, all present in `macosMain` (native K/N). The **24
  `foundation/macosMain` files are Apple-API-free** and reused verbatim; **only 3** leaked an AppKit bit,
  patched one line each: `platformScrollConfig` (wheel deltas → stub, mediator will supply),
  `NativeClipboard.hasText`/`pasteboardItems` (adapted to Jalon 2's in-memory clipboard).
- **`animation-core`: 1 actual**, `getCurrentThread(): Any`, written in 3 lines via `@ThreadLocal`
  (per-thread identity, which `rememberTransition`'s check requires).
- **`material3`: 5 actuals.** `PlatformRipple`/`createPlatformRippleNode` come for free from
  `material-ripple/nonAndroidMain`. The remaining 3 are the **DatePicker i18n layer** (`CalendarLocale`,
  `defaultLocale`, `PlatformDateFormat`), written on **kotlinx-datetime** with English labels (~130 lines,
  POC-grade) where macOS relies on `NSDateFormatter`/`NSLocale`. `createCalendarModel` and the bulk of the
  calendar are already **shared skiko** code (`KotlinxDatetimeCalendarModel`).

### Decision impact

The full `ui` + `foundation` + `material3` stack **ports to linux K/N by mechanical work**: source-set
mapping, leading-edge published deps, reuse of the existing Apple-native actuals, and a handful of small
Linux actuals. The **only** genuinely-new, non-trivial code written so far is the **DatePicker i18n layer**
(localized, replaceable by ICU later). Exactly **one** piece remains to *display and animate* a real
material3 UI on linux K/N without a JVM: the **`ui-glfw` mediator** (Jalon 4), whose GLFW + skiko plumbing is
already proven at POC 3/4.

### Time spent

- Jalon 3 (this session): ~1 h (foundation + material3 + deps + date/locale actuals).

## Jalon 4: the `ui-glfw` mediator renders and animates REAL material3 on K/N Linux, no JVM

**Achieved, screenshot-proven.** The real JetBrains `ComposeScene` (compiled at Jalons 2-3) is wired to a
**GLFW** window through a **skiko GL** surface, and renders genuine **material3** content (`MaterialTheme` +
`Button` + `Text`), interactive, on Linux arm64 **with no JVM**.

- Before: [`docs/poc5-material3-knative-arm64.png`](./docs/poc5-material3-knative-arm64.png) (`count: 0`).
- After a click (synthetic, injected at the button center):
  [`docs/poc5-material3-after-knative-arm64.png`](./docs/poc5-material3-after-knative-arm64.png)
  (`count: 1`). The real material3 `Button`'s `onClick` goes through the `ComposeScene`'s **real
  hit-testing + layout**, triggers recomposition, and the skiko re-render. This is **not** POC 4's
  hand-drawn output: it is the material3 `Button` composable, with the theme's primary color, shape, and
  white label.

### The mediator (~180 lines, `main.kt`)

The template is `ImageComposeScene` (shared skiko), extended with a window + input:

1. `CanvasLayersComposeScene(frameRecomposer, density, size, platformContext)` + `setContent { App() }`.
2. A minimal `PlatformContext` with a `WindowInfoImpl` (container size). **Key trick:** the mediator is
   **compiled in the same module** as the compose source, so its `internal` API (`WindowInfoImpl`,
   `CanvasLayersComposeScene`, `FrameRecomposer`, `PlatformContext.Empty`) is directly reachable.
3. Per-frame loop: `drivePostDelayed(t)` → `frameRecomposer.performFrame(t)` → `scene.measureAndLayout()`
   → `scene.draw(skiaCanvas.asComposeCanvas())` → `context.flush()` → `glfwSwapBuffers`.
4. Input: GLFW click → `scene.sendPointerEvent(Press/Release, position, PointerButton.Primary)`.
5. The GLFW + skiko GL base (cinterop `glfw.def`, `DirectContext.makeGL`,
   `Surface.makeFromBackendRenderTarget`) is **reused from POC 4**, as is the vendored linux arm64 `.so`
   set (glfw/GL/EGL/fontconfig/freetype).

### The one platform wall hit at runtime: `Dispatchers.Main`

The binary started (glfw + GL + skiko surface OK) then crashed at `ComposeScene` creation:
`Dispatchers.Main is missing on the current platform`. Cause: `compose.ui#postDelayed` (used by
`RectManager`'s debounce) launches on `Dispatchers.Main`, **absent on K/N Linux**. Fixed by replacing
`postDelayed`/`removePost` with a **frame-loop-drained scheduler** (`LinuxPostDelayed.kt`): the callbacks run
on the **compose thread**, exactly where `RectManager`'s debounce belongs. Two skiko/nonJvm actual files
excluded and re-implemented for Linux (the `expect PostDelayedDispatcher` disappears with them). This is
precisely the kind of small actual the mediator layer owns.

### Build & run

- **Cross-compiled AND cross-linked from the macOS host** to a linuxArm64 `.kexe` (K/N has no linux-aarch64
  host; the link resolves the vendored `.so`). Debug binary: **70 MB**.
- **Run in Docker Linux arm64** (`debian:bookworm-slim` + mesa/llvmpipe + libglfw3 + fontconfig + freetype +
  DejaVu fonts), under **Xvfb** (software GL). 120 frames, clean exit, PNGs captured.
- Rendering is **software (llvmpipe)**: GPU smoothness on real hardware is still to confirm (same caveat as
  POC 2/3/4).

### Time spent

- Jalon 4 (this session): ~1 h 15 (mediator + cinterop + `Dispatchers.Main` fix + Docker run + click proof).

## Jalon 5: K/N vs JVM weight/RAM measurement (the point of the whole series)

Measured on the **release** binary (K/N Linux arm64, the same Jalon 4 material3 app), against POC 2's JVM
figures (same class of app, Linux arm64):

| Metric | POC 2 (CMP Desktop JVM) | POC 5 (K/N Linux, no JVM) | Delta |
|---|---|---|---|
| Shipped weight | **137 MB** (app image, incl. 87 MB JRE) | **35 MB** (self-contained release binary) | **~74%** |
| Idle RAM (peak RSS) | **~224 MB** | **124 MB** | **~45%** (~100 MB) |

- Weight drops 137 -> 35 MB: the **87 MB JRE is gone** (the K/N binary bundles no JVM), the rest is linked
  native code. The unoptimized debug binary is 70 MB; release is 35 MB.
- RAM drops ~224 -> 124 MB. The reduction is real (~100 MB) but not proportional to weight: **Skia dominates
  RAM** regardless of runtime (already noted at POC 3). Dropping the JVM removes its overhead, not Skia's
  footprint.
- **Caveat:** RSS measured under **software rendering (llvmpipe)** on Xvfb; on real GPU the memory profile
  will differ. Indicative figure, consistent with the POC 2/3/4 caveats.

**Series conclusion.** The original goal (drop the JVM behind POC 2's 137/224 MB) is **achieved and
quantified**: a real material3 app, rendered and interactive, runs on **Kotlin/Native Linux with no JVM**,
at **35 MB / 124 MB**. The cost is **maintaining an out-of-JetBrains `ui-glfw` backend** (the mediator + a
handful of small Linux actuals), until JetBrains publishes the UI layer's K/N Linux artifacts (which the
Maven poll watches for).

### Time spent

- Jalon 5 (this session): ~20 min (release build + RSS measurement).

## Jalon 6: the same stack on linuxX64 (x86_64), no arch-specific code

Jalons 1 to 5 all targeted **linuxArm64**. SKIKO-611, the ticket that tracks K/N Linux, is about
**x86_64**. So the open question was whether any of this carries over to the other architecture, or
whether arm64 was doing something special.

It carries over, and there was nothing to port.

**What changed** (build plumbing only, no Kotlin was written):

- `src/linuxArm64Main/` -> `src/linuxMain/`, a source set shared by both Linux targets. This is the same
  layout upstream PR #2027 uses (`compose/ui/ui/src/linuxMain/...`).
- `linuxX64` target declared next to `linuxArm64`; both go through the same `linuxTarget()` helper.
- GLFW cinterop commonized (`kotlin.mpp.enableCInteropCommonization=true`): the `.def` is header-only, so
  the bindings resolve from the shared source set.
- `linkerOpts` is the **only** arch-dependent input: which `.so` files to link (`native/glfw/lib` for
  arm64, `native/glfw/lib-x64` for x86_64). That is linking, not Compose.
- `scripts/fetch-deps.sh` now also stages the x86_64 `.so` files; `scripts/run-native.sh` takes an arch
  argument (`arm64` by default, so existing invocations are unchanged).

**Result.** `compileKotlinLinuxX64` is green on the first run, 0 errors. The two klibs are structurally
identical: 101 compose packages each (48 `ui`, 39 `foundation`, 7 `material3`, 4 `animation`), 23 MB of
IR each, with `MaterialTheme`, `Button`, `Scaffold`, `LazyColumn`, `DatePicker`, `CanvasLayersComposeScene`
all present in the x64 klib. None of the 44 Linux actuals needed an arm64/x64 split.

| | linuxArm64 | linuxX64 |
|---|---|---|
| Compile | 0 errors | 0 errors |
| Release binary | 35 MB | **38 MB** (ELF x86-64) |
| Renders material3 | yes | yes |
| Click -> `count` 0 to 1 | yes | yes |

Run with `scripts/run-native.sh poc5-native release x64`, in a `linux/amd64` container under Xvfb.
Captures: `docs/poc5-material3-knative-x64.png` (count: 0) and `docs/poc5-material3-after-knative-x64.png`
(count: 1).

**Caveats.** x86_64 runs under **qemu emulation** on an ARM host, and rendering is **software (llvmpipe)**,
so this says nothing about GPU smoothness on real x86 hardware. The click is **synthetic** (injected), as in
Jalon 4. Same evidence level as Jalon 4/5, transposed to x64: no more, no less.

**Rebuilt from a fresh `jb-main`.** `.cmc` was re-cloned from `jb-main` HEAD on 2026-07-14 and the arm64
baseline still compiles with 0 errors, no patching. This is a same-week check, not a long-term one: the POC
was written days earlier, so it says the recipe reproduces from a clean checkout, not that it survives
upstream drift over months. That question stays open, and the Maven poll is what watches it.

### Time spent

- Jalon 6 (this session): ~1 h (source set restructure + x86_64 libs + link + run).

## Jalon 7: from "it renders" to "it is usable" (the platform layer)

Jalons 1 to 6 proved the compose stack runs. They did not make it usable: `scene.sendKeyEvent` was never
called, the mouse wheel was dead on both ends, the clipboard was a `String` in process memory, and the
locale was hardcoded to `en-US`. An inventory of the 43 Linux actuals plus the mediator found 9 that were
stubs by their own admission, and 2 `TODO()` that threw `NotImplementedError` at runtime.

Two lots, both verified by driving the running app with **real X11 input** (`xdotool`) and reading back the
**real system clipboard** (`xclip`) from another process. Nothing is simulated in-process: if a GLFW
callback or an actual is not wired, the test fails.

### Lot 1: keyboard, wheel, clipboard, cursors, resize, HiDPI

| | Before | After |
|---|---|---|
| Keyboard | `sendKeyEvent` never called | `xdotool type "hello"` lands in the text field |
| Mouse wheel | dead on both ends | the list scrolls (item 0 -> 5) |
| Clipboard | a `String` in memory | `xclip`, another process, reads `copied:hello` |
| Cursors | inert marker strings | real I-beam over the text field |
| Resize | surface frozen at 520x300 | surface, render target and scene size rebuilt |
| HiDPI | `Density(1f)` hardcoded | from `glfwGetWindowContentScale` |
| Clock | `frame * 16ms` | `clock_gettime` (animations run in seconds, not frames) |

The mediator now wires GLFW callbacks for key, char, scroll, mouse button, motion, resize and focus. They
are `staticCFunction` (they capture nothing), so they push into a global queue that the frame loop drains.

**`Key.linux.kt` did not need rewriting.** It carries Apple key codes, which the inventory flagged as a
multi-day job. But Compose only ever compares keys against its own constants (`Key.Backspace`, `Key.C`),
never against raw numbers, so a GLFW -> `Key` translation table in the mediator is enough. The constants'
underlying values are irrelevant.

Other actuals: clipboard goes through the real X11/Wayland selection via GLFW; `UriHandler` execs
`xdg-open` (fork+execvp, so the uri never reaches a shell, which is what the JVM backend does on Linux
too); the wheel `ScrollConfig` uses JetBrains' own `LinuxGnomeConfig` formula; `KeyMapping` switched from
the macOS map (Cmd-based, so Ctrl+C did nothing) to the Ctrl-based one Compose Desktop uses on Linux.

### Lot 2: system locale and right-to-left

`Locale.current` reads the POSIX environment (`LC_ALL`, `LC_MESSAGES`, `LANG`) instead of returning
`en-US`. `isRtl()` resolves against a table of RTL languages and scripts. The mediator derives the scene's
`LayoutDirection` from it, which is what actually mirrors the UI: the scene defaults to `Ltr` forever
otherwise, so this had to be done in the mediator, exactly as the desktop backend derives it from AWT's
`ComponentOrientation`.

With `LANG=ar_EG` the whole UI mirrors: `docs/poc5-lot2-rtl-arabic.png`.

material3: `CalendarLocale` reads `LC_TIME`, and `PlatformDateFormat` lets the locale drive the first day
of the week, the 12h/24h clock, and the order of the date input fields (region-derived, per CLDR territory
data). It previously handed every locale on earth a US date format **and** a Monday-first week, which
contradict each other.

### Two bugs only the runtime tests could find

- **The canvas was never cleared between frames.** Compose paints only what it owns, so every frame was
  drawn on top of the last one. A static screen hid this completely; a text field turned the screen into
  mush. This bug was present since Jalon 4 and invisible to the compiler.
- **The pointer never moved.** `xdotool --window` sends synthetic events (XSendEvent): GLFW delivers the
  wheel, but the pointer never actually moves, so Compose received every scroll at (0,0) and nothing
  scrolled. Driving through XTEST (no `--window`) fixed it. The lesson is the POC's own: trust the real
  run, not the compiler.

### What is still missing, and what it costs

- **ICU.** Month and weekday names are still English. This needs CLDR data. The blocker is not code, it is
  a design decision: ICU exports **version-suffixed symbols** (`udat_open_72`, never `udat_open`), so a
  binary linked against ICU 72 will not start on a distro shipping ICU 74. Options: link hard (fine only
  if you control the distro), `dlopen`+`dlsym` with runtime suffix detection (portable, more code), or
  embed minimal CLDR data (no dependency, limited coverage).
- **IME** (`zwp_text_input_v3` or IBus/Fcitx over D-Bus). No virtual keyboard and no CJK input without it.
  Not verifiable in this harness: it needs a real compositor or input-method daemon.
- **Accessibility** (AT-SPI2 over D-Bus), rich clipboard (MIME types) and drag-and-drop. Worth knowing what
  the JVM desktop backend actually does on Linux, since it sets the bar: the accessibility tree **is** built
  and exposed there (`accessibleContextProvider` is handed to the SkiaLayer on every OS), but **focus
  notification does nothing on Linux**: `requestFocusOnAccessible` wires only Windows (Java Access Bridge)
  and macOS (`CAccessible`), and returns early on anything else. So a screen reader could read the tree but
  would never be told what has focus. Partial, not absent.

### Time spent

- Jalon 7 (this session): ~2 h (inventory, Lot 1, Lot 2, X11 input test harness).

## Jalon 8: real localization (ICU at runtime), and tests that click by name

Jalon 7 left month and weekday names in English: CLDR data cannot be derived, it has to be read. Getting it
means ICU4C, and ICU comes with a trap.

### Why ICU is dlopen'd, not linked

ICU renames every exported symbol with its major version. The library exports `udat_open_72`, never
`udat_open` (the headers rewrite the call with a macro). **A binary linked against ICU 72 therefore fails to
start on a distro shipping ICU 74.** For a binary meant to be shipped, that is not acceptable.

So the loader (`icu/IcuLoader.kt`) `dlopen`s the library, finds which version is actually installed, and
resolves the suffixed symbols with `dlsym`. The bindings (`icu/IcuApi.kt`) cover date formatting, the
calendar, locale orientation and case mapping. Every call degrades to the previous behaviour when ICU is
absent, so the app still runs without it.

The test verifies this directly: **the binary carries no ICU entry in its `NEEDED` list**. It starts against
ICU 72, ICU 74, or none at all.

### What the locale now actually does

| | Before | After |
|---|---|---|
| Month/weekday names | English, always | `14 juillet 2026`, week starts `lundi` (fr_FR) |
| Date field order | region table | CLDR pattern via `udatpg_getBestPattern` |
| First day of week | region table | `ucal_getAttribute` |
| 12h/24h | region table | derived from the locale's own time pattern |
| RTL | hand-kept table | `uloc_getCharacterOrientation` (table is now the fallback) |
| Case mapping | root locale (Turkish `i` -> `I`, wrong) | locale-aware (`u_strToUpper`) |

### Tests click by name, not by pixel

The input tests drive the app with real X11 events, so they need screen coordinates. Hardcoding them broke
silently: adding two lines to the UI shifted every widget by ~45px, the clicks landed on labels, and the
tests still reported PASS because the events did reach Compose, just not the widget.

The mediator now walks Compose's semantics tree, the one `Modifier.testTag()` writes into, and dumps each
tag with its real bounds (`testsupport/SemanticsExport.kt`). Tests aim at tags. The same tree is what an
AT-SPI accessibility bridge would consume, so this is not throwaway plumbing.

### Five bugs the tests found, three of which were making tests pass wrongly

- **`LD_LIBRARY_PATH=/nonexistent` does not hide a library from `dlopen`**, which searches the system paths
  anyway. The "without ICU" test passed while ICU was loaded the entire time.
- **Removing libicu breaks mesa**: its DRI drivers pull ICU in through libxml2, so the window never opens
  and the app dies before reaching any ICU code. On a Linux box that renders with GL, ICU is always
  present. "No ICU at all" is not a real scenario; independence from the ICU *version* is the real benefit,
  and that is what the test now checks.
- **`python3-minimal` ships without the `json` module.** Reading the tag dump failed silently, so every
  click went to the centre of the screen. The dump is now a plain `tag x y w h` line read with awk.
- A lone `y` in a pattern was treated as two digits (`26` instead of `2026`). CLDR says `yy` is two digits;
  `y` is the full year.
- The no-ICU fallback did not reorder the skeleton's fields, producing `26 July 14`.

### What is still missing

- **IME** (`zwp_text_input_v3`, or IBus/Fcitx over D-Bus). No virtual keyboard, no CJK input. Not verifiable
  in this harness: it needs a real compositor or input-method daemon.
- **Accessibility** (AT-SPI2 over D-Bus). The semantics tree is now exported, which is the input side of it.
  For reference, the JVM desktop backend is only partly there on Linux: it builds and exposes the
  accessibility tree, but never notifies focus changes (see Jalon 7).
- **Rich clipboard** (MIME types beyond plain text) and **drag-and-drop**.

### Time spent

- Jalon 8 (this session): ~1 h 30 (ICU loader + bindings, semantics export, three test suites).

## Jalon 9: native Wayland, in the same binary as X11

Every previous Jalon ran on X11. That is no longer enough: **Budgie 10.10 moved to Wayland (labwc, wlroots)
and Ubuntu Budgie 26.04 ships no X11 session at all**. An X11-only binary would run there only through
XWayland, in degraded mode. Supporting GNOME and KDE, in Wayland and in X11, is a hard requirement.

### The blocker was a dependency version, not code

The stack was on **GLFW 3.3.8**, which picks its backend **at compile time**: Debian ships `libglfw3` (X11)
and `libglfw3-wayland` as two incompatible packages. A 3.3 binary is X11-only, full stop.

**GLFW 3.4 selects the backend at runtime** (`glfwGetPlatform`, `glfwPlatformSupported`) and `dlopen`s it,
so GLFW itself links neither X11 nor Wayland. Debian trixie ships 3.4, and marks `libglfw3-wayland` as a
*transitional* package: one library, both backends.

### The binary still pulled X11 in, through libGL

Bumping GLFW was not enough, and the first version of this Jalon claimed otherwise. `ldd` on the binary
showed **`libX11.so.6`**, and it did not come from GLFW: it came from **`libGL`**. Desktop GL carries GLX,
which is X11 by construction, so `-lGL` drags libX11 in. On a Wayland-only system with no libX11, the binary
would not have started, which is exactly the case this Jalon exists to support.

It turned out to be removable. The app references **no GLX symbol at all**: it resolves GL through EGL
(`eglGetCurrentDisplay`, `eglGetProcAddress`), and all **105** `gl*` symbols it needs are provided by
`libGLESv2`. Linking `-lGLESv2` instead of `-lGL` drops libX11 entirely:

    before: libglfw.so.3, libGL.so.1, libX11.so.6, ...
    after:  libglfw.so.3, libGLESv2.so.2, libGLdispatch.so.0     (no libX11, no libwayland)

Both suites stay green after the change. So the binary now genuinely has **no display-server dependency at
link time**: it picks X11 or Wayland at runtime, and needs neither present to start.

So this Jalon is a dependency bump (bookworm -> trixie) plus one linker change plus a test harness. No
platform code was written.

### What is proven

| | |
|---|---|
| GLFW backend chosen at runtime | **Wayland**, with **no X server present at all** (`DISPLAY` unset) |
| Both backends in one binary | `wayland supported = true, x11 supported = true` |
| Rendering | skiko GL surface on Wayland (EGL path), frames drawn |
| Keyboard | real Wayland key events reach Compose |
| X11 regression | none: the 10 X11 assertions still pass |

The test (`scripts/test-wayland.sh`) runs **sway with the headless wlroots backend**, the same wlroots family
labwc (hence Budgie) uses. There is deliberately **no X server in the container for that run**: if GLFW had
fallen back to X11, there would have been nothing to fall back to and the app would have died. Rendering a
frame is therefore itself the proof that the Wayland path works.

Input goes through `wtype` (the virtual-keyboard protocol), because **xdotool cannot drive a Wayland
client**: Wayland forbids one client from injecting events into another. That is the security model X11
never had, and it is worth knowing before planning any Wayland automation.

### The dlopen bet paid off by accident

trixie ships a different ICU than bookworm. **The same binary, with no recompilation, found ICU 72 on
bookworm and ICU 76 on trixie, and worked on both.** That is exactly the scenario the runtime loading of
Jalon 8 was built for, and it happened for real, unplanned: a binary linked against ICU 72 would simply not
have started here.

### Automating a Wayland UI: there is no xdotool, and that is by design

Wayland **forbids a client from injecting events into another client**. This is the security model X11 never
had, and it means no generic injector exists. Every input needs an explicit protocol **and** a compositor
that cooperates (`scripts/test-wayland-input.sh`):

| Input | Tool | Protocol | Works headless |
|---|---|---|---|
| Keyboard | `wtype` | `zwp_virtual_keyboard_v1` | **yes** |
| Pointer | `wlrctl` | `zwlr_virtual_pointer_v1` | **no**, see below |
| Clipboard | `wl-clipboard` | `wl_data_device` | needs focus |
| Window geometry | `swaymsg` | compositor IPC | yes |

**The pointer cannot be driven on a headless seat, and this is measured, not assumed.**
`swaymsg -t get_seats` reports **`capabilities: 0`**: the headless wlroots backend attaches no input device,
so the seat advertises no pointer, so a client never creates a `wl_pointer`, so virtual-pointer events go to
nobody. Verified directly: five `wlrctl` move+click rounds and sway's own cursor commands both produced
**zero** pointer events in the app.

This is a property of the **headless compositor**, not of the app. The mediator has **no X11/Wayland branch
at all** (it is the same GLFW callback code), and pointer input is proven under X11. So the Wayland suite
reports those checks as **SKIP**, not PASS. Closing the gap needs a seat with a real input device: a real
desktop session, or a `uinput` device in a privileged container. Neither belongs in this harness.

Note also that under Wayland the **client cannot resize itself**: the compositor owns geometry. The window
must be floating (tiling overrides `resize set`), and the surface the app receives is smaller than the size
requested, because the compositor subtracts its decorations (asked 900x700, app got 896x673).

### Three harness bugs worth recording

- **The legacy Docker builder ignores `--platform`.** `DOCKER_BUILDKIT=0` (added earlier to work around a
  registry timeout) silently produced an **amd64** image on an arm64 host, and the arm64 `.kexe` failed with
  "No such file or directory". Keep buildkit; pull the base image first if it times out.
- **Every `wtype` invocation loses its first keystroke.** It creates a throwaway virtual keyboard, sends its
  own keymap, and the first key is dropped while the client reloads it (measured: a separate warm-up call
  produced zero events; `"wayland"` always arrived as `"ayland"`). The fix is a sacrificial character inside
  the same string. This is not a keyboard-layout problem: the lost character is always the first one
  whatever it is, and `w` arrives as `w` (codepoint 119), not as a mismapped `z`.
- **`swaymsg` without `SWAYSOCK` fails silently.** An earlier run reported resize as PASS while `swaymsg`
  had never executed: what actually resized the window was sway's own tiling at startup. Same failure mode
  as the ICU and tag-dump bugs of Jalon 8: a test passing for the wrong reason.

### Time spent

- Jalon 9 (this session): ~1 h (GLFW 3.4 bump, trixie image, Wayland harness, regressions).

## Jalon 10: IME probe. The Wayland input-method path works end to end.

The last thing missing from the platform layer is the IME. Without it there is **no virtual keyboard and no
CJK input**, which on a mobile device means the user cannot type at all. Everything else (clipboard,
cursors, wheel, locale, RTL) is done; this is not.

### The claim that had to be checked first, and was wrong

I had written that "one IBus client over D-Bus covers X11 and Wayland". **False**, and it mattered: it was
the basis of a 4-to-6 week estimate.

- **Measured**: our wlroots compositor advertises `zwp_text_input_manager_v3` and
  `zwp_input_method_manager_v2`. Under Wayland the application speaks **text-input-v3 to the COMPOSITOR**,
  and the compositor relays to the input method (over input-method-v2). The app does **not** talk to IBus.
- **Corroborated**: under Wayland the recommended setting is `GTK_IM_MODULE=wayland`, not `ibus`. And
  JetBrains has the same work open: **JBR-5672**, "Wayland: support input methods (text-input-unstable-v3)".

So there are **two** IME backends, not one: `text-input-v3` (Wayland) and XIM/IBus (X11). The good news is
that the Wayland one needs **no D-Bus at all**, and if the target is Wayland-only there is only one backend
to write.

### What the probe proves

The app now binds `zwp_text_input_v3` on the `wl_display` GLFW already owns (`glfwGetWaylandDisplay`),
opens its own registry to bind the text-input manager and the seat, and drives the protocol. The full loop,
with a minimal input method written for the test (`scripts/docker/fake-ime.c`, speaking input-method-v2):

    IME  -> registered as the input method for this seat
    IME  -> activate                      (triggered by OUR app's enable())
    IME  -> preedit_string 'compo'
    IME  -> commit_string 'IME-OK'

    APP  <- bound zwp_text_input_manager_v3 + wl_seat
    APP  <- created zwp_text_input_v3
    APP  <- enable + commit sent
    APP  <- enter                         (compositor gave this surface text focus)
    APP  <- preedit_string='compo'        (text being composed)
    APP  <- commit_string='IME-OK'        (text the IME committed)

app -> text-input-v3 -> compositor -> input-method-v2 -> IME -> back to the app. And it is not the app's own
log that proves it: `WAYLAND_DEBUG=1` shows the messages on the wire:

    -> wl_registry#34.bind(21, "zwp_text_input_manager_v3", 1, new id #35)
    -> zwp_text_input_manager_v3#35.get_text_input(new id zwp_text_input_v3#29, wl_seat#36)
    -> zwp_text_input_v3#29.enable()
    -> zwp_text_input_v3#29.set_cursor_rectangle(16, 100, 600, 60)
    -> zwp_text_input_v3#29.commit()

**For a mobile target, the key line is `activate`**: our `enable()` is what wakes the input method up. That
is the same mechanism that makes a **virtual keyboard** appear, and `maliit-keyboard`, `squeekboard` and
`wvkbd` all speak this same input-method-v2.

### Three traps, all in the test scaffolding, none in the real path

- The `input-method-v2` XML is **not** in `wayland-protocols`: it is a wlroots protocol, and Debian does not
  package it. It lives in the wlroots repo. Two plausible URLs returned an **HTML error page**, which would
  have been fed to wayland-scanner as if it were a protocol.
- The `serial` passed to `zwp_input_method_v2.commit()` must be **the serial of the `done` event received**,
  not a counter of one's own. Get it wrong and the compositor **silently drops the request**: the first run
  sent the preedit and lost the commit_string, with no error anywhere.
- The compositor emits `done` **only when state changes**. Waiting for a second `done` waits forever; a real
  IME gets one per keystroke.

### What is NOT done

The app **receives** the preedit and the commit, but does **not insert them into the text field** yet: the
`TextField` does not move. Wiring that up means implementing Compose's `PlatformTextInputService` (start/stop
input, feed the real cursor rectangle, render the preedit underlined), which is Compose work, not Wayland
work. **Estimated 8 to 15 days**, and the main technical risk is now retired. X11 (XIM or IBus) would be a
separate backend on top.

### Time spent

- Jalon 10 (this session): ~1 h 30 (cinterop, text-input-v3 client, test input method, full loop).

## Jalon 11: the IME text lands in the Compose text field

Jalon 10 proved the protocol loop: the app receives what the IME composes and commits. But the text went
nowhere, because nothing fed it into Compose. This closes that gap: **the IME's text now appears in the
`TextField`**.

The IME's committed text (`IME-OK`) and its preedit (`compo`, rendered underlined, which is how Compose
shows text still being composed) are both in the field, and Compose's own state agrees:
`typed: [IME-OKcompo]`.

Capture: `docs/poc5-ime-wayland-textfield.png`.

### Compose has TWO text-input contracts, and picking the wrong one fails silently

This is the trap, and it cost three iterations:

- the **legacy** `PlatformTextInputService` (`startInput` / `stopInput`), and
- the **modern** `PlatformContext.startInputMethod(request)`, a `suspend` function returning `Nothing`.

**material3's `TextField` goes through the modern one.** Implementing only the legacy one gives a text field
that takes focus, shows its caret, and never enables the IME. There is no error anywhere: the default
`startInputMethod` is `awaitCancellation()`, so it simply suspends forever.

What made the mistake visible: **`stopInput()` was being called and `startInput()` never was.** Compose was
talking to the service, just not through the method that matters.

The modern contract is the right one anyway: it carries `onEditCommand` (how text gets in) and
`focusedRectInRoot` (where to put the candidate window or the virtual keyboard).

### Wiring, and the two ordering bugs it exposed

`LinuxTextInputService.startInputMethod` stores `onEditCommand`, enables the Wayland text-input with the
caret rectangle, and then suspends until Compose cancels the session (the field lost focus), disabling the
IME in the `finally`. Committed text becomes a `CommitTextCommand`; preedit becomes a
`SetComposingTextCommand`, which Compose renders underlined and replaces on the next update.

The Wayland listeners are `staticCFunction`: they capture nothing and cannot call into Compose. They queue
(`ImeState`), and the frame loop drains the queue on the compose thread. Same pattern as the GLFW callbacks.

Two ordering bugs, both silent:

- **The protocol must be bound BEFORE `setContent`.** Compose focuses the field during composition and
  starts an input session immediately; if the protocol is not bound yet, that `enable()` lands on nothing.
- **The IME must not answer the first activation.** The compositor has not yet given the surface text focus
  (`enter` has not arrived), so it drops everything sent. And Compose **tears the session down and
  re-establishes it** right after focus, so the first activation is not the one that sticks. A real IME
  never hits either, because a human takes time to type.

### What is left

`surrounding_text` (the context an IME uses to predict), `delete_surrounding_text`, and live caret tracking.
Refinements, not unknowns. A real IME (fcitx5) or a virtual keyboard (`maliit`, `squeekboard`, `wvkbd`) sees
exactly what the test input method sees, so **the on-screen keyboard path for a phone is open**.

No regressions: X11 (10 assertions) and Wayland (7) both still pass.

### Time spent

- Jalon 11 (this session): ~1 h (modern contract, edit commands, ordering fixes).

## Jalon 12: drag and drop, which no Kotlin/Native Compose target has

### The starting point: this is not our gap, it is everyone's

Before writing anything, it is worth knowing what the official ports actually do. The macOS K/N target, which
JetBrains maintains, has this:

    actual class DragAndDropEvent private constructor()
    internal actual val DragAndDropEvent.positionInRoot: Offset
        get() = TODO("Not yet implemented")

A **private constructor**: no `DragAndDropEvent` can be created at all. Drag and drop therefore does not
exist on any Kotlin/Native Compose target. Ours was a copy of that, with the same `TODO()`.

The same holds for the other two gaps people would point at:

- **`InteropView = Any`**: identical on macOS K/N. It is not a stub, it is the honest answer: in Kotlin/Native
  there is no native widget toolkit to embed (iOS has `UIView`, desktop has Swing; native Linux has neither).
- **Accessibility**: **no K/N target has any**, macOS included (no a11y file exists in `macosMain`).

And `ClipEntry.clipMetadata` throws `TODO("ClipMetadata is not implemented")` on **every platform**,
including the **JVM desktop backend that ships in production**. That one is not ours to fix.

### What was built

`DragAndDropEvent` now carries what the window system gives us (a position and the dropped file paths), so
`positionInRoot` returns a real value and the `TODO()` is gone. GLFW's drop callback feeds it, and the
mediator hands it to `ComposeScene.rootDragAndDropNode`, the same entry point the desktop backend uses.

Files dropped on the window reach a Compose `dragAndDropTarget`: `dropped: [report.pdf, photo.png]`.

### Two bugs worth recording

- **The target was behind the padding.** `Modifier.padding(16.dp).dragAndDropTarget(...)` puts the drop zone
  *inside* the padding, so a drop at the window origin lands outside it and Compose rightly refuses it.
- **`onMoved` is not optional.** Sending `started -> entered -> drop` returns `accepted=false` with no error:
  without `onMoved`, Compose never hit-tests the pointer position, so no target is under it and the drop is
  delivered to nobody. The real sequence is `started -> entered -> moved -> drop -> ended`.

### Scope, stated plainly

**File drops INTO the app.** Dragging OUT of the app, and rich MIME payloads, would need XDND (X11) or
`wl_data_device` (Wayland) directly.

And the drop is **injected** in the test, not produced by a real drag: nothing in the harness can act as a
drag source (`xdotool` cannot, and Debian packages no such tool). What IS exercised is everything downstream
of the window system: the actual, `rootDragAndDropNode`, Compose's routing, and the UI. The GLFW callback
that feeds it (`glfwSetDropCallback`) is standard wiring but is **not covered by a test**.

No regressions: X11 (10 assertions), Wayland (7) and ICU (9) all still pass.

### Time spent

- Jalon 12 (this session): ~45 min.

## Route A1 (full monorepo build): recipe + wall, and the Maven poll

**Maven poll (2026-07-11, 17:45Z):** `org.jetbrains.compose.ui:ui-linuxarm64`,
`foundation-linuxarm64`, `material3-linuxarm64`, `ui-backhandler-linuxarm64`,
`org.jetbrains.androidx.navigationevent:navigationevent-linuxarm64` → **all 404 (not yet published)**.
The **foundation** klibs (skiko, runtime, runtime-saveable, lifecycle, savedstate, collection,
annotation) **are** published for linux K/N: a partial, in-progress rollout.

**Maven poll (2026-07-14), now covering x64 too:** `ui`, `foundation` and `material3` are still **404 for
both `linuxx64` and `linuxarm64`**, at 1.12.0-beta02 (the latest). `skiko` 0.150.1 and
`compose.runtime` 1.12.0-beta02 publish **both** Linux architectures. So the gap this POC fills is
unchanged after 14 months, and it is not arm64-specific: nobody ships the UI layer for K/N Linux.

**Why it is still missing, upstream.** Thomas Vos's PR
[compose-multiplatform-core#2027](https://github.com/JetBrains/compose-multiplatform-core/pull/2027)
("Compose UI for native Linux") has been **open as a draft since 2025-04-16**, with no JetBrains review.
The blocker is not missing code, it is an unanswered design question he raised himself in the PR: with
`expect/actual`, actualizing the windowing layer to Wayland breaks the embedded / no-window-manager use
case, and there is no clean way to make it pluggable. Jake Wharton makes the same point in the Kotlin
Slack `#compose` thread, and JetBrains (Ivan Matkov) confirmed they want to move to polymorphism but that
"it's not so easy". His skiko work did land: `skiko#1051` (linuxArm64 target) and `skiko#1052` (EGL) are
both merged and published, which is why the foundation is in place while the UI layer is not.

**Route A1 recipe** (to build `compose-multiplatform-core` from HEAD, established from the fork's build
files):

1. **Full checkout** of the monorepo: a sparse/`blob:none` clone is insufficient (the
   `gradle-wrapper.jar` blob is missing, and `settings-fork.gradle` `include`s ~hundreds of modules).
2. Fork build entry: `./gradlew -c settings-fork.gradle` + `build-fork.gradle` + the setup script
   `buildSrc-fork/settingsScripts/out-setup.groovy`. Property `androidx.studio.type=jetbrains-fork`.
3. **`ANDROIDX_JDK21`** env → a JDK 21 (`org.gradle.java.installations.fromEnv=ANDROIDX_JDK21`).
4. **Enable the Linux native targets** (`linuxX64`/`linuxArm64`) in the shared multiplatform convention
   (jb-main enables ios/macos/desktop only).
5. `kotlin.native.enableKlibsCrossCompilation=false` in `gradle.properties` → **native klibs must be
   built on Linux** (not cross-compiled from macOS), or flip the flag.
6. Then build `:compose:ui:ui`, `:compose:foundation:foundation`, `:compose:material3:material3`,
   `:navigationevent:*` for `linuxArm64` (androidx builds want ~16 GB+ RAM, lots of disk, hours).

**Verdict on A1:** it is a **multi-day build-infrastructure task**, not session-work: recipe and the
6 prerequisites are established; running it (multi-GB checkout + fork setup + monorepo build on macOS
with cross-compilation disabled) was not attempted to completion, as it would near-certainly stall and
burn the budget with no artifact. **And it is likely unnecessary:** JetBrains is actively publishing the
linux-K/N stack bottom-up (foundation done, UI pending), so the pragmatic path is to **monitor the UI
artifacts and, when they land, write only the `ui-glfw` mediator** (already de-risked in POC 3/4).

### Automated monitoring

A recurring Maven poll for `ui-linuxarm64` / `material3-linuxarm64` / `foundation-linuxarm64` is set up
as a scheduled routine, to notify when the UI klibs publish. At that point POC 5 collapses to wiring the
`ui-glfw` mediator onto the POC 4 stack.

## Remaining milestones (multi-session)

- **Jalon 2: DONE.** `ui-graphics`, `ui-text`, `ui-backhandler`, then `ui:ui` (with `ComposeScene`)
  compile to a linuxArm64 K/N klib via A2, against published deps + 3 source modules (`runtime-retain`,
  `navigationevent`, `navigationevent-compose`) + 3 Linux actual stubs. See the "BREAKTHROUGH" section
  above.
- **Jalon 3: DONE.** `foundation` + `material3` (+ `foundation-layout`, `animation(-core)`,
  `material-ripple`, `graphics-shapes`) compile to a linuxArm64 K/N klib. See the "Jalon 3" section above.
  Klib exports `Button`/`Text`/`DatePicker`/`LazyColumn`/`Scaffold`…
- **Jalon 4: DONE.** The `ui-glfw` mediator wires the real `ComposeScene` to a GLFW window and renders
  interactive material3 (`Button`/`Text`, click -> `count` 0->1) on K/N Linux, no JVM. See "Jalon 4".
- **Jalon 5: DONE.** Release **35 MB** vs 137 MB JVM; peak RSS **124 MB** vs ~224 MB JVM. See "Jalon 5".

### Reproducibility note

`poc5-native/build.gradle.kts` points at a clone of `compose-multiplatform-core` (branch `jb-main`,
scratch path, `cmcRoot` var). Monorepo source sets pulled in: `compose/ui/{ui-util, ui-geometry, ui-unit,
ui-graphics, ui-text, ui-backhandler, ui}` (their `commonMain`/`skikoMain`/`nonJvmMain`/`nativeMain`/
`skikoExcludingWebMain` per the hierarchy), plus `compose/runtime/runtime-retain`, `navigationevent/
navigationevent`, `navigationevent/navigationevent-compose`. Linux actuals added under
`poc5-native/src/linuxArm64Main/`: `NativeFontPatched` (skiko FontStyle shim), `LinuxLocale`,
`LinuxStringDelegate`, `LinuxResolveInterceptor`, `TraceActual`, `PointerIcon.linux`,
`PlatformClipboard.linux`, `PlatformUriHandler.linux`, plus 5 files copied from `ui/src/macosMain`
(`Key`, `DragAndDrop`, `Focusability`, `PlatformVelocityTracker`, `InteropView`). Key published deps:
`compose.runtime:1.12.0-beta01`, `lifecycle:2.11.0-rc01`, `savedstate:1.3.0-alpha06`,
`savedstate-compose:1.4.0`, `skiko:0.150.1`. Global compose opt-ins required (see the `sourceSets.all`
block). Command: `gradle compileKotlinLinuxArm64` (produces the klib under
`build/classes/kotlin/linuxArm64/main/klib`).

### Time spent

- POC 5 (this session): ~40 min (recon + leaf compile).
