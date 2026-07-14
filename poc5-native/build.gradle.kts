// POC 5: compile the real JetBrains compose.ui stack for Kotlin/Native Linux from source,
// with the proper KMP hierarchy: commonMain -> nonJvmMain -> skikoMain -> linuxMain -> linux{Arm64,X64}Main.
// Both Linux architectures share linuxMain: nothing in the compose stack is arch-specific.
// skiko pinned to the version jb-main targets (0.150.1), which publishes linuxArm64 AND linuxX64.
plugins {
    kotlin("multiplatform") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

// Path to a local checkout of JetBrains/compose-multiplatform-core (branch jb-main).
// Provide it via -PcmcRoot=/abs/path, the CMC_ROOT env var, or a ./.cmc checkout (git-ignored).
// See the README "Reproduce" section for the exact clone command.
val cmcRoot: String = (project.findProperty("cmcRoot") as String?)
    ?: System.getenv("CMC_ROOT")
    ?: "$rootDir/.cmc"
val cmc = "$cmcRoot/compose/ui"

fun org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.srcs(vararg rel: String) =
    rel.forEach { kotlin.srcDir("$cmc/$it/kotlin") }
fun org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.srcsRoot(vararg rel: String) =
    rel.forEach { kotlin.srcDir("$cmcRoot/$it/kotlin") }

kotlin {
    // Jalon 4: same module compiles the compose.ui source AND the ui-glfw mediator, so the mediator
    // can reach compose's `internal` API (WindowInfoImpl, PlatformContext.Empty, ...) directly.
    // The GLFW cinterop is header-only (see native/glfw.def), so it commonizes across both Linux
    // architectures; only linking needs the per-arch .so files.
    // libDir is the ONLY architecture-dependent input: the per-arch .so files to link against
    // (fetch-deps.sh stages them). The Kotlin sources are identical for both architectures.
    fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.linuxTarget(libDir: String) {
        compilations.getByName("main").cinterops.create("glfw") {
            defFile(project.file("native/glfw.def"))
            includeDirs(project.file("native/glfw/include"))
        }
        binaries {
            executable {
                entryPoint = "main"
                // GLESv2, not GL. Desktop libGL carries GLX, so it drags libX11 in, and that was the
                // binary's LAST tie to X11 (GLFW itself dlopens its backends and links neither). The app
                // needs no GLX at all: it resolves GL through EGL, and all 105 gl* symbols it references
                // are provided by libGLESv2. Dropping -lGL makes the binary run on a Wayland-only system
                // that ships no libX11.
                linkerOpts(
                    "-L${project.file(libDir)}",
                    "-lglfw", "-lGLESv2", "-lEGL", "-lfontconfig", "-lfreetype",
                    "--allow-shlib-undefined",
                )
            }
        }
    }
    linuxArm64 { linuxTarget("native/glfw/lib") }
    linuxX64 { linuxTarget("native/glfw/lib-x64") }

    // The real compose-ui build opts in globally to these (cross-module internal/experimental APIs).
    sourceSets.all {
        languageSettings.apply {
            optIn("androidx.compose.ui.InternalComposeUiApi")
            optIn("androidx.compose.ui.ExperimentalComposeUiApi")
            optIn("androidx.compose.runtime.InternalComposeApi")
            optIn("androidx.compose.runtime.ExperimentalComposeApi")
            optIn("androidx.compose.runtime.ExperimentalComposeRuntimeApi")
            optIn("androidx.compose.ui.text.ExperimentalTextApi")
            optIn("androidx.compose.ui.text.InternalTextApi")
            optIn("androidx.compose.ui.graphics.ExperimentalGraphicsApi")
            optIn("androidx.compose.foundation.ExperimentalFoundationApi")
            optIn("androidx.compose.foundation.InternalFoundationApi")
            optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
            optIn("androidx.compose.animation.ExperimentalAnimationApi")
            optIn("androidx.compose.animation.core.ExperimentalAnimationSpecApi")
            optIn("androidx.compose.animation.core.InternalAnimationApi")
            optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
            optIn("androidx.compose.material3.InternalMaterial3Api")
            optIn("androidx.graphics.shapes.ExperimentalGraphicsShapesApi")
            optIn("kotlin.contracts.ExperimentalContracts")
            optIn("kotlin.experimental.ExperimentalNativeApi")
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // Real jb-main hierarchy: commonMain -> skikoMain -> nonJvmMain -> linuxMain -> linux{Arm64,X64}Main.
    // linuxMain holds the whole Linux actual surface; the leaf source sets stay empty, which is the
    // claim under test: the compose stack is arch-neutral (upstream PR #2027 assumes the same).
    val commonMain = sourceSets.getByName("commonMain")
    val skikoMain = sourceSets.create("skikoMain")
    val nonJvmMain = sourceSets.create("nonJvmMain")
    val linuxMain = sourceSets.create("linuxMain")
    skikoMain.dependsOn(commonMain)
    nonJvmMain.dependsOn(skikoMain)
    linuxMain.dependsOn(nonJvmMain)
    sourceSets.getByName("linuxArm64Main").dependsOn(linuxMain)
    sourceSets.getByName("linuxX64Main").dependsOn(linuxMain)

    commonMain.apply {
        srcs(
            "ui-util/src/commonMain",
            "ui-geometry/src/commonMain",
            "ui-unit/src/commonMain",
            "ui-graphics/src/commonMain",
            "ui-text/src/commonMain",
            "ui-backhandler/src/commonMain",
            "ui/src/commonMain",
        )
        // Extend A2 with the modules that aren't published for linux K/N (retain + navigationevent).
        srcsRoot(
            "compose/runtime/runtime-retain/src/commonMain",
            "navigationevent/navigationevent/src/commonMain",
            "navigationevent/navigationevent-compose/src/commonMain",
            // Jalon 3: foundation + material3 chain (none published for linux K/N).
            "compose/animation/animation-core/src/commonMain",
            "compose/animation/animation/src/commonMain",
            "compose/foundation/foundation-layout/src/commonMain",
            "compose/foundation/foundation/src/commonMain",
            "graphics/graphics-shapes/src/commonMain",
            "compose/material/material-ripple/src/commonMain",
            "compose/material3/material3/src/commonMain",
        )
        dependencies {
            // Bumped to the latest published linux-K/N versions to match jb-main HEAD's API usage.
            implementation("org.jetbrains.compose.runtime:runtime:1.12.0-beta01")
            implementation("org.jetbrains.compose.runtime:runtime-saveable:1.12.0-beta01")
            implementation("org.jetbrains.compose.collection-internal:collection:1.12.0-alpha02")
            implementation("org.jetbrains.compose.annotation-internal:annotation:1.12.0-alpha02")
            implementation("org.jetbrains.skiko:skiko:0.150.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1") // material3 DatePicker
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") // mediator runBlocking/loop
            // linux-K/N artifacts for lifecycle only start at 2.11.0-rc01 (recent rollout).
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0-rc01")
            implementation("org.jetbrains.androidx.savedstate:savedstate:1.3.0-alpha06")
            implementation("org.jetbrains.androidx.savedstate:savedstate-compose:1.4.0")
        }
    }

    nonJvmMain.srcs(
        "ui-util/src/nonJvmMain",
        "ui-unit/src/nonAndroidMain", "ui-unit/src/nonJvmMain",
        "ui-graphics/src/nonJvmMain",
        "ui-text/src/nonJvmMain",
        "ui/src/nonJvmMain",
    )
    nonJvmMain.srcs(
        // jbMain = JetBrains shared (non-Android); provides BackHandler/PredictiveBackHandler/BackEventCompat.
        "ui-backhandler/src/jbMain",
    )
    nonJvmMain.srcsRoot(
        "compose/runtime/runtime-retain/src/nonJvmMain",
        "navigationevent/navigationevent-compose/src/nonAndroidMain",
        "compose/animation/animation-core/src/nonJvmMain",
        "compose/animation/animation/src/nonJvmMain",
        "compose/animation/animation/src/nonAndroidMain",
        "compose/material/material-ripple/src/nonAndroidMain",
        "compose/foundation/foundation-layout/src/nonJvmMain",
        "compose/foundation/foundation/src/nonJvmMain",
        "graphics/graphics-shapes/src/nonJvmMain",
        "compose/material3/material3/src/nonJvmMain",
    )

    skikoMain.srcs(
        "ui-graphics/src/skikoMain",
        "ui-text/src/skikoMain",
        "ui/src/skikoMain",
    )
    skikoMain.srcsRoot(
        "compose/foundation/foundation-layout/src/skikoMain",
        "compose/foundation/foundation/src/skikoMain",
        "compose/material3/material3/src/skikoMain",
    )

    // Native (Linux K/N) actuals: incl. skikoExcludingWeb (skiko desktop+native, not web).
    linuxMain.srcs(
        "ui-graphics/src/skikoExcludingWebMain",
        "ui-graphics/src/nativeMain",
        "ui-text/src/nativeMain",
        "ui/src/nativeMain",
    )
    linuxMain.srcsRoot(
        "navigationevent/navigationevent/src/nativeMain",
        "compose/foundation/foundation/src/nativeMain",
        "compose/material3/material3/src/nativeMain",
    )
    // Use our skiko-version-shimmed NativeFont instead of the HEAD original (see NativeFontPatched.kt).
    linuxMain.kotlin.exclude("**/platform/NativeFont.native.kt")
    // The skiko postDelayed actual uses Dispatchers.Main (absent on K/N Linux). Replace it with a
    // frame-loop-drained version (see LinuxPostDelayed.kt). linux is the only target, so excluding it
    // from skikoMain is safe.
    skikoMain.kotlin.exclude("**/Actuals.skiko.kt")
    // Actuals.nonJvm.kt carries the now-orphaned PostDelayedDispatcher actual (its expect lived in the
    // excluded Actuals.skiko.kt); its other two actuals are re-provided in LinuxActualsMisc.kt.
    nonJvmMain.kotlin.exclude("**/ui/Actuals.nonJvm.kt") // NOT ui/platform/Actuals.nonJvm.kt (nativeClass)
}
