// Bridge between the GLFW mediator (main.kt) and the GLFW-specific pieces of the embedder.
//
// GLFW's cursor API needs the window handle, so the mediator publishes it here once at startup. This POC
// drives a single window, so a single global handle is the whole story.
//
// Note what is NOT here any more: the clipboard (it moved to GlfwClipboardBackend, installed into Compose
// through a toolkit-neutral seam) and the cursor SHAPE enum (it moved into Compose, where it belongs, as
// LinuxCursorShape). Compose now names no toolkit at all.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package linuxglfw

import androidx.compose.ui.input.pointer.LinuxCursorShape
import cnames.structs.GLFWcursor
import cnames.structs.GLFWwindow
import kotlinx.cinterop.CPointer

object GlfwBridge {
    /** The mediator's window. Null until the mediator has created it. */
    var window: CPointer<GLFWwindow>? = null

    /** Standard cursors, created once by the mediator (GLFW must be initialized first). */
    var cursors: Map<LinuxCursorShape, CPointer<GLFWcursor>?> = emptyMap()
}
