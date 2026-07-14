// POC 5: actuals Linux pour les icônes de curseur. Pas de bibliothèque de curseur native
// ici (le mediator GLFW fixera le curseur via glfwSetCursor). Stubs marqueurs suffisants
// pour compiler ui:ui : chaque icône est un objet distinct identifiable.
package androidx.compose.ui.input.pointer

private data class LinuxCursor(val kind: String) : PointerIcon

internal actual val pointerIconDefault: PointerIcon = LinuxCursor("default")
internal actual val pointerIconCrosshair: PointerIcon = LinuxCursor("crosshair")
internal actual val pointerIconText: PointerIcon = LinuxCursor("text")
internal actual val pointerIconHand: PointerIcon = LinuxCursor("hand")
