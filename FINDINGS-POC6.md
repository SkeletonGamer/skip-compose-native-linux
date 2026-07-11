# FINDINGS: POC 6: de-Android-ify SkipUI / SkipFoundation to target Compose Multiplatform

> English canonical. French copy: [`FINDINGS-POC6.fr.md`](./FINDINGS-POC6.fr.md).
> POC 1-5 FINDINGS are closed. POC 6 re-opens exactly one question POC 1 left on the table.

## Why this POC exists

POC 1 concluded NO-GO on a Skip to CMP transpiler because SkipUI/SkipFoundation looked "diffusely
coupled to Android". But POC 1 tested that by *excluding* files (breaking the monolith raised errors
68 -> 264). POC 5 later taught the opposite lesson: a "diffuse-looking" coupling can be a bounded,
enumerable set of platform actuals, and effort estimates lean conservative. POC 6 re-tests the Skip
coupling with POC 5's method: keep the monolith whole, shim the Android surface, compile against CMP.

The target split matters:
- **CMP Desktop (JVM)**: `java.*` is present, so SkipFoundation's Java backing survives. Tractable first.
- **CMP on Kotlin/Native Linux (no JVM, POC 5's world)**: `java.*` is gone, SkipFoundation needs native
  reimplementation. The heavy finale.

## Jalon 0: recon. Verdict: the coupling surface is BOUNDED, not diffuse

Measured on the Skip-transpiled Kotlin (from POC 1's `skip export`).

**SkipUI**: 234 files, 72,729 lines.

| Imports | Count | Note |
|---|---|---|
| `androidx.compose.*` | 1815 | CMP provides these (same package names). The bulk. |
| `android.*` | 53 (in 18 files) | Android-only, concrete shimmable APIs (Build, Intent, Context, Bitmap, Log, Uri, haptics...). |
| `androidx.navigation3.*` | 32 (in 3 files) | THE hard one: navigation3 has no CMP artifact (POC 1/2). |
| other `androidx.*` non-compose | ~43 | lifecycle 9 (CMP has it), activity 9, work 6, core ~15. Bounded shims. |

**SkipFoundation**: 84 files, 24,912 lines. Only 6 `android.*` + 13 `java.*` (java.time 8, java.util 4,
java.io 1). On JVM desktop the `java.*` just work; for K/N they map to known libs (kotlinx-datetime, okio).

**So ~7% of imports are Android-only, concentrated in a minority of files.** That is countable and
localized, not "android.* everywhere". This revises POC 1: POC 1 measured structural monolithism
(exclusion cascades); Jalon 0 measures the Android-only surface with the monolith intact = bounded.

Prior art: none published. Discussion [skiptools#163](https://github.com/orgs/skiptools/discussions/163)
and a Skip blog note the idea ("target CMP everywhere except Apple; since CMP uses the same APIs as
Jetpack Compose it might already work for the most part"), but nobody shipped it. POC 6 would be novel.

**The single real risk: `androidx.navigation3`** (32 imports, 3 files). No CMP artifact exists. Shim its
~10-symbol surface (NavDisplay, NavKey, NavBackStack...), or drop navigation for a minimal app.

**Decision: GO to Jalon 1.** Import-counting is surface, not depth. Jalon 1 (does it compile against CMP,
does the error set converge or sprawl) is the real signal.

### Time spent

- Jalon 0: ~30 min (source location + coupling measurement + prior art).

## Jalon 1: does the real SkipUI compile against CMP Desktop (JVM)? Verdict: it CONVERGES

Setup: `poc6-skip-cmp/`, a plain `kotlin("jvm")` + Compose Desktop project that pulls the 4 transpiled
Skip modules as source (SkipLib 41, SkipFoundation 84, SkipModel 10, SkipUI 234 = ~369 files) and
compiles them against `org.jetbrains.compose` (Desktop), with `android.jar` as a compile-only stub for
the `android.*` surface (POC 1 found this dissolves the SkipFoundation wall).

The error count is the signal (converge = bounded = POC 1 was wrong; sprawl = diffuse = POC 1 was right):

1. **First compile: 1348 errors.** But dominated by **missing third-party libraries**, not coupling:
   `Icons` (293, material-icons-extended), `coil3` (51, images), `okhttp3` (36), `commonmark` (13),
   `okio` (6). These all have JVM/CMP artifacts.
2. **After adding those 5 libraries (+ a 10 GB compiler heap; material-icons-extended is huge): 535
   errors.** The count collapsed, and what remains IS the coupling.

Of the 535: **325 real "unresolved reference"** + ~94 cascades (78 type-inference, 29 lost
`@Composable` context, once a type is unresolved its users fail too) + **39 material "experimental"
opt-ins** (trivial). So the true coupling surface is ~325, and it is **concentrated in ~6 identifiable
subsystems**, not spread across the 234 files:

| Subsystem | ~errors | Where | Fix |
|---|---|---|---|
| `androidx.navigation3` | ~75 | Navigation.kt (85), TabView.kt (88) | THE hard one: no CMP artifact. Shim its ~10 symbols, or drop nav for a minimal app. |
| Android notifications | ~53 | UserNotifications.kt | stub or drop (whole feature) |
| App lifecycle | ~37 | UIApplication.kt | shim (desktop host, like POC 5's PlatformContext) |
| Presentation / dialogs | ~31 | Presentation.kt (`DialogWindowProvider`) | CMP dialog shim |
| Image | ~40 | AsyncImage.kt, Image.kt (`asAndroidPath`, coil svg) | bounded shims / deps |
| Android compose-locals | ~28 | `LocalContext`, `LocalConfiguration`, `LocalView` | provide desktop CompositionLocals |
| resources / prefs | ~19 | `stringResource`, `getSharedPreferences` | CMP resources + a prefs shim |
| `material3.adaptive`, dynamic color, misc | ~40 | `currentWindowAdaptiveInfo`, `dynamicLightColorScheme`, `testTagsAsResourceId` | adaptive dep + stubs |

**Verdict: the coupling converges to a bounded, enumerable set of subsystem shims. It does NOT sprawl.**
This empirically confirms Jalon 0 and revises POC 1: the port is not "diffuse android.* everywhere", it
is a countable list of localized decisions (shim navigation3, stub 3-4 Android-only features, provide
~30 compose-local shims, add 2-3 deps).

**Honest caveat:** "converges to bounded subsystems" is not "trivial". Reaching a GREEN compile is
Jalon 2/3 work: the navigation3 shim is genuinely non-trivial (no CMP artifact), and notifications /
work / app-lifecycle are whole Android features to stub or drop. But that is enumerable and localized,
which is the opposite of POC 1's "diffuse/hopeless" framing. The person-weeks estimate for a *full clean*
port may still hold; the "don't bother, it's diffuse" conclusion does not.

### Time spent

- Jalon 1: ~40 min (project setup + 2 compile iterations + categorization).

## Jalon 2: drive toward a green compile. Verdict: monotone convergence, bounded

The method: keep all 4 Skip modules whole, and fill the Android surface with (a) published libraries,
(b) `android.jar` compile-only for `android.*`, (c) the one androidx that has a JVM variant
(`navigation3`, compile-only non-transitive so its compose refs bind to CMP), and (d) ~18 hand-written
shim files for the rest (the POC 5 "provide the actuals" method).

Error trajectory, each step a deliberate action:

| Step | Errors | Action |
|---|---|---|
| First compile | 1348 | (baseline) |
| + 5 published libs (material-icons-extended, coil3, okhttp, okio, commonmark) | 535 | most of 1348 was just missing libraries, not coupling |
| + `navigation3` compile-only non-transitive | 347 | the "hard one" resolved via a single dep |
| + opt-ins + coil-svg + kotlin-reflect | (noise removed) | - |
| + 13 shim files (compose-locals, adaptive/window-size, dynamic color, string resources, activity, work, semantics, graphics interop, dialog provider) | 136 | - |
| + 5 more shims (core.* compat, worker classes, insets controller) | **107** | - |

**It converges monotonically and never sprawls.** 1348 -> 107 with published deps + one navigation3 dep +
~18 small shim files. The residual ~107 is concentrated in **~5 deep subsystems**, each a bounded but
real chunk: `NavigationSuiteScaffold` (TabView.kt, 19), `NotificationCompat` (UserNotifications.kt, 17),
activity-result + app lifecycle (UIApplication.kt, 17), a **coil API-version skew** (AsyncImage/Image,
~23: `fetcherFactory`, `memoryCacheKey`), and small tails (commonmark strikethrough extension, Android
font-scale converter).

**Verdict: the port is mechanical, bounded, and localized. It did NOT sprawl.** Reaching a *fully green*
compile is the enumerable multi-day work of deep-stubbing a handful of *complete* Android feature APIs
(NotificationCompat, the NavigationSuiteScaffold API, activity-result) plus aligning the coil version.
This confirms Jalons 0-1 decisively and revises POC 1's "diffuse/hopeless" framing: the coupling is a
countable list of localized decisions. The person-weeks estimate for a *full clean* port still holds;
the "don't bother, it's diffuse" conclusion does not. The grind was stopped at 107 on purpose: this is a
throwaway probe, and driving deep Android feature stubs to zero is diminishing-returns past the signal.

### Time spent

- Jalon 2: ~1 h (dependency strategy + ~18 shim files + 6 compile iterations).

## Jalon 3: toward green + a rendered screen. Verdict: Android coupling fully resolved, residual is pure version skew

Pushed on from 107: more shims (activity-result, core.app, navigation-suite scaffold, back handler,
font-scaling, insets appearance) + excluding the two notification files (leaf, 0 core references). Landing:
**1348 -> 33 errors**, via `android.jar` (compile-only) + one `navigation3` dep + ~26 hand-written shim
files + 2 excluded notification files.

**The decisive finding: the remaining 33 errors are, with one exception, all library-VERSION skew, not
Android coupling.** SkipUI was transpiled against specific versions of coil / Compose / material3 /
navigation3, and this project uses different ones:

- **coil (~22)**: `ImageRequest.Builder.fetcherFactory` / `memoryCacheKey` / `size` do not exist on
  coil 3.0.4's builder (Image.kt, AsyncImage.kt). A coil version mismatch.
- **Compose (2)**: `Font.kt` passes a `Typeface` where the CMP 1.9.0 overload expects `Boolean`.
- **navigation3 (2)**: `rememberNavBackStack` wants `SavedStateConfiguration`, SkipUI passes its own key
  type (nav3 1.2.0-alpha05 vs SkipUI's target).
- **material3 (2)**: `Picker.kt` `ContentPadding`, `Presentation.kt` `sheetGesturesEnabled` (ModalBottomSheet
  API drift). `Shape.kt` one receiver mismatch.

These version-skew sites were closed the way a real port would: a coil bump (3.5.0), the navigation3
**desktop** artifact (it exists), a small `rememberNavBackStack` overload, and ~10 one-line source patches
of the transpiled output (drop the Android-only `autoSize` / `sheetGesturesEnabled` args, `FontFamily.Default`
for `FontFamily(Typeface)`, `SegmentedButtonDefaults.ContentPadding` -> `PaddingValues()`, and neutralize the
Android-coil image-loading chains, unused on a desktop witness). Result: **`BUILD SUCCESSFUL`, 0 errors.**
**The full trajectory: 1348 -> 535 -> 347 -> 136 -> 107 -> 33 -> 15 -> 3 -> 0.**

### Green + render: ACHIEVED

The transpiled SwiftUI `ContentView` renders through the **real SkipUI** into a PNG on Compose Multiplatform
Desktop, no Android: [`docs/poc6-skipui-render.png`](./docs/poc6-skipui-render.png) shows "Count: 0" (a
SwiftUI `Text`) and an "Increment" `Button` (material3 accent). Offscreen via `ImageComposeScene`.

Getting a pixel exposed the RUNTIME side (distinct from compile):
- **Skip's JVM Context bootstrap.** `Bundle.main` (triggered by any `Text`) makes SkipFoundation's
  `ProcessInfo` reflectively call `androidx.test.core.app.ApplicationProvider.getApplicationContext()` (the
  androidx-test / Robolectric path Skip uses on the JVM). We faked it with a class of that name returning a
  **Mockito** `Context` (`RETURNS_MOCKS`), so the render boots without a full Robolectric environment.
- **The Android compose-locals must have values.** `LocalContext` / `LocalConfiguration` / `LocalView` are
  read during `Text` rendering (`EnvironmentValues.getLocale`). `android.jar` types throw `Stub!` if
  instantiated, so they are backed by Mockito mocks too.

That is the honest shape of it: a real production port would swap those mocks for Robolectric (Skip's own
JVM runtime) or desktop-backed `Context`/`Configuration`, and would port (not disable) the coil image path.
But the vertical is proven end to end: **transpiled SwiftUI -> real SkipUI -> CMP Desktop -> a rendered
screen, no Android.**

**Bottom line for POC 6:** POC 1's "diffuse / hopeless" verdict on de-Android-ifying SkipUI is decisively
wrong. The Android coupling is a countable, localized set of shims + patches (done here), the compile is
green, and a transpiled SwiftUI screen actually renders on CMP Desktop. The person-weeks estimate for a
*clean, maintained, production* port still holds (Robolectric or a real desktop Context, port the image
subsystem, restore notifications, track Skip upstream); the "don't bother, it's diffuse" does not.

### Time spent

- Jalon 3: ~2 h (activity-result/scaffold/insets shims, version-skew closure, source patches, the
  ApplicationProvider + locals runtime bootstrap, and the render).

## Jalon 4 (started): push to Kotlin/Native Linux, no JVM. The wall, measured by the compiler

Jalon 3 ran on CMP Desktop (JVM), where SkipFoundation's `java.*` backing just works. K/N Linux has no
`java.*`, so Jalon 4's question is the size of that wall. New project `poc6-native/`: a `kotlin("multiplatform")`
linuxArm64 target that compiles the transpiled Skip modules from source (no compose yet) and lets the
**compiler, not grep**, measure the gap. Started at the base module, SkipLib (everything depends on it).

**SkipLib: 100 -> 33 compiler errors with 5 tiny value-type shims.** The first compile was 100 errors; 5
small shims under `poc6-native/src/linuxArm64Main/kotlin/shims/` closed 67 of them:
- `@androidx.annotation.Keep` (42 errors, all one annotation) -> a no-op annotation.
- `java.util.Random` / `java.security.SecureRandom` -> backed by `kotlin.random`.
- `java.lang.Character` code-point helpers -> BMP-only.
- `java.util.regex.Matcher.quoteReplacement` -> a 6-line escaper.
- `java.lang.UnsupportedOperationException` -> typealias to Kotlin's; plus one K/N opt-in.

**The residual 33 is the genuinely hard core: exactly three reimplementations plus one trivial.**

| Site | Errors | Nature |
|---|---|---|
| `Concurrency.kt` | 22 | Skip's `Task` engine: `GlobalScope.async(dispatcher + ThreadLocal.asContextElement(state))` + `synchronized`. Swift structured concurrency built on JVM coroutines + `ThreadLocal` (coroutines-android). Needs a native rewrite (a K/N lock + a coroutines task-local). |
| `Numbers.kt` | 7 | `java.math.BigInteger` / `BigDecimal` (arbitrary precision). No K/N stdlib equivalent; needs a bignum. |
| `String.kt` | 2 | `String.format` (printf-style). No K/N stdlib equivalent; needs a formatter. |
| `AsyncStream.kt` | 2 | `@JvmName` (test-only signature hint). Trivial: drop the two lines. |

So even the base module does not go green on K/N without reimplementing Skip's concurrency core, a bignum,
and a string formatter. That is a different kind of wall from the Android surface: the Android coupling was
bounded shims over an otherwise-working compose; the K/N coupling is in Skip's runtime **core**.

**And SkipLib is the light module.** SkipFoundation carries the weight: 710 `java.*` references across 34
files, concentrated in the date/calendar subsystem (`java.util.Calendar` alone: 231 refs; `DateComponents.kt`
134, `Calendar.kt` 104), the filesystem (`java.nio.file`, `FileManager.kt` 110), number formatting
(`java.math.BigDecimal`), and networking (`java.net.*`, plus okhttp / commonmark / org.xmlpull, all JVM-only).
Porting it is reimplementing a slice of java.time + java.text + java.nio + java.math + an HTTP/markdown/XML
stack natively (kotlinx-datetime, okio, ktor are the usual substitutes).

### Jalon 4 result: SkipLib + SkipFoundation compile GREEN on K/N Linux, no JVM

The wall was then driven to zero. **Both SkipLib and SkipFoundation now compile to a `linuxArm64`
Kotlin/Native klib with no JVM (`BUILD SUCCESSFUL`, 0 errors), down from a 2103-error first compile.**
125 transpiled Skip files compile natively.

The method is the `android.jar` approach turned on the JDK: the port does **not** reimplement Foundation's
behavior, it provides the **`java.*` / `android.icu` / third-party API surface** SkipFoundation calls, as
compile-only stubs (`TODO()` bodies) with correct signatures and return types, exactly as `android.jar`
stubbed the Android surface on the JVM side in Jalon 3. Plus a handful of real native substitutes where
cheap.

- **51 shim files, ~2400 lines**, spanning 40+ packages: `java.util` (Calendar/Date/Locale/TimeZone/
  Currency/UUID/Base64/logging/concurrent/stream/Timer/Scanner), `java.time`(`.format`/`.temporal`/`.zone`),
  `java.text`, `java.nio.file`(`.attribute`)/`.charset`, `java.net`, `java.io`, `java.math`, `java.security`,
  `java.lang` (Number/System/Class/Thread/Runtime/Integer/Byte/Method), `javax.crypto`, `kotlin.reflect.full`,
  `android.os`/`util`/`content`(`/pm`/`res`)/`net`/`icu`(`.text`/`.util`), `okhttp3`, `okio`, `org.commonmark`,
  `org.xmlpull`, `org.json`.
- **Real native substitutes** where trivial: SkipLib's `Task` engine on atomicfu (`synchronized`) + a
  coroutines-native task-local (a no-op `asContextElement`, best-effort); `BigInteger`/`BigDecimal` on the
  ionspin multiplatform bignum; `Charsets` + string encoding on kotlin's UTF-8; `java.util.Random` /
  `Character` / regex-quote / `String.format` (a printf subset).
- **~12 one-line source patches** to the transpiled output (like Jalon 3): drop `@JvmName` / `@JvmStatic` /
  `@JvmOverloads` (JVM-only annotations, invalid in a K/N leaf source set), swap `Dispatchers.IO` (internal
  on K/N) for `Dispatchers.Default`, decode bytes via `decodeToString()` instead of `java.lang.String(bytes,
  charset)`, and add a few `import java.lang.System/Class/Integer` (java.lang is not auto-imported on K/N).

**What this proves:** the K/N wall for Skip's runtime foundation is **bounded and mechanical at the compile
level**: 2103 -> 0 in a single session, mostly by enumerating the JDK API surface Skip touches. The nullability
edge (Kotlin's strict types vs Java platform types) and the missing-auto-import of `java.lang` were the only
systematic frictions, both handled locally.

**Honest caveat:** this is a GREEN COMPILE with stub bodies, not a functional runtime. The stubs throw
`TODO()` at runtime, just as `android.jar` throws `Stub!`; a functional port would replace the date /
calendar / file / number / net stubs with real native impls (kotlinx-datetime, okio, ktor). But the
compile-level port, the thing POC 1 called "diffuse / hopeless", is done.

### The complete Skip stack, green on K/N Linux, no JVM

The push then went all the way up the stack:

- **SkipModel** (snapshot-state on `compose.runtime`): green after adding the published `compose.runtime`
  K/N klib + `java.util.LinkedList` / `java.lang.ref.WeakReference` shims + a nullability fix. Quick.
- **SkipUI** (234 files, 1897 `androidx.compose.*` imports): green. This is the big one. It needs the real
  `compose.ui` / `foundation` / `material3` for K/N Linux, which JetBrains does not publish, so (exactly like
  POC 5) they are compiled from a source checkout of `compose-multiplatform-core` (jb-main) with the same
  KMP source-set hierarchy and Linux `actual`s. On top of that, SkipUI's Android UI surface is stubbed the
  same way as SkipFoundation's: the 1500-error first compile fell to 0 by providing `material-icons` (281
  icon placeholders), `navigation3`, `coil3`, `okhttp`, `android.os/graphics/app/content/view/provider/
  accessibility/database/webkit`, `org.w3c.dom` + `javax.xml`, and the Android-only compose interop
  (`LocalContext`/`LocalConfiguration`, `ContentAlpha`, `currentWindowAdaptiveInfo`, `NavigationSuite*`,
  `pullRefresh`, `asAndroidPath`, `stringResource`, dynamic color, `testTagsAsResourceId`, ...).
- **The transpiled SwiftUI app** (`Witness` `ContentView`) compiles too.

**Final tally: 371 transpiled Skip files (SkipLib + SkipFoundation + SkipModel + SkipUI + Witness) compile
to a `linuxArm64` Kotlin/Native klib, no JVM, on top of the from-source compose stack.** The port is
**115 shim files (~4300 lines)** plus **42 compose Linux `actual` files** (reused from POC 5) plus ~15
one-line patches to the transpiled output. The two systematic frictions were the same throughout: Kotlin's
strict nullability vs Java/Android platform types, and symbols the JVM auto-imports (`java.lang.*`) or
exposes as companion statics that K/N does not.

**Bottom line: the entire Skip UI framework, transpiled from SwiftUI, compiles for Kotlin/Native Linux
with no JVM.** POC 1's "diffuse / hopeless" verdict is decisively overturned, at the compile level, for the
whole stack. The residual is runtime: the stubs (`android.jar`-style) must be swapped for functional native
impls for an app that actually renders.

### Time spent

- Jalon 4: ~6 h total (K/N project setup, wall categorization, SkipLib/Foundation/Model 2103 -> 0, then the
  compose-from-source merge + SkipUI 1500 -> 0, via 115 shim files driven by the compiler and parallelized
  across sub-agents for the bulk stubbing).

## Jalon 5 (done): link, run, render, and measure. No JVM.

The compiled stack was linked into an actual `linuxArm64` executable (glfw + skiko GL mediator, same as
POC 5) and run in a Linux arm64 container under Xvfb (software GL). Two results.

**Weight.** The whole thing links to a **37 MB** release binary (93 MB debug), no JVM: the FULL Skip UI
framework (transpiled from SwiftUI) + the from-source compose stack, versus **137 MB** for the POC 2 JVM
Compose Desktop app (the JRE alone is 87 MB). Dead-code elimination is decisive: 37 MB is only ~2 MB above
POC 5's minimal material3 sample, because the linker strips the unused Skip surface. (A prerequisite for
linking: Skip's ~130 `external fun Swift_*` JNI bridges compile but have no native symbol, so they were
patched to throwing stub bodies, unused in a Kotlin-only render.)

**Render + RAM.** The binary does not just link, it RUNS: the transpiled SwiftUI `ContentView` renders
through the real SkipUI, no JVM. [`docs/poc6-skipui-native-render.png`](./docs/poc6-skipui-native-render.png)
shows "Count: 0" (a SwiftUI `Text`) and an "Increment" `Button` (material3), the same screen the JVM side
produced in Jalon 3, now 100% native. Peak RSS: **~122 MB** (124820 kB), versus ~224 MB for the POC 2 JVM app.

**Interactive, not just rendered.** We injected a synthetic tap (`ComposeScene.sendPointerEvent`
Press+Release) on the button, then recomposed: "Count: 0" becomes "Count: 1"
([`docs/poc6-skipui-native-click.png`](./docs/poc6-skipui-native-click.png)). So the click drives a real
recomposition of the transpiled `ContentView`, on K/N Linux with no JVM, not merely a static first draw.

Getting from green-compile to a live render is the "runtime demining": the `android.jar`-style stubs throw
`TODO()` at runtime, so each one on the boot+render path was made functional or benign, driven by the actual
crash traces (about 10 fixes):
- a functional desktop `Context` (`DesktopContext`) with benign resources / prefs / dirs, wired into
  SkipFoundation's `ProcessInfo.androidContext` (one source patch, replacing the JVM's Robolectric /
  ApplicationProvider reflection);
- a functional `java.net.URI` (RFC 3986 parser) so Skip's `Bundle` URL bootstrap succeeds;
- functional `System` (real epoch time via posix `gettimeofday`, sensible property defaults), `KClass.java`
  (carries the qualified name), `Configuration.locales` (default `LocaleList`);
- benign `PackageManager` / `Log` / `commonmark` (return defaults / empty trees instead of throwing);
- `setURLStreamHandlerFactory` as a no-op.

That is the honest shape of the runtime: a basic screen renders with ~10 targeted fixes; a full app (real
assets, images via coil, networking via ktor, dates via kotlinx-datetime) would functionalize the rest of the
stubs the same way, following the same crash-driven loop.

**Series goal reached and measured: the entire Skip UI framework, transpiled from SwiftUI, both COMPILES and
RENDERS on Kotlin/Native Linux with no JVM, at 37 MB / 122 MB versus 137 MB / 224 MB on the JVM.**

### Time spent

- Jalon 5: ~2 h (entry point + glfw/skiko mediator, release/debug link, and the ~10-fix runtime demining loop
  to first render, plus the weight/RSS measurement).

## One shared export for both targets (JVM + K/N)

The two POC 6 halves are separate build projects (`poc6-skip-cmp`, JVM; `poc6-native`, Kotlin/Native
Linux) but read the **same** transpiled Skip code under `./export`. The K/N-specific source patches had
diverged that export to the point where the JVM build no longer compiled. We reconciled it: **a single
export compiles AND renders on both targets**, so there is nothing to maintain twice.

Principle: the export uses only **neutral** forms (ones that resolve on both sides); the symbols carrying a
"K/N" name are supplied to the JVM target as its own small shims. Four points of divergence, all reduced:

- **`value class` without `@JvmInline`** (`StackLayouts.kt`). The JVM requires `@JvmInline` on a value class;
  K/N cannot resolve `@JvmInline` (the `kotlin.*` package is reserved, hence un-shimmable). Resolved by
  making it a **plain class** (drop `value`): compiles on both, identical behaviour, we only lose inlining.
- **`skip.lib.synchronized`** (import in `Publisher.kt`). Needed on K/N (no builtin `synchronized`); on the
  JVM we provide a `skip.lib.synchronized` shim delegating to the builtin `kotlin.synchronized`, with the
  `EXACTLY_ONCE` contract so assignments to captured locals inside the block type-check (Skip relies on it).
- **`android.content.DesktopContext`** (`ProcessInfo.kt`). K/N's benign desktop `Context`; on the JVM we
  provide a `val DesktopContext` backed by the **same** Mockito context `ApplicationProvider` already fakes.
- **`Context.CLIPBOARD_SERVICE`** (`UIPasteboard.kt`). The K/N import path `Context.Companion.CLIPBOARD_SERVICE`
  does not resolve on the JVM (`android.jar`'s `Context`, a Java class, has no `Companion`). Switched to
  **qualified access** `Context.CLIPBOARD_SERVICE`, which resolves on both (static field on the JVM, companion
  const on K/N): it was the import, not the access, that diverged.

Finally, `android.jar` detection (JVM, compile-time only) is made cross-platform: `ANDROID_JAR`, then
`ANDROID_HOME` / `ANDROID_SDK_ROOT`, then the usual per-OS SDK locations (macOS `~/Library/Android/sdk`, Linux
`~/Android/Sdk`, Windows `%LOCALAPPDATA%\Android\Sdk`), picking the highest installed platform.

**Automated reproduction.** `./export` regenerates in one command (`scripts/setup.sh`: `fetch-deps`, then
`skip export`, then `scripts/patch-export.sh`). `skip export` transpiles **two witness screens**
(`witness-app/Sources/Witness/`): `MinimalContentView` (rendered by POC 6) and POC 1's richer `ContentView`,
which compiles but is not rendered. `patch-export.sh` applies `scripts/export.patch` (the broad `Swift_*`
stub transform plus ~20 SkipLib/Foundation/Model/UI edits), which holds **only the changed lines** (no Skip
source context). The patch targets the pinned Skip version; a different version may need it regenerated.

Verified end to end: `scripts/run-native.sh poc6-native` renders on K/N with no JVM (PNG, ~138 MB RSS) and
`scripts/run-jvm.sh poc6-skip-cmp` renders on CMP Desktop JVM (PNG), both from this one shared export.

## Remaining milestones

- **Jalon 2 (attempted, converged to 107)**: see the Jalon 2 section above. 1348 -> 107 via deps + ~18
  shims; the remaining tail is deep-stubbing NotificationCompat / NavigationSuiteScaffold / activity-result
  + a coil version alignment. Bounded, multi-day, stopped on purpose (throwaway probe).
- **Jalon 3**: a Skip-transpiled SwiftUI screen rendered via the real SkipUI on CMP Desktop.
- **Jalon 4 (DONE at compile level)**: the ENTIRE Skip stack (SkipLib + SkipFoundation + SkipModel + SkipUI +
  the transpiled Witness app, 371 files) compiles GREEN on K/N Linux, no JVM, on top of the from-source
  compose stack. 115 compile-only shim files + 42 compose Linux actuals + ~15 source patches.
- **Jalon 5 (DONE, see the section above)**: linked to a 37 MB release binary, and the transpiled SwiftUI
  `ContentView` RENDERS on K/N Linux with no JVM (37 MB / 122 MB RSS vs 137 MB / 224 MB JVM), via ~10 targeted
  runtime fixes (functional Context/URI/System, benign PackageManager/Log/commonmark).
- **Beyond the series**: a fully functional runtime (real assets, coil images, ktor networking, kotlinx-datetime
  dates) would functionalize the remaining stubs the same crash-driven way; plus the standing cost of tracking
  Skip upstream.
