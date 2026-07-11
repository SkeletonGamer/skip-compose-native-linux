// Jalon 1: draw a Skia rectangle to a raster surface and write a PNG, entirely in Kotlin/Native
// (no JVM at runtime). Proves the skiko Linux arm64 K/N klib actually builds, links and renders.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

// Jalon 2: exercise the Compose runtime's snapshot state (mutableStateOf + derivedStateOf) in
// Kotlin/Native Linux: the reactive core of Compose, running with no JVM.
fun composeRuntimeCheck() {
    val count = mutableStateOf(0)
    val doubled = derivedStateOf { count.value * 2 }
    var observed = -1
    Snapshot.withMutableSnapshot { count.value = 21 }
    observed = doubled.value
    println("compose.runtime in K/N Linux: count=${count.value} derived(doubled)=$observed")
}

fun main() {
    composeRuntimeCheck()

    val width = 400
    val height = 300

    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    canvas.clear(Color.WHITE)

    val paint = Paint().apply { color = Color.makeRGB(90, 60, 180) }
    canvas.drawRect(Rect.makeXYWH(50f, 50f, 300f, 200f), paint)

    val image = surface.makeImageSnapshot()
    val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encode failed")
    val bytes = data.bytes

    val path = "/out/skia-knative.png"
    val f = fopen(path, "wb") ?: error("cannot open $path")
    bytes.usePinned { fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
    fclose(f)

    println("WROTE $path (${bytes.size} bytes): Skia rectangle rendered in Kotlin/Native, no JVM")

    // Jalon 3 windowing brick (needs a display server; no-op if none).
    renderInWindow()
}
