# FINDINGS. POC 4: a Linux `ui-glfw` backend for Compose on Kotlin/Native (no JVM)

> English canonical. French copy: [`FINDINGS-POC4.fr.md`](./FINDINGS-POC4.fr.md). POC 1-3 FINDINGS are
> closed: do not overwrite. POC 3 proved the foundation (skiko + compose.runtime + a GLFW windowing
> brick all run in K/N Linux arm64, no JVM). POC 4 attacks the remaining gap: a real Compose UI.

## Executive summary (session 1: a working interactive Compose UI, no JVM)

**An interactive Compose UI runs on Kotlin/Native Linux arm64 with no JVM**, proven end to end:

- **Jalon 0** ✅: scoping, compose.ui's native code is platform-agnostic (`ui-uikit` isolated to
  `iosMain`), and its shared-native deps (lifecycle, coroutines, compose internals) all have K/N Linux
  klibs. The gap is only the ui modules built for Linux + a platform backend (`ui-glfw`).
- **Jalon 1** ✅: a real `@Composable` composition **composes + recomposes** on K/N Linux (compiler
  plugin + Recomposer + custom Applier).
- **Jalon 2** ✅: that composition is **rendered by skiko in a real GLFW window**: boxes + fontconfig
  text, live counter ([`docs/poc4-ui-knative-arm64.png`](./docs/poc4-ui-knative-arm64.png)).
- **Jalon 3** ✅: a real **GLFW click → recompose** (count 0→1:
  [before](./docs/poc4-ui-before-arm64.png) / [after](./docs/poc4-ui-after-arm64.png)).
- **Jalon 4** ✅: the whole interactive UI is a **24 MB** self-contained native binary vs POC 2's
  137 MB JVM image (no 87 MB JRE).

**This is a hand-written *minimal* `ui-glfw` (Path B)**: it proves the full vertical (composition →
skiko → window → input) works natively without a JVM, and measures it. It is **not** JetBrains'
`compose.ui`: no real measure/layout, no `foundation`/`material3` widgets. Delivering those (so real
Compose apps run) is **Path A**: add Linux K/N targets to compose's ui modules + a real `ui-glfw`
backend, from source: the multi-week job POC 4 Jalon 0 scoped. **Verdict: the approach is proven
viable; a production backend is bounded, known, and remains weeks of engineering.**

> Hindsight (added after POC 5): POC 5 built exactly this Path A, and rendered interactive material3,
> in about half a day. This "multi-week" estimate was too conservative. It applies to the ride-existing-
> Compose path, not to the heavier Skip-porting estimate of POC 1.

## Goal

Make an actual Compose UI (composition → layout → skiko draw → window, with input) run on **Kotlin/Native
Linux, no JVM**, i.e. provide the Linux equivalent of `compose.ui`'s `ui-uikit` backend (call it
`ui-glfw`). R&D, milestone-based; only the current milestone is time-boxed.

## Jalon 0: Scoping (from POC 3 + source/deps recon) ✅

**The structure is favorable and the gap is precisely located.**

- **compose.ui's native code is platform-agnostic except for the platform backend.** In
  `JetBrains/compose-multiplatform-core` (`jb-main`, `compose/ui/ui/build.gradle`), the source sets are
  `skikoMain ← nonJvmMain ← nativeMain`, and the iOS platform backend (`:compose:ui:ui-uikit`) is
  pulled **only in `iosMain`**. So the shared native engine (ComposeScene, layout, draw) lives in
  `nativeMain`; only window/events/fonts are platform-specific.
- **The external deps of that shared native code are already published for K/N Linux:**

  | Dep | linuxArm64 K/N klib |
  |---|---|
  | `skiko` | ✅ (POC 3) |
  | `compose.runtime:runtime` | ✅ (POC 3) |
  | `androidx.lifecycle:lifecycle-viewmodel` / `-runtime` | ✅ |
  | `compose.annotation-internal:annotation` | ✅ |
  | `compose.collection-internal:collection` | ✅ |
  | `kotlinx-coroutines-core` | ✅ |
  | `compose.ui:ui-backhandler` | ❌ (a sibling ui module, built alongside) |

  So nothing external blocks it; the missing pieces are compose's own ui modules built for Linux K/N
  **+ a Linux platform backend**.

### Two implementation paths

- **Path A (build from source)**: fork `compose-multiplatform-core`, add `linuxArm64`/`linuxX64`
  targets to `ui`, `foundation`, `material3`, `ui-backhandler`, and write a `linuxMain`/`ui-glfw`
  platform backend. Real material3 widgets; upstream-able. Heavy monorepo build; weeks.
- **Path B (minimal on published runtime)**: build a small `ui-glfw` on top of the *published*
  `compose.runtime` + `skiko` + GLFW: a custom `Applier` + layout + skiko draw + GLFW input. Proves the
  end-to-end architecture and measures the backend, without material3. Faster; the POC methodology used
  throughout (minimal adapter + measure the gap).

This session pursues **Path B first** (a functional interactive native Compose UI is the killer
signal), and scopes Path A.

## Jalon 1: A real Compose composition composes + recomposes in K/N Linux ✅

