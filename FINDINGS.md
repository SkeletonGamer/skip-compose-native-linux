# FINDINGS: Skip → Compose Multiplatform PoC (desktop Linux)

> English is the canonical version (this file) for GitHub. A French working copy is kept in
> [`FINDINGS.fr.md`](./FINDINGS.fr.md).

> **Update (POC 6, later):** this NO-GO headline was reopened. POC 6 de-Android-ified the transpiled
> SkipUI and got it to compile AND render, first on Compose Multiplatform Desktop (JVM), then on
> Kotlin/Native Linux with no JVM (37 MB / 122 MB vs 137 MB / 224 MB on the JVM). So the "diffuse /
> don't reopen" framing does not hold at the compile-and-render level. What still holds is this doc's
> own nuance (the coupling is "concentrated, not diffuse") and the person-weeks estimate for a full,
> maintained, production port. See [`FINDINGS-POC6.md`](./FINDINGS-POC6.md).

PoC journal: real versions, commands actually run, raw errors, fixes, time spent, and at the end
the argued go/no-go decision.

## Question to answer

How much work does it take to run a Skip-transpiled SwiftUI app on Compose Multiplatform desktop
Linux, instead of Jetpack Compose on Android?

## Executive summary

**Decision: NO-GO on a Skip→Compose-Multiplatform transpiler for the target device. Go Compose-first.**
The path is *technically possible* (proven, not argued) but for any real app its cost equals owning
a SwiftUI→Compose runtime, which is the same work as writing Compose directly, minus a layer of
indirection and plus a dependency on upstream Skip.

**What was proven.** A **verbatim** Skip-transpiled SwiftUI screen runs, interactively, on **Compose
Multiplatform desktop, on macOS and Linux** (JVM/Skia). The witness grew from a counter to a small
real screen: `@AppStorage` persistence, `HStack`, a conditional, a `List`, and a `NavigationStack`
with a pushable detail, all rendering from the untouched transpiler output. Screenshots:
macOS [`desktop-witness.png`](./docs/desktop-witness.png) /
[`…-after-click.png`](./docs/desktop-witness-after-click.png), Linux (Docker, headless)
[`linux-witness.png`](./docs/linux-witness.png) /
[`…-after-click.png`](./docs/linux-witness-after-click.png),
persistence [`…-persisted.png`](./docs/desktop-witness-persisted.png). Runnable:
[`desktop-witness/`](./desktop-witness/).

**Why it's not "just build".** `skip export` yields a **standalone but Android-only** Gradle project
(6× `com.android.library`). On plain JVM, `SkipLib`/`SkipFoundation`/`SkipModel` compile once the stub
`android.jar` is on the classpath (the first "wall" was *classpath*, not rewrite). But **full SkipUI**
is a monolith: a **3561-line `View` interface** wired to every feature, dragging `androidx.navigation3`
(which has **no CMP artifact at all**), `LocalContext`, material3-adaptive, activity… You cannot carve
a trivial subset. So two routes exist, and both converge on Compose-first:

1. **Port the real Skip runtime to CMP**: hits its worst wall at `navigation3` + SkipFoundation's
   Android backing. **Person-months** plus tracking upstream Skip.
2. **Per-app adapter** (what this PoC built, ~300 lines total): every feature turns out toy-crossable
   on plain Compose *because the adapter bypasses Skip's Android deps*. But extended to a real app that
   adapter **is** a hand-written SwiftUI-on-Compose runtime, reimplementing Skip's job, at a fraction
   of its fidelity.

**The cost curve, measured (adapter LOC vs the real SkipUI/SkipFoundation file):**

| SwiftUI feature | Nature of the gap | Real | Adapter toy | Kept |
|---|---|---|---|---|
| `HStack`, `Text`, `Button` | plain layout primitive | - | ~12 lines each | full |
| `@AppStorage` | **Android-backed** (UserDefaults → SharedPreferences) | 894 lines | 28 lines | Int-only, persists |
| `List` | **SkipUI architecture** (LazyItemFactory) | 1334 lines | 9 lines | static rows |
| `NavigationStack` | **navigation3** (no CMP artifact) | 2326 lines | 42 lines | push/pop, no transitions |

Each toy is 1-3% of the real code; the behaviour that makes the feature real lives in the 97-99% you
drop. Primitives are cheap; every Foundation- or navigation-backed feature is its own re-implementation.

