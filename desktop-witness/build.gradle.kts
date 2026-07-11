// Compose Desktop app rendering the REAL Skip-transpiled ContentView.kt on a minimal
// skip.ui desktop adapter. This is the e2e artifact for the PoC.
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

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
}

kotlin {
    // JDK 21 is available on both the macOS host and the Linux container image.
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "witness.MainKt"
    }
}

// Offscreen PNG render task for headless verification.
tasks.register<JavaExec>("renderPng") {
    group = "verification"
    mainClass.set("witness.RenderPngKt")
    classpath = sourceSets["main"].runtimeClasspath
}
