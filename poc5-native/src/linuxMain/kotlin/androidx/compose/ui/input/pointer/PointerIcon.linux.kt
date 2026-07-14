// Linux cursor actuals. Each icon carries the shape Compose asks for; the GLFW mediator turns it into a
// real cursor via glfwSetCursor (see PlatformContext.setPointerIcon in main.kt). Keeping the GLFW call in
// the mediator rather than here lets these stay comparable value objects.
package androidx.compose.ui.input.pointer

import linuxglfw.LinuxCursorKind

internal data class LinuxCursor(val kind: LinuxCursorKind) : PointerIcon

internal actual val pointerIconDefault: PointerIcon = LinuxCursor(LinuxCursorKind.Default)
internal actual val pointerIconCrosshair: PointerIcon = LinuxCursor(LinuxCursorKind.Crosshair)
internal actual val pointerIconText: PointerIcon = LinuxCursor(LinuxCursorKind.Text)
internal actual val pointerIconHand: PointerIcon = LinuxCursor(LinuxCursorKind.Hand)