**Recommendation.** For the target device, **write Compose Multiplatform directly.** Revisit the transpiler only
if Skip upstream ships an official CMP/desktop target (discussion #163: interest, no delivery), which
would move the runtime port off our plate.

**Time-box result:** full loop (setup → e2e macOS+Linux → three cliff experiments) in **~4h**, far
inside the 3-day box. Determining go/no-go was cheap; the expensive part (a maintained runtime port)
we correctly did not start. Detail and raw evidence in the numbered steps below.

---

## Step 1: Setup (2026-07-11)

### Host toolchain (macOS Apple Silicon)

| Component | Version | Status | Note |
|---|---|---|---|
| macOS | 26.5.2 (ARM) | ✅ | |
| Xcode | 26.6 (build 17F113) | ✅ | required for Skip's SwiftUI transpilation |
| Swift | 6.3.3 | ✅ | provided by Xcode |
| Skip | 1.9.4 | ✅ | installed via Homebrew |
| Gradle | 9.6.1 | ✅ | pulled in as a dependency of the `skip` formula |
| Java (JDK) | 26.0.1 (seen by skip) / Temurin 21 (in shell PATH) | ✅ | > 17 required, both qualify |
| Android SDK | 37.0.0 | ✅ | Android Studio already installed on the host |
| Android Debug Bridge | 1.0.41 | ✅ | |
| Homebrew | 6.0.9 | ✅ | |
| Swiftly | - | ✗ | `error executing swiftly`: **non-blocking** (see below) |
| GitHub CLI (`gh`) | 2.96.0 | ✅ | |

### Commands actually run

```bash
# Skip was not installed. Homebrew 6 requires trusting the tap before installing.
brew tap skiptools/skip
brew trust skiptools/skip
brew install skip          # also pulls gradle 9.6.1, gradle-completion, swiftly

skip doctor                # full toolchain diagnostic
```

Notes:
- `skip --version` does not exist (`Error: Unknown option '--version'`). The version is reported by
  `skip doctor`.
- `gradle` was not installed standalone at first; the `skip` formula pulled it in, so no issue.

### The only red flag: swiftly

`skip doctor` reports a single error: `Swiftly version: error executing swiftly`. Swiftly is the
Swift toolchain manager. **Non-blocking for this PoC**: Swift 6.3.3 is correctly detected via
Xcode, and Skip's transpilation relies on the Xcode toolchain, not a swiftly-managed one. To be
rechecked only if a Skip command fails explicitly on it.

### Time spent

- Step 1: ~15 min (mostly installing Skip + dependencies).

---

## Step 2: Minimal witness app (2026-07-11)

### `skip init` syntax (verified, not assumed)

`skip --help` exposes both `create` (interactive) and `init` (scriptable). `init` distinguishes two
app models:

- `--transpiled-app` (**Skip Lite**): transpiles Swift/SwiftUI to **Kotlin/Jetpack Compose**. Emits
  Compose source: this is the model to inspect and retarget to CMP.
- `--native-app` (**Skip Fuse**): compiles Swift natively for Android via a bridge; does *not* emit
  Compose source.

The PoC is explicitly about *transpiled* SwiftUI and "where the emitted Compose lives", so
**`--transpiled-app`** is the right model.

Command actually run:

```bash
skip init --transpiled-app --appid=dev.skeletongamer.witness \
  --no-fastlane --no-module-tests --show-tree --no-git-repo witness-app Witness
```

Gotcha: project folder name and module name must differ (case-insensitive). `witness` + `Witness`
was rejected; used `witness-app` (folder) + `Witness` (module). Init took ~42 s.

### Generated structure

A single Swift package targeting both platforms:

- `Sources/Witness/`: shared Swift/SwiftUI (`ContentView.swift`, `ViewModel.swift`, `WitnessApp.swift`)
- `Darwin/`: Xcode project, `Info.plist`, asset catalogs, `Main.swift` (iOS entry point)
- `Android/`: Gradle project (`settings.gradle.kts`, `gradle.properties`, launcher icons)
- `Package.swift` / `Package.resolved`: SPM manifest pulling SkipUI, SkipFoundation, SkipModel…
- `Skip.env`, `Sources/Witness/Skip/skip.yml`: Skip config

### Trimming to the floor

The default template is a full sample app (TabView, List, `NavigationStack`, JSON persistence to
`applicationSupportDirectory` via `FileManager`, `@Observable` view model, `OSLog`). That is exactly
the SkipFoundation-heavy surface that would blur the CMP gap measurement. Per the PoC brief we want
the *floor*, so:

- `ContentView.swift` rewritten to a single screen: `@State` counter, `VStack { Text; Button }`.
- `ViewModel.swift` deleted (persistence / `FileManager` / `Codable`, no longer referenced).
- `WitnessApp.swift` kept as-is: platform entry points reference `WitnessRootView`, which loads
  `ContentView`.

Result: the witness app exercises SwiftUI UI primitives only (no navigation, no persistence, no
Foundation I/O).

### Time spent

- Step 2: ~15 min.

## Step 3: Nominal path (Android) (2026-07-11)

### Result: works ✅

The witness app builds, installs and runs on an Android emulator. Screenshot in
[`docs/android-witness.png`](./docs/android-witness.png): a centered `Count: 0` label and an
`Increment` button, i.e. the trimmed minimal UI.

### Commands actually run

```bash
# Boot an emulator (none was running; AVDs already existed)
emulator -avd Pixel_9_API34 -no-snapshot-save &
adb wait-for-device                       # then poll sys.boot_completed == 1

# Build + transpile + install + launch, Android only
skip app launch --android
```

Verification:

```bash
adb shell pm list packages | grep witness
# package:dev.skeletongamer.witness
adb shell dumpsys activity activities | grep ResumedActivity
# topResumedActivity=... dev.skeletongamer.witness/witness.module.MainActivity
```

First build ~5 min (Gradle downloads + Skip transpilation). Exit code 0.

### Gotchas

- `skip app launch --android` still logged **iOS** build attempts against physical devices
  (`The device is passcode protected`, `Failed to start remote service …`). Non-fatal noise, exit
  code stayed 0 and the Android build/launch succeeded.
- **Stale transpiled artifacts**: three copies of the transpiled `ContentView.kt` exist under
  `.build/` (`plugins/outputs`, `index-build`, `Darwin/DerivedData/…`). The first two were stale
  (older template); the freshest, real one is under `Darwin/DerivedData/…`. Always read the newest
  by mtime.

### Key finding: what Skip actually emits

The transpiled Kotlin (saved verbatim in
[`docs/transpiled-ContentView.kt`](./docs/transpiled-ContentView.kt)) does **not** target raw
Jetpack Compose widgets. It targets the **Skip runtime**:

```kotlin
import skip.lib.*
import skip.ui.*
import skip.foundation.*
import skip.model.*
// widgets are SkipUI types, not androidx:
VStack(spacing = 16.0) { ... Text(...); Button(...) }
```

- `VStack`, `Text`, `Button`, `View`, `ComposeBuilder`, `LocalizedStringKey` are **SkipUI**
  (`skip.ui.*`) types, not `androidx.compose.material3.*`.
- The **only** direct androidx imports are `androidx.compose.runtime.*` (`remember`,
  `mutableStateOf`, `rememberSaveable`, `Saver`), the state/runtime layer, which Compose
  Multiplatform ships as well.

**Consequence for CMP.** Retargeting is *not* about translating widgets to raw CMP. The emitted
code depends on the Skip runtime libraries, so running it on CMP desktop means making **SkipUI /
SkipFoundation / SkipModel** compile and work on the JVM/desktop target. That is exactly where the
presumed Android coupling lives, and it is the object of steps 4-5.

Skip runtime deps resolved (from `Package.resolved`): `skip-lib`, `skip-foundation`, `skip-model`,
`skip-ui`, `skip-unit`.

### Time spent

- Step 3: ~15 min (of which ~5 min first build).

## Step 4: Gradle export (`skip export`) (2026-07-11)

### Command actually run

```bash
skip export --debug --no-ios --show-tree -d ../export
```

Produced three artifacts in `export/` (git-ignored, large):

- `Witness-debug.apk` (84 MB), `Witness-debug.aab` (24 MB): Android app bundles
- `Witness-project.zip` (2.7 MB): **the standalone Gradle project sources** (the useful part)

### The exported project is a real standalone Gradle project

Unzipped, `Witness-project/Witness/` contains the **whole Skip runtime as Gradle modules** plus the
app:

```
settings.gradle.kts        # self-contained version catalog, mavenCentral() + google()
Witness/                   # the app module (transpiled ContentView.kt etc.)
SkipUI/  SkipFoundation/  SkipModel/  SkipLib/  SkipUnit/
```

Unlike the in-repo `Android/settings.gradle.kts` (which shells out to `skip plugin --prebuild` and
references Xcode `BUILT_PRODUCTS_DIR`), the exported `settings.gradle.kts` is **decoupled from Xcode
and the Skip CLI**: a plain version catalog resolving everything from Maven Central / Google. So the
export goal from discussion #163 is reached: a Gradle project that builds without Xcode. The
question is only *what target* it can build for.

### Structural blocker #1: every module is an Android Library

All 6 modules declare `plugins { alias(libs.plugins.android.library) }` and an `android { compileSdk
= 36; minSdk = 28; namespace = … }` block. They compile to **Android AARs against the Android SDK**,
not Kotlin/JVM or KMP artifacts. Compose Desktop needs KMP/JVM modules using **JetBrains Compose**
(`org.jetbrains.compose` / the multiplatform androidx artifacts). Converting `com.android.library`
→ KMP/JVM for every runtime module is the first, unavoidable, cross-cutting change.

### Android coupling, quantified (source-level, `.kt` excluding tests)

> **Correction (see step 5).** The counts below were first taken from `^import android.` /
> `^import androidx.` only. Skip's transpiler emits most platform references as **fully-qualified
> inline names** (e.g. `@androidx.annotation.Keep`, `android.icu.text.DecimalFormat`), which import
> grepping misses. The table now uses **files touching `android.*` framework** (inline included).
> The corrected figures are higher: SkipFoundation **11** files (not 4), SkipUI **29** (not 20).

| Module | .kt files | files touching `android.*` framework | `androidx.compose.*` refs | androidx non-compose | Verdict |
|---|---|---|---|---|---|
| **SkipLib** | 41 | 0 | 0 | 0 (only `annotation.Keep`) | portable (1 trivial dep) |
| **SkipFoundation** | 84 | **11** | ~1 | ~2 | substantial port (Context/ICU/prefs) |
| **SkipModel** | 10 | 2 | 7 | 0 | trivial (`Looper`) |
| **SkipUI** | 280 | **29** | **2428** | **79** | the wall |

### Reading of SkipUI (the wall): concentrated, not diffuse

Of SkipUI's 2095 androidx imports:

- **2019 (96.4%) are `androidx.compose.*`**: the Compose API, which **Compose Multiplatform mirrors
  under the same package names**. Portable at the source level (swap Google's Android Compose
  artifacts for JetBrains' multiplatform ones).
- **~76 (3.6%) are Android-only androidx**, concentrated in a handful of subsystems, not sprinkled
  everywhere:
  - `androidx.navigation3` (32): Android navigation backing `NavigationStack`. No direct CMP
    equivalent; would need reimplementation or a stub.
  - `androidx.core.view/content/app/graphics/util` (~18): Android framework wrappers.
  - `androidx.activity` + `ComponentActivity` + `activity.compose` + `activity.result` (~9): Android
    Activity entry point, permission/result plumbing.
  - `androidx.lifecycle.*` (~8): now has KMP variants; partly portable.
  - `androidx.work` / WorkManager (~6): Android-only background work.
  - `androidx.window.core` (2), `androidx.annotation` (portable).
- Plus 55 `android.*` framework imports over **20/280 files**: `Context`, `Intent`, `Bitmap`,
  `VibrationEffect` (haptics), `Settings`, `Build`, `Log`, `Looper` (main-thread checks).

`material-icons-extended` appears in the version catalog but is **not a blocker**: it is deprecated
upstream and replaced in practice by Material Symbols vector drawables, so it can simply be dropped.

`android.os.Looper` (main-thread detection) recurs across SkipUI/SkipModel/SkipFoundation, a textbook
`expect/actual` candidate.

### Classification against the PoC's error categories

- **(a) Gradle config**: convert all 6 `com.android.library` modules to KMP/JVM + JetBrains Compose.
  Large but mechanical, cross-cutting.
- **(b) Android dep to replace**: `navigation3`, `activity`/`activity-compose`, `appcompat`, `work`,
  `lifecycle-process`, `coil` (Android). Some have CMP equivalents, some need stubs.
- **(c) SkipUI non-portable API**: the ~20 files touching `android.*` framework (Context, Intent,
  Bitmap, haptics, Settings, Build, Log, Looper). Partly `expect/actual`-able (Log, Looper, haptics),
  partly harder (Activity/Context/Intent entry).
- **(d) SkipFoundation non-portable**: **11 files** backed by Android APIs: ICU i18n
  (`android.icu.text.*`, number/date formatters), `Context`/`SharedPreferences`/`PackageManager`/
  `AssetManager` (UserDefaults, Bundle, ProcessInfo), `Looper`, `android.net.Uri`, `android.util.Xml`.
  **This is the suspected main gap, and step 5 confirms it is real, not minor** (see below).

### Preliminary lean (superseded by step 5's actual compiles)

The gap is **concentrated and structured** (not sprinkled everywhere), but **substantial**:

- SkipLib is portable; SkipUI's UI is ~96% Compose API that CMP mirrors.
- **But** SkipFoundation is *not* nearly-free: 11 files backed by Android `Context`,
  `SharedPreferences`, `PackageManager` and ICU i18n. And everything above depends on it.
- The real work is: (1) re-plumbing all 6 Gradle modules from Android AAR to KMP/JVM + JetBrains
  Compose, (2) reimplementing SkipFoundation's Android backing on desktop (prefs, filesystem,
  ICU/`java.text`), and (3) desktop `expect/actual`/stubs for SkipUI's Android surfaces (navigation3,
  Activity entry, ~29 framework files).

