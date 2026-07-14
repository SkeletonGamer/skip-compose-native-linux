// The GLFW system clipboard, living in the embedder rather than inside Compose.
//
// GLFW's clipboard API talks to the X11 CLIPBOARD selection (or wl_data_device under Wayland), so text put
// here is visible to other applications and vice versa. It needs the window handle, which is precisely why it
// cannot live inside Compose: a window handle belongs to a toolkit, and Compose must not pick one. The
// embedder installs this at startup through the LinuxClipboardBackend seam; a GTK, Qt or no-window-manager
// embedder installs its own, or none at all.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package linuxglfw

import androidx.compose.ui.platform.LinuxClipboardBackend
import glfw.glfwGetClipboardString
import glfw.glfwSetClipboardString
import kotlinx.cinterop.toKString

class GlfwClipboardBackend : LinuxClipboardBackend {
    override fun getText(): String? {
        val w = GlfwBridge.window ?: return null
        return glfwGetClipboardString(w)?.toKString()
    }

    override fun setText(text: String?) {
        val w = GlfwBridge.window ?: return
        glfwSetClipboardString(w, text ?: "")
    }
}
