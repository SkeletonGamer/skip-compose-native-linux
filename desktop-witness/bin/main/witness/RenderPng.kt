// Offscreen render of the witness to a PNG (no screen-recording permission needed).
// Proves the Skip-transpiled ContentView renders through the desktop skip.ui adapter.
package witness

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import skip.ui.ComposeContext
import witness.module.ContentView
import java.io.File

private fun save(image: org.jetbrains.skia.Image, name: String) {
    val data = image.encodeToData() ?: error("PNG encode failed")
    val out = File("../docs/$name")
    out.writeBytes(data.bytes)
    println("WROTE ${out.absolutePath} (${out.length()} bytes)")
}

fun main() {
    val scene = ImageComposeScene(width = 520, height = 520, density = Density(2f)) {
        MaterialTheme {
            Surface {
                val context = ComposeContext()
                val root = ContentView()
                root.Compose(context)
            }
        }
    }
    // Frame 0: initial state.
    save(scene.render(), "desktop-witness.png")

    // Simulate a click on the "Details" NavigationLink, then re-render.
    // Proves the transpiled NavigationStack pushes the destination onto the (hand-rolled) back
    // stack and the detail screen (with a Back button) appears.
    val target = Offset(260f, 335f)
    scene.sendPointerEvent(PointerEventType.Press, target)
    scene.sendPointerEvent(PointerEventType.Release, target)
    save(scene.render(), "desktop-witness-after-click.png")

    scene.close()
}
