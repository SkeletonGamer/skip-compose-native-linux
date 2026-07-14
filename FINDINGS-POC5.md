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