This points away from NO-GO ("reimplementing Skip") and toward a **bounded, if substantial, port**.
Step 5 (actually compiling a module against Compose Desktop JVM) will confirm or refute this.

Saved for the record: [`docs/export-inspect/SkipUI.build.gradle.kts`](./docs/export-inspect/SkipUI.build.gradle.kts),
[`docs/export-inspect/settings.gradle.kts`](./docs/export-inspect/settings.gradle.kts).

### Time spent

- Step 4: ~25 min (of which ~5 min export build).

## Step 5: CMP desktop JVM target (macOS) (2026-07-11)

Method: instead of adding a desktop target to the Android-library modules (blocked at the plugin
level), take the **exported Kotlin sources** and compile them, module by module, on a plain
`kotlin("jvm")` target with no Android SDK. This isolates "does the Kotlin itself run off-Android"
from Gradle plumbing. Scratch projects live in `cmp-attempt/` (git-ignored).

### SkipLib → compiles on pure JVM ✅ (1 trivial fix)

```
cmp-attempt/skiplib-jvm  →  gradle compileKotlin  →  BUILD SUCCESSFUL
```

First attempt failed with `Unresolved reference 'androidx'`: 42× `@androidx.annotation.Keep`
emitted as fully-qualified inline annotations. Fix: add `androidx.annotation:annotation:1.9.1`, a
**pure KMP/JVM** artifact (`@Keep` is a no-op off-Android). After that, all 41 SkipLib files compile
with only `kotlin-reflect` + `kotlinx-coroutines-core`. **SkipLib is portable.** Category (a) plugin
swap + category (b) one trivial dep.