Beyond POC 3's snapshot-state: a full **@Composable composition** with the **compose compiler plugin**,
a custom `AbstractApplier` building a node tree, a `Recomposer` + `BroadcastFrameClock`, driven
manually. Cross-compiled macOS → `linuxArm64`, run in the Linux arm64 container:

```
POC4 Jalon1: composed once -> count=0
POC4 Jalon1: after state change -> count=42
POC4 Jalon1: composition+recomposition on K/N Linux, no JVM => OK
```

- Initial composition runs the `@Composable`, the Applier materialises the node (`count=0`).
- A `mutableStateOf` change + `Snapshot.sendApplyNotifications()` + `clock.sendFrame()` triggers
  **recomposition**; the node tree updates to `count=42`. No JVM (native ELF).
- One gotcha: `recomposer.runRecomposeAndApplyChanges()` must be launched with the
  `MonotonicFrameClock` in **its** coroutine context (`launch(clock) { … }`): otherwise
  `IllegalStateException: A MonotonicFrameClock is not available`.

**The composition engine, the heart of Compose (compiler plugin + Recomposer + Applier + snapshot +
frame clock), runs natively on K/N Linux.** The published `compose.runtime` klib is enough. Next:
turn the node tree into pixels via skiko, in the GLFW window (Jalon 2).

### Time spent

- Jalon 1: ~25 min.

## Jalon 2: Composition → skiko draw in a GLFW window ✅ (a real Compose UI on screen)

A minimal `ui-glfw` end to end: `@Composable Box/Label` build a `UiNode` tree through the custom
`UiApplier`; a render loop walks the tree and draws it with **skiko** (rounded rects + text) into the
**GLFW GL window** from POC 3; a `mutableStateOf` counter recomposes live each ~40 frames. On K/N Linux
arm64, **no JVM**, visual proof read back from the GL surface:
[`docs/poc4-ui-knative-arm64.png`](./docs/poc4-ui-knative-arm64.png): a header "Compose on
Kotlin/Native Linux" and a purple button showing a live "count: N".

```
wrote /out/poc4-ui.png (6989 bytes)
POC4: rendered a live Compose UI (count reached 5) in a GLFW window on K/N Linux, no JVM
```

### Link plumbing added (fonts)

skiko's `FontMgr` on Linux uses **fontconfig + freetype**: linking failed on `FcPatternAddString`,
`FcFontSetAdd`, … until `-lfontconfig -lfreetype` were added (arm64 libs extracted like GLFW/GL/EGL).
Text is loaded via `FontMgr.default.makeFromFile(".../DejaVuSans.ttf")` (`Typeface.makeFromFile` does
not exist in the K/N API). Full native link set now: `glfw, GL, EGL, fontconfig, freetype`.

### What this proves

The **entire vertical works natively on K/N Linux with no JVM**: compose compiler plugin + Recomposer
(composition/recomposition) → a custom Applier/node tree (the `ui` layer's job) → **skiko** draw (rects
+ fontconfig/freetype text) → a real **GLFW** window. This is a hand-written *minimal* `ui-glfw` (Path
B): it renders the composed tree but is not JetBrains' `compose.ui` (no real layout/measure, no
foundation/material3 widgets). Binary: 32.5 MB debug (skiko + compose runtime included), still no
separate JRE.

### Time spent

- Jalon 2: ~40 min.

## Jalon 3: Input → recompose ✅ (interactive)

Polled the left mouse button each frame (`glfwGetMouseButton`), detected the press edge, and on a click
incremented the `count` state (`Snapshot.sendApplyNotifications()`) → recomposition → re-render. Driven
by a **real `xdotool` click** on the GLFW window under Xvfb:

- before: [`docs/poc4-ui-before-arm64.png`](./docs/poc4-ui-before-arm64.png), "count: 0"
- after a click: [`docs/poc4-ui-after-arm64.png`](./docs/poc4-ui-after-arm64.png), "count: 1"

A real GLFW mouse event drove Compose state → recomposition → redraw, on K/N Linux, no JVM. (Coarse
hit test = whole window; poll-based edge detection missed some of the 3 rapid `xdotool` clicks under
headless timing, but the click→recompose loop is proven. Real layout-aware hit testing is what
JetBrains' `compose.ui` provides: the piece a full `ui-glfw` would wire in.)

### Time spent

- Jalon 3: ~20 min.

## Jalon 4: Measure ✅

The **complete interactive Compose UI** (compose compiler + runtime + skiko + the minimal ui-glfw) as
a self-contained Kotlin/Native `linuxArm64` binary, **no JVM**:

| | POC 4 (K/N, interactive Compose UI) | POC 2 (JVM Compose Desktop) |
|---|---|---|
| Binary / image | **24.0 MB** release (32.5 MB debug) | 137 MB app image (87 MB JRE + 49 MB jars) |
| Separate JRE | none | 87 MB |

A full Compose UI stack (composition + skiko + windowing) fits in a **24 MB** self-contained native
binary vs a 137 MB JVM image. As POC 3 noted, RAM is Skia-dominated so the RAM gain is modest; the
**footprint** win is decisive.

### Time spent

- Jalon 4: ~10 min.
