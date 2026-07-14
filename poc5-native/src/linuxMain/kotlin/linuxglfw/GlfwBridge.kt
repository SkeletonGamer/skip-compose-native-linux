// Bridge between the GLFW mediator (main.kt) and the Compose platform actuals.
//
// Some actuals (clipboard, cursors) have to talk to the window system, but they are compiled inside the
// compose stack and cannot reach the mediator's locals. GLFW's clipboard and cursor APIs both need the
// window handle, so the mediator publishes it here once at startup. This POC drives a single window, so
// a single global handle is the whole story.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package linuxglfw

import cnames.structs.GLFWcursor
import cnames.structs.GLFWwindow
import kotlinx.cinterop.CPointer

object GlfwBridge {
    /** The mediator's window. Null until the mediator has created it. */
    var window: CPointer<GLFWwindow>? = null

    /** Standard cursors, created once by the mediator (GLFW must be initialized first). */
    var cursors: Map<LinuxCursorKind, CPointer<GLFWcursor>?> = emptyMap()
}

/** The cursor shapes Compose asks for. The mediator maps these to GLFW_*_CURSOR. */
enum class LinuxCursorKind { Default, Crosshair, Text, Hand }