### SkipFoundation → does NOT compile ❌ (real reimplementation needed)

```
cmp-attempt/skipfoundation-jvm  →  gradle compileKotlin  →  BUILD FAILED, 416 errors
```

Deps added (okhttp, commonmark, kxml2 for `org.xmlpull`) resolved fine. The failures are
**Android-framework APIs with no JVM classpath equivalent**, spread over the 11 flagged files:

- **ICU i18n**: `android.icu.text.DecimalFormat`, `DecimalFormatSymbols`, `MessageFormat`,
  `RelativeDateTimeFormatter` (+ `RelativeUnit`/`AbsoluteUnit`/`Direction`), `Currency`, `ULocale`.
  Backs `NumberFormatter`, `Formatter`, `RelativeDateTimeFormatter`. Desktop equivalent exists
  (`java.text` / ICU4J `com.ibm.icu`) but under different packages/APIs → rewrite, not a swap.
- **App context / storage**: `Context.getPackageName/getPackageManager/getPackageInfo/getFilesDir/
  getContentResolver/resources`, `SharedPreferences` (`edit`, `getAll`), `AssetManager`,
  `ApplicationInfo`. Backs `UserDefaults`, `Bundle`, `ProcessInfo`. Needs a desktop backing
  (`java.util.prefs`, filesystem, classloader resources).
