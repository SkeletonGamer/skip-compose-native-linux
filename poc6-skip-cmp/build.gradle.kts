import java.io.File

// POC 6 Jalon 1: compile the REAL Skip-transpiled runtime (SkipLib/SkipFoundation/SkipModel/SkipUI)
// against Compose Multiplatform Desktop (JVM), shimming the Android surface. The signal: does the
// error set converge to a bounded set of shims, or sprawl (POC 1's "diffuse" fear)?
plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    id("org.jetbrains.compose") version "1.9.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val skip = "../export/Witness-project/Witness"
// Any installed Android SDK platform provides the android.* API surface we need at compile time only.
// Pick the highest-numbered platform present, or honour an explicit ANDROID_JAR override.
val androidJar: String = System.getenv("ANDROID_JAR")
    ?: listOfNotNull(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        System.getenv("LOCALAPPDATA")?.plus("/Android/Sdk"),          // Windows
        "${System.getProperty("user.home")}/Library/Android/sdk",     // macOS
        "${System.getProperty("user.home")}/Android/Sdk",             // Linux
    )
        .map { File(it, "platforms") }
        .flatMap { it.listFiles().orEmpty().asList() }
        .filter { File(it, "android.jar").exists() }
        .maxByOrNull { it.name }
        ?.let { File(it, "android.jar").path }
    ?: error("No android.jar found. Set ANDROID_JAR (path to an android.jar), or ANDROID_HOME to an SDK with a platform installed.")

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended) // SkipUI maps SF Symbols to material icons (Icons.*)
    // Third-party libraries SkipUI/SkipFoundation use (all have JVM/CMP artifacts).
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    implementation("io.coil-kt.coil3:coil-svg:3.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:0.24.0")
    implementation(kotlin("reflect")) // SkipLib uses kotlin.reflect.full.companionObjectInstance
    // SkipFoundation reflectively loads androidx.test ApplicationProvider for a JVM Context. We fake it
    // with a Mockito Context so the render can boot without the full Robolectric runtime.
    implementation("org.mockito:mockito-core:5.14.2")
    // lifecycle has a CMP artifact (unlike navigation3).
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    // android.* framework: provide the API surface at compile time only (POC 1: this dissolved the
    // SkipFoundation "compile wall"). It throws at runtime, which is fine for the compile signal.
    compileOnly(files(androidJar))
    // navigation3 has no CMP artifact. Try the Android one compile-only, non-transitive so its compose
    // refs resolve against CMP (same package names) instead of dragging Android compose.
    // Same trick for the other Android-only androidx subsystems SkipUI touches. Non-transitive so their
    // compose refs resolve against CMP; their own classes (activity/work/core/window/adaptive) fill the gap.
    // navigation3 publishes a JVM-consumable variant (unlike core/window/activity/work/adaptive, which
    // are aar-only). Take it compile-only non-transitive; the rest is stubbed in shims/.
    listOf(
        "androidx.navigation3:navigation3-runtime:1.2.0-alpha05",
        "androidx.navigation3:navigation3-ui:1.2.0-alpha05",
    ).forEach { compileOnly(it) { isTransitive = false } }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.animation.core.ExperimentalAnimationSpecApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.animation.ExperimentalSharedTransitionApi",
        )
    }
    sourceSets["main"].kotlin.srcDir("$skip/SkipLib/src/main/kotlin")
    sourceSets["main"].kotlin.srcDir("$skip/SkipFoundation/src/main/kotlin")
    sourceSets["main"].kotlin.srcDir("$skip/SkipModel/src/main/kotlin")
    sourceSets["main"].kotlin.srcDir("$skip/SkipUI/src/main/kotlin")
    sourceSets["main"].kotlin.srcDir("$skip/Witness/src/main/kotlin") // the transpiled ContentView
    // Pure-Android feature files a minimal desktop render does not need (notifications = deep
    // NotificationCompat; AsyncImage = coil version skew). Excluded rather than deep-stubbed.
    sourceSets["main"].kotlin.exclude(
        "**/skip/ui/UserNotifications.kt",
        "**/skip/ui/UserNotificationsDelegateSupport.kt",
    )
}

// Offscreen render of the transpiled ContentView through the real SkipUI. android.jar is added at
// RUNTIME too here: SkipUI genuinely calls android.* at runtime (Context, resources...), which
// compile-only stubs do not provide.
tasks.register<JavaExec>("renderPng") {
    group = "verification"
    mainClass.set("render.RenderPngKt")
    classpath = sourceSets["main"].runtimeClasspath + files(androidJar)
}

