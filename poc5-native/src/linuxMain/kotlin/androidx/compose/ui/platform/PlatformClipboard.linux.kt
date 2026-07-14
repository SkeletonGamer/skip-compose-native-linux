// Linux clipboard actual, with NO window toolkit in it.
//
// This file used to call glfwGetClipboardString/glfwSetClipboardString directly, and that one choice was
// enough to make the whole Compose stack un-embeddable anywhere else. Jake Wharton predicted exactly this on
// the Kotlin Slack: "If you actualize to GTK then it would be impossible to use for Qt [...] unless your
// actuals on Linux are themselves an abstraction that can be swapped out."
//
// He was right, and the linker proved it: building the GTK embedder (src/linuxMain/kotlin/gtkmain/) without
// -lglfw failed on precisely two undefined symbols, glfwGetClipboardString and glfwSetClipboardString, out of
// the whole compose.ui/foundation/material3 surface and all 42 Linux actuals. Nothing else in Compose was
// tied to the toolkit.
//
// So the clipboard becomes the swappable abstraction he asks for: Compose keeps an in-process clipboard,
// which is the honest default for an embedded device with no window server at all, and the embedder installs
// the system clipboard it owns. The SAME Compose klib now links into a GLFW app and a GTK app, and only the
// GLFW one links GLFW.
//
// Scope: plain text only. Rich clip entries (MIME types, images) would need X11 selections or wl_data_device
// directly.
package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.AnnotatedString

/**
 * The seam an embedder fills to reach the real system clipboard.
 *
 * The system clipboard on Linux lives in the display server and is reached through whatever toolkit owns the
 * window (GLFW, GTK, Qt, raw Wayland...). Compose must not pick one, so it declares the shape and lets the
 * embedder supply it. Upstream this belongs on [PlatformContext], next to `textInputService` and
 * `setPointerIcon()`, which are already injected in exactly this way.
 */
interface LinuxClipboardBackend {
    fun getText(): String?

    fun setText(text: String?)
}

// The "native clipboard" handle Compose hands back to callers. With no backend installed it is an in-process
// buffer: copy and paste work within the app, which is all a device with no window server can offer anyway.
actual class NativeClipboard {
    private var inProcess: String? = null

    var text: String?
        get() = backend?.getText() ?: inProcess
        set(value) {
            val b = backend
            if (b == null) inProcess = value else b.setText(value)
        }

    companion object {
        /** Installed once by the embedder at startup. Null means "no window system": stay in-process. */
        var backend: LinuxClipboardBackend? = null
    }
}

private val sharedNativeClipboard = NativeClipboard()

@Suppress("DEPRECATION")
private class LinuxPlatformClipboardManager : ClipboardManager {
    override fun getText(): AnnotatedString? = sharedNativeClipboard.text?.let { AnnotatedString(it) }
    override fun setText(annotatedString: AnnotatedString) { sharedNativeClipboard.text = annotatedString.text }
    override fun hasText(): Boolean = !sharedNativeClipboard.text.isNullOrEmpty()
    override fun getClip(): ClipEntry? =
        sharedNativeClipboard.text?.takeIf { it.isNotEmpty() }?.let { ClipEntry.withPlainText(it) }

    @Suppress("GetterSetterNames")
    override fun setClip(clipEntry: ClipEntry?) { sharedNativeClipboard.text = clipEntry?.plainText }
}

private class LinuxPlatformClipboard : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? =
        sharedNativeClipboard.text?.takeIf { it.isNotEmpty() }?.let { ClipEntry.withPlainText(it) }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        sharedNativeClipboard.text = clipEntry?.plainText
    }

    override val nativeClipboard: NativeClipboard get() = sharedNativeClipboard
}

@Suppress("DEPRECATION")
internal actual fun createPlatformClipboardManager(): ClipboardManager = LinuxPlatformClipboardManager()

internal actual fun createPlatformClipboard(): Clipboard = LinuxPlatformClipboard()

actual class ClipEntry internal constructor() {
    // Not implementable: skiko declares `actual class ClipMetadata private constructor()` with no
    // factory, so no instance can be built from here. Upstream macOS throws the same way, and nothing
    // in Compose itself reads this (only a demo does). Tracked upstream as CMP-1260.
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata has no public constructor upstream (CMP-1260). Use nativeClipboard.")

    internal var plainText: String? = null

    @ExperimentalComposeUiApi
    fun getPlainText(): String? = plainText

    companion object {
        @ExperimentalComposeUiApi
        fun withPlainText(text: String): ClipEntry = ClipEntry().apply { plainText = text }
    }
}
