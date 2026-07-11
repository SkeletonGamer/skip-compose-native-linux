// Offscreen sanity render (build verification only; the decisive proof is the real-screen runs).
package app

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import java.io.File

private fun save(image: org.jetbrains.skia.Image, name: String) {
    val data = image.encodeToData() ?: error("PNG encode failed")
    val out = File("../docs/$name")
    out.writeBytes(data.bytes)
    println("WROTE ${out.absolutePath} (${out.length()} bytes)")
}

fun main() {
    val scene = ImageComposeScene(width = 520, height = 420, density = Density(2f)) { App() }
    save(scene.render(), "poc2-home.png")
    // Click "Details" (approx center of that button) to exercise navigation.
    val target = Offset(260f, 330f)
    scene.sendPointerEvent(PointerEventType.Press, target)
    scene.sendPointerEvent(PointerEventType.Release, target)
    save(scene.render(), "poc2-detail.png")
    scene.close()
}
