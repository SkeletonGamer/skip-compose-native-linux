// POC 3 Jalon 1: Kotlin/Native linuxArm64 executable that renders a Skia rectangle via skiko.
// No JVM in the OUTPUT (the Kotlin/Native compiler runs on the JVM at build time only).
plugins {
    kotlin("multiplatform") version "2.3.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    linuxArm64 {
        // Jalon 3: GLFW windowing via C interop (headers + arm64 libs under native/glfw).
        compilations.getByName("main").cinterops.create("glfw") {
            defFile(project.file("native/glfw.def"))
            includeDirs(project.file("native/glfw/include"))
        }
        binaries {
            executable {
                entryPoint = "main"
                linkerOpts(
                    "-L${project.file("native/glfw/lib")}",
                    "-lglfw", "-lGL", "-lEGL",
                    // The extracted Ubuntu arm64 libs reference newer glibc symbol versions than
                    // K/N's bundled linux sysroot; defer those to runtime instead of failing the link.
                    "--allow-shlib-undefined",
                )
            }
        }
    }
    sourceSets.getByName("linuxArm64Main").dependencies {
        // Jalon 1: the official Kotlin/Native Linux arm64 Skia klib.
        implementation("org.jetbrains.skiko:skiko:0.150.0")
        // Jalon 2: Compose runtime: published for K/N linuxArm64 (unlike ui/foundation/material3).
        implementation("org.jetbrains.compose.runtime:runtime:1.9.0")
    }
}
