// Compose Desktop entry point. Instantiates the Skip-transpiled ContentView and renders it
// through the minimal skip.ui desktop adapter.
package witness

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import skip.ui.ComposeContext
import witness.module.ContentView

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Witness: Skip → Compose Multiplatform (desktop)",
    ) {
        MaterialTheme {
            Surface {
                val context = remember { ComposeContext() }
                val root = remember { ContentView() }
                root.Compose(context)
            }
        }
    }
}
