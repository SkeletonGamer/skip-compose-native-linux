// POC 6 render: instantiate the Skip-transpiled ContentView and render it through the REAL SkipUI on
// CMP Desktop, offscreen to a PNG. Proves (or exposes) the runtime side of de-Android-ifying SkipUI.
package render

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import skip.ui.ComposeContext
import witness.module.MinimalContentView
import java.io.File

fun main() {
    val scene = ImageComposeScene(width = 420, height = 640, density = Density(2f)) {
        MaterialTheme {
            Surface {
                val context = ComposeContext()
                MinimalContentView().Compose(context)
            }
        }
    }
    val image = scene.render()
    val data = image.encodeToData() ?: error("PNG encode failed")
    val out = File("out/poc6-skipui-render.png")
    out.parentFile.mkdirs()
    out.writeBytes(data.bytes)
    println("WROTE ${out.absolutePath} (${out.length()} bytes)")
    scene.close()
}