- **os/util/net**: `android.os.Build.*` (device info), `Looper` (main thread), `android.util.Log`,
  `android.util.Xml`, `android.net.Uri`, `android.os.Process`.

None of this is diffuse *within* those files, but it is **not optional**: SkipFoundation is a hard
dependency of SkipModel, SkipUI and the app, so nothing above it compiles on desktop until this layer
is ported. This is exactly the "SkipFoundation is the main suspect" hypothesis from the brief,
**confirmed**.

### Update: the compile "wall" was a classpath issue (`android.jar`)

Adding the Android **stub** `android.jar` (SDK `platforms/android-36/android.jar`) to the *compile*
classpath (`compileOnly`) makes **SkipFoundation compile with 0 errors**, and SkipModel too. So the
416 errors were not "must reimplement": they were "`android.*` symbols absent from the classpath".
This matches Skip's own model: it tests its runtime on the JVM via **Robolectric**, i.e. `android.*`
is expected to be provided off-device. `android.jar` is a stub (methods throw at runtime), so this
buys *compilation*, not device-free execution, but for code paths a desktop app never hits, that is
enough.

### SkipUI → the real wall, quantified

Compiling **all of SkipUI** on JVM against **JetBrains Compose desktop** (provides `androidx.compose.*`
off-Android) + `android.jar`:

- Start: 1100 errors. Adding Coil 3 (multiplatform) + JetBrains material-icons-extended → **457**.
- Remaining: `androidx.navigation3` (Android-only, no CMP equivalent), `LocalContext`/
  `LocalConfiguration`/`LocalView`, `androidx.activity`, `androidx.core.*`,
  `material3.adaptive`, `dynamicColorScheme`, `stringResource`, `asAndroidPath`, `androidx.work`.
- **Not modular by feature.** Excluding the feature files (TabView, Navigation, Image, …) *raised*
  `View.kt` errors from 68 to 264: the core `View` (a **3561-line interface**) references the feature
  types (`LocalNavigator`, `TabBarPreferenceKey`, `isViewPresented`, …). You cannot carve a trivial
  subset out of SkipUI: it is monolithic.

So porting the **real** SkipUI to desktop means shimming/porting its whole Android-Compose surface at
once: person-weeks, plus tracking upstream Skip (an estimate of that port, which was not attempted; it
is a different and heavier task than POC 5's path of riding JetBrains' existing Compose).

