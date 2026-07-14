// Linux cursor actuals, with no window toolkit in them.
//
// Each icon carries only the SHAPE Compose asks for. Turning a shape into a real cursor needs the window, so
// that job belongs to the embedder: it arrives through PlatformContext.setPointerIcon(), an interface method
// that already exists upstream with a no-op default. The GLFW mediator calls glfwSetCursor there; a GTK, Qt
// or no-window-manager embedder does its own thing, or nothing at all.
//
// The shape enum used to live in the mediator's `linuxglfw` package, which left Compose naming a toolkit it
// does not use. It is a plain value type, so it belongs here.
package androidx.compose.ui.input.pointer

/** The cursor shapes Compose asks for. An embedder maps these to whatever its own toolkit calls them. */
enum class LinuxCursorShape { Default, Crosshair, Text, Hand }

internal data class LinuxCursor(val shape: LinuxCursorShape) : PointerIcon

internal actual val pointerIconDefault: PointerIcon = LinuxCursor(LinuxCursorShape.Default)
internal actual val pointerIconCrosshair: PointerIcon = LinuxCursor(LinuxCursorShape.Crosshair)
internal actual val pointerIconText: PointerIcon = LinuxCursor(LinuxCursorShape.Text)
internal actual val pointerIconHand: PointerIcon = LinuxCursor(LinuxCursorShape.Hand)
