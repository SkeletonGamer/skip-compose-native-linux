// POC 4: a real Compose composition (compiler plugin + runtime) rendered by skiko into a GLFW
// window on Kotlin/Native Linux, no JVM. A minimal `ui-glfw` proof.
plugins {
    kotlin("multiplatform") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

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
    sourceSets.getByName("linuxArm64Main").dependencies {
        implementation("org.jetbrains.compose.runtime:runtime:1.9.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        implementation("org.jetbrains.skiko:skiko:0.150.0")
    }
}