### Time spent

- Step 5: ~70 min (JVM compiles + `android.jar` + SkipUI enumeration).

## Step 6: Run on Compose Desktop (JVM/Skia) ✅

Rather than port all of SkipUI, provide the **thin slice the witness actually uses**. A ~200-line
`skip.ui` desktop adapter on JetBrains Compose (`View`, `ComposeBuilder`, `ComposeContext`,
`ComposeResult`, `Renderable`, `State`, `LocalizedStringKey`, `VStack`, `HStack`, `Text`, `Button`,
`.padding()`; later `AppStorage` and `List` were added, see the cliff sections) compiles the
**verbatim** Skip-transpiled `ContentView.kt` and renders it.

Result: end-to-end functional (app in [`desktop-witness/`](./desktop-witness/)):

- [`docs/desktop-witness.png`](./docs/desktop-witness.png), Count: 0, Material3 button.
- [`docs/desktop-witness-after-click.png`](./docs/desktop-witness-after-click.png), after a simulated
  click, **Count: 1**: the transpiled `@State` counter updates and Compose recomposes.

Rendered offscreen via `ImageComposeScene` (headless; `gradle renderPng`), and runnable as a real
window (`gradle run`). This is **desktop CMP on the JVM via Skia**, first verified on macOS.

### Linux run (Docker): confirmed ✅ (the brief's "juge de paix")

The same app rendered **inside a Linux (arm64) container**, headless, with Skia's software backend:
identical output: [`docs/linux-witness.png`](./docs/linux-witness.png) (Count: 0) and
[`docs/linux-witness-after-click.png`](./docs/linux-witness-after-click.png) (Count: 1 after the
simulated click). Reusable build scripts live in [`desktop-witness/docker/`](./desktop-witness/docker/)
(`Dockerfile` + `render-linux.sh`).

Two Linux-specific gotchas, both mechanical:

- **`libGL.so.1`**: skiko's native lib `libskiko-linux-arm64.so` `dlopen`s `libGL.so.1` **even in
  software render mode**. A bare `eclipse-temurin:21-jdk` image lacks it, so the JVM dies with
  `UnsatisfiedLinkError: … libGL.so.1: cannot open shared object file`. Fix: `apt-get install libgl1`
  (plus `libx11-6 libxext6 libxrender1` for AWT, `fontconfig libfreetype6 fonts-dejavu-core` so text
  renders). Env `SKIKO_RENDER_API=SOFTWARE` selects the CPU backend (no GPU/X server in the container).
- **JDK toolchain**: `kotlin { jvmToolchain(17) }` failed in the container (only JDK 21 present, no
  toolchain download repo configured): `Cannot find a Java installation … matching languageVersion=17`.
  Set `jvmToolchain(21)` (present on both host and image).

Net: the witness renders identically on macOS and Linux. The Skia/JVM (`skiko`) stack is OS-portable;
the only Linux delta is a handful of apt packages, now captured in the Dockerfile.

### Adapter growth: the cost curve, one measured point

The witness was then bumped up one notch, still primitive: `VStack { Text; HStack { Button("-");
Button("+") }; if count > 0 { Text("Positive") } }`. Re-transpiled by Skip (`swift build`) and run
on macOS + Linux (Count: 0 → click "+" → Count: 1 with "Positive" appearing, same PNGs, both OSes).

Cost of those three added SwiftUI widgets on the adapter: **+1 symbol** (`HStack`, ~12 lines; a
`Row`). The second `Button` reused the existing `Button`; the `if` conditional is **plain Kotlin** in
the transpiled output, so it costs the adapter nothing. So for **primitive layout/control widgets**
the adapter grows roughly linearly and cheaply (≈ one small function per new primitive).

