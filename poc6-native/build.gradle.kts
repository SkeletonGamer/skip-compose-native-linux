// POC 6 Jalon 4: SkipLib + SkipFoundation + SkipModel + SkipUI on Kotlin/Native linuxArm64, no JVM.
// SkipLib/Foundation/Model reached green via compile-only java.* shims (see shims/). SkipUI needs the
// real compose.ui/foundation/material3 stack, which is not published for K/N Linux, so (like POC 5) it is
// compiled from a source checkout of compose-multiplatform-core (jb-main), against the published K/N klibs.
plugins {
    kotlin("multiplatform") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
}

repositories {
    mavenCentral()
    google()
    // Compose K/N Linux klibs only exist at leading-edge (alpha/beta) versions on this dev repo.
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

// Local checkout of JetBrains/compose-multiplatform-core (branch jb-main), same as POC 5.
// Provide it via -PcmcRoot=/abs/path, the CMC_ROOT env var, or a ./.cmc checkout (git-ignored).
val cmcRoot: String = (project.findProperty("cmcRoot") as String?)
    ?: System.getenv("CMC_ROOT")
    ?: "$rootDir/.cmc"
val cmc = "$cmcRoot/compose/ui"
// Skip's transpiled Kotlin output, obtained via `skip export` (not committed, see the README).
val skip = "../export/Witness-project/Witness"

fun org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.srcs(vararg rel: String) =
    rel.forEach { kotlin.srcDir("$cmc/$it/kotlin") }
fun org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.srcsRoot(vararg rel: String) =
    rel.forEach { kotlin.srcDir("$cmcRoot/$it/kotlin") }

kotlin {
    linuxArm64 {
        compilations.getByName("main").cinterops.create("glfw") {
            defFile(project.file("native/glfw.def"))
            includeDirs(project.file("native/glfw/include"))
        }
        binaries {
            executable {
                entryPoint = "main"
                linkerOpts(
                    "-L${project.file("native/glfw/lib")}",
                    "-lglfw", "-lGL", "-lEGL", "-lfontconfig", "-lfreetype",
                    "--allow-shlib-undefined",
                )
            }
        }
    }

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
            optIn("androidx.compose.material.ExperimentalMaterialApi")
            optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
            optIn("androidx.compose.material3.InternalMaterial3Api")
            optIn("androidx.graphics.shapes.ExperimentalGraphicsShapesApi")
            optIn("kotlin.contracts.ExperimentalContracts")
            optIn("kotlin.experimental.ExperimentalNativeApi")
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // Real jb-main hierarchy: commonMain -> skikoMain -> nonJvmMain -> linuxArm64Main.
    val commonMain = sourceSets.getByName("commonMain")
    val skikoMain = sourceSets.create("skikoMain")
    val nonJvmMain = sourceSets.create("nonJvmMain")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
    skikoMain.dependsOn(commonMain)
    nonJvmMain.dependsOn(skikoMain)
    linuxArm64Main.dependsOn(nonJvmMain)

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
        srcsRoot(
            "compose/runtime/runtime-retain/src/commonMain",
            "navigationevent/navigationevent/src/commonMain",
            "navigationevent/navigationevent-compose/src/commonMain",
            "compose/animation/animation-core/src/commonMain",
            "compose/animation/animation/src/commonMain",
            "compose/foundation/foundation-layout/src/commonMain",
            "compose/foundation/foundation/src/commonMain",
            "graphics/graphics-shapes/src/commonMain",
            "compose/material/material-ripple/src/commonMain",
            "compose/material3/material3/src/commonMain",
        )
        dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.12.0-beta01")
            implementation("org.jetbrains.compose.runtime:runtime-saveable:1.12.0-beta01")
            implementation("org.jetbrains.compose.collection-internal:collection:1.12.0-alpha02")
            implementation("org.jetbrains.compose.annotation-internal:annotation:1.12.0-alpha02")
            implementation("org.jetbrains.skiko:skiko:0.150.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0-rc01")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0-rc01")
            implementation("org.jetbrains.androidx.savedstate:savedstate:1.3.0-alpha06")
            implementation("org.jetbrains.androidx.savedstate:savedstate-compose:1.4.0")
            // Skip runtime deps (java.* substitutes provided by shims/).
            implementation("org.jetbrains.kotlinx:atomicfu:0.26.0")
            implementation("com.ionspin.kotlin:bignum:0.3.10")        }
    }

    nonJvmMain.srcs(
        "ui-util/src/nonJvmMain",
        "ui-unit/src/nonAndroidMain", "ui-unit/src/nonJvmMain",
        "ui-graphics/src/nonJvmMain",
        "ui-text/src/nonJvmMain",
        "ui/src/nonJvmMain",
    )
    nonJvmMain.srcs(
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

    linuxArm64Main.srcs(
        "ui-graphics/src/skikoExcludingWebMain",
        "ui-graphics/src/nativeMain",
        "ui-text/src/nativeMain",
        "ui/src/nativeMain",
    )
    linuxArm64Main.srcsRoot(
        "navigationevent/navigationevent/src/nativeMain",
        "compose/foundation/foundation/src/nativeMain",
        "compose/material3/material3/src/nativeMain",
    )
    linuxArm64Main.kotlin.exclude("**/platform/NativeFont.native.kt")
    skikoMain.kotlin.exclude("**/Actuals.skiko.kt")
    nonJvmMain.kotlin.exclude("**/ui/Actuals.nonJvm.kt")

    // The transpiled Skip runtime + UI live in the leaf source set (they use the java.* shims there).
    linuxArm64Main.kotlin.srcDir("$skip/SkipLib/src/main/kotlin")
    linuxArm64Main.kotlin.srcDir("$skip/SkipFoundation/src/main/kotlin")
    linuxArm64Main.kotlin.srcDir("$skip/SkipModel/src/main/kotlin")
    linuxArm64Main.kotlin.srcDir("$skip/SkipUI/src/main/kotlin")
    linuxArm64Main.kotlin.srcDir("$skip/Witness/src/main/kotlin")
    // Pure-Android leaf files a K/N compile does not need.
    linuxArm64Main.kotlin.exclude(
        "**/skip/ui/UserNotifications.kt",
        "**/skip/ui/UserNotificationsDelegateSupport.kt",
    )
}
