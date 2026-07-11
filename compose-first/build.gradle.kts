// POC 2: Compose Multiplatform desktop app, 100% Kotlin/Compose (no Skip, no SwiftUI).
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
    implementation(compose.material3)
    // Navigation with a CONFIRMED Compose Multiplatform artifact (verified on Maven Central,
    // stable 2.9.2). navigation3 (used by Skip/SkipUI) has no CMP artifact: see POC 1.
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "app.MainKt"
        nativeDistributions {
            packageName = "compose-first"
            packageVersion = "1.0.0"
        }
    }
}

// Offscreen render task for a quick sanity image (NOT a substitute for the real-screen runs).
tasks.register<JavaExec>("renderPng") {
    group = "verification"
    mainClass.set("app.RenderPngKt")
    classpath = sourceSets["main"].runtimeClasspath
}