The caveat is the shape of the curve, not this point: it stays cheap only while the app sticks to
primitives the adapter can map 1:1 to JetBrains Compose. The first `List`, `NavigationStack`, `Image`,
`TextField`-with-formatter or `@AppStorage` pulls in SkipUI's Android-coupled, monolithic files
(navigation3, Coil, `LocalContext`, SkipFoundation's ICU/prefs), which is not a per-symbol adapter
add but the whole-runtime port measured in step 5. **Primitive breadth is cheap; the first
Android-backed feature is the cliff.**

### The cliff, hit on purpose: `@AppStorage`

Changed the counter to `@AppStorage("count") var count = 0` (SwiftUI persistence) and re-transpiled.
The transpiler swapped `skip.ui.State<Int>` for **`skip.ui.AppStorage<Int>`**, and the minimal
adapter stopped compiling: `Unresolved reference 'AppStorage'`. That is the cliff at compile time: a
persistence property wrapper is *not* a free primitive.

What sits behind it in real Skip: `skip.ui.AppStorage` (**283 lines**) delegates to
`skip.foundation.UserDefaults` (**611 lines**), which is backed by **`android.content.SharedPreferences`
+ `Context`**. To cross the cliff on desktop you must supply that persistence yourself.

Crossed it: a desktop `AppStorage<T>` re-backed on `java.util.prefs.Preferences` (the desktop-native
equivalent of SharedPreferences). Result: **persistence works end-to-end across process restarts**:
run 1 starts at Count 0, a `+` click writes 1; run 2 (a fresh JVM) loads **Count 1** in its very first
frame with no click, [`docs/desktop-witness-persisted.png`](./docs/desktop-witness-persisted.png).
Also re-rendered on Linux.

Cost, measured honestly:

- The desktop `AppStorage` is **~28 lines**, but a **toy**: it dispatches only the 5 primitive
  types (`Int/Long/Boolean/Double/String`) the witness needs, versus Skip's **894 lines**
  (AppStorage 283 + UserDefaults 611) covering `Data`, arrays, dictionaries, `Date`, `URL`, custom
  `Codable` via serializer/deserializer, and change listeners.
- It required **choosing a desktop persistence backend** and reimplementing the type dispatch, i.e.
  the *start* of porting SkipFoundation's `UserDefaults`, not a layout function. A real app's
  `@AppStorage` over an enum/`Codable` (as Skip's own default template does:
  `AppStorage<ContentTab>(… serializer/deserializer …)`) would need the serializer path too.

So the cliff is **real but crossable per-subsystem**: each Foundation-backed feature (`@AppStorage`,
formatters, `Bundle`, `FileManager`, dates) is its own desktop re-backing. Cheap once, but they add
up, and this is exactly the whole-runtime cost from step 5, now shown feature-by-feature.

### A second, different cliff: `List` (UI side, not Foundation)

Added a `List { Text("Alpha"); Text("Bravo"); Text("Charlie") }` and re-transpiled. This cliff has a
different shape: it is **SkipUI-internal**, not Android/Foundation.

- **Compile symptom.** The transpiler emits `skip.ui.List { … }`; with no adapter `List`, Kotlin falls
  back to the stdlib `kotlin.collections.List(size, init)` and errors (`No value passed for parameter
  'size'`, lambda type mismatch). A UI container is not a free primitive either.
- **What's behind it.** Real `skip/ui/List.kt` is **1334 lines**. Its Android surface is light (only
  Compose `material`/`animation`, both in CMP), the weight is SkipUI's own model: a `LazyItemFactory`
  protocol, `EnvironmentValues` (×22), `ForEach` diffing, rows, separators, selection, swipe actions.
- **Crossed it** with a **9-line** `List` mapping to a `LazyColumn`. The three rows render on macOS and
  Linux (`Count`, `-`/`+`, `Positive`, then `Alpha`/`Bravo`/`Charlie`).

Same lesson, second axis: 9 lines vs 1334 (~0.7%). The toy renders *static* rows and drops everything
that makes `List` a list: lazy virtualization, `ForEach`-over-data, `NavigationLink` rows,
swipe-to-delete (all of which Skip's own default template uses). So the two cliffs are distinct:
`@AppStorage` is an **Android-backed** subsystem (re-back on a desktop store); `List` is a
**SkipUI-architecture** subsystem (reimplement the lazy/environment model). Both cross as toys; the
real behavior lives in the 97-99% you drop.

### The hardest cliff: `NavigationStack` (and a corrected prediction)

Added `NavigationStack { … NavigationLink("Details"){ Text("Detail for \(count)") } … }
.navigationTitle("Witness")`. This was expected to be the **first cliff not crossable as a toy**,
because real `skip/ui/Navigation.kt` (**2326 lines**) is built on **`androidx.navigation3`** (
`NavKey`, `NavBackStack`, `NavDisplay`, `entryProvider`, `Scene`, transition specs), an **Android-only
library with no Compose Multiplatform artifact at all** (unlike List's deps, which all exist in CMP).
On the *port-the-real-SkipUI* route, this is the worst wall: you would have to reimplement navigation3
itself or rewrite 2326 lines onto a different nav library.

**The prediction was wrong for the adapter route.** Because the adapter never touches navigation3, a
**42-line** hand-rolled back stack (a `SnapshotStateList<View>` in a `CompositionLocal`, push on
`NavigationLink` click, pop on Back) gives **real push/pop navigation** on plain Compose. Clicking
"Details" pushes the destination; the detail screen with a "< Back" button appears, on macOS and
Linux. 42 lines vs 2326 (~1.8%), dropping navigation3's transitions, predictive-back, deep links and
state restoration.

### Synthesis: the two routes, and why they both point to Compose-first

The cliff series (`@AppStorage`, `List`, `NavigationStack`) clarifies the real choice:

- **Port the real Skip runtime to CMP.** Hits its hardest wall exactly at `navigation3` (no CMP
  artifact), plus SkipFoundation's Android backing. Person-months + tracking upstream. 
- **Per-app adapter (what this PoC built).** *Every* feature turns out toy-crossable on plain Compose,
  because the adapter bypasses Skip's Android dependencies and reimplements only the behavior the app
  uses. But that is the catch: extended to a real app, the adapter **is** a hand-written
  SwiftUI-on-Compose runtime, which is precisely Skip's own job, minus fidelity. You converge on
  "reimplement Skip's UI layer yourself," i.e. the brief's NO-GO, reached incrementally.

Either way, for a real app you end up **owning a SwiftUI→Compose runtime** (ported or rewritten). For
the target device, writing Compose directly is the same work without the transpile indirection. **Compose-first
stands, now demonstrated from three angles rather than argued.**

## Step 7: Go/no-go decision

### Question, answered

*How much work to run a Skip-transpiled SwiftUI app on CMP desktop?* → It **works today for a
floor-level screen**; the cost scales with how much of SwiftUI/SkipUI the app uses.

### Measured evidence (recap)

- `skip export` = a real standalone Gradle project, but **Android-only** (6× `com.android.library`).
- **SkipLib / SkipFoundation / SkipModel compile on plain JVM** with `androidx.annotation` +
  `android.jar` on the classpath. The compile "wall" was classpath, not rewrite.
- **Full SkipUI** does not compile off-Android without shimming its whole Android surface
  (navigation3, activity, adaptive, `LocalContext`, …); it is a **monolithic 3561-line `View`
  interface**, not carve-able by feature.
- **The transpiled app** runs e2e on CMP desktop through a **~200-line adapter**, proven with a
  live, interactive counter.

### Against the decision criteria

- **GO** (*trivial app renders on CMP desktop with localized, understandable adjustments*) → **met for
  the floor.** The witness renders and is interactive; the adapter is small and comprehensible.
- **NO-GO** (*`android.*` diffuse and non-isolable → running the UI means reimplementing Skip*) →
  **met for a real app.** To use SkipUI's breadth (lists, navigation, images, formatters, persistence)
  you must port the real SkipUI runtime (monolithic, Android-coupled), effectively maintain a Skip
  fork.

Both criteria fire, at different app sizes. That is the finding: **feasibility is real, cost is a
function of surface area.**

### Decision

- **For a throwaway/trivial UI**: GO. The transpiled output is genuinely portable; a small adapter
  suffices, as demonstrated.
- **For the target device (a real app)**: **NO-GO on the Skip→CMP transpiler path.** The per-app adapter grows
  with every SwiftUI feature, and the alternative (porting real SkipUI + SkipFoundation's Android
  backing to KMP/JVM and tracking upstream Skip) is person-months plus standing maintenance.
  **Recommendation: Compose-first for the target device**, while noting the transpile→CMP path is *not* impossible:
  it is a cost/maintenance trade-off, newly quantified.

### Rough effort estimate (labeled estimate, not measured)

- Trivial app on a per-app adapter: **hours** (done here).
- Real app, porting the actual Skip runtime: Gradle restructure (6 modules → KMP/JVM) a few days;
  SkipFoundation desktop backing ~1-2 weeks; **SkipUI** whole-surface port several weeks; then
  continuous upstream-tracking. Order of magnitude: **person-months** + standing cost.

### What would flip the real-app case to GO

- Skip upstream officially shipping a CMP/desktop target (discussion #163 shows interest, no delivery)
  then the runtime port is *their* maintenance.
- Or an app deliberately constrained to the SkipUI slice already covered by a small adapter.

### Meta-result (the time-box is a result)

Full loop (from zero to an interactive Skip-transpiled screen on CMP desktop) in **~3h**, far inside
the 3-day box. Two methodological notes for the file: (1) the step-4 import-only coupling count
undercounted (Skip emits fully-qualified inline refs); actual compiles corrected it: **trust the
compiler, not the grep**. (2) The first "compile wall" (SkipFoundation) dissolved once `android.jar`
was on the classpath: **distinguish a classpath gap from a rewrite** before concluding.

## References

- Skip: https://skip.dev
- skiptools/skip (native SwiftUI for iOS + Android): https://github.com/skiptools/skip
- The origin of this POC, discussion "Why not Compose Multiplatform?" (`skiptools#163`):
  https://github.com/orgs/skiptools/discussions/163 . The Skip maintainer explains CMP is not a
  planned target (it paints to a Skia canvas, a sub-par iOS experience; Skip stays focused on native
  mobile), but suggests exporting to a Gradle project and testing other CMP targets independently,
  while flagging Android-dependency hurdles. This POC is that experiment: it found the Android
  coupling diffuse enough to make a clean transpiler a NO-GO.
- Compose Multiplatform: https://kotlinlang.org/compose-multiplatform/
