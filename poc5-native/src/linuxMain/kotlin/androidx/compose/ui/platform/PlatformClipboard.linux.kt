// Linux clipboard actuals, backed by the real system clipboard.
//
// GLFW owns the window, and its clipboard API talks to the X11 CLIPBOARD selection (or wl_data_device
// under Wayland), so text copied here is visible to other applications and vice versa. Falls back to an
// in-process buffer when no window exists yet (e.g. offscreen tests).
//
// Scope: plain text only. Rich clip entries (MIME types, images) would need X11 selections or
// wl_data_device directly; ClipMetadata is reported as plain text rather than throwing.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.AnnotatedString
import glfw.glfwGetClipboardString
import glfw.glfwSetClipboardString
import kotlinx.cinterop.toKString
import linuxglfw.GlfwBridge

// The "native clipboard" handle Compose hands back to callers. On Linux the system clipboard lives in
// the window server, so this type just routes to GLFW (with an in-process fallback pre-window).
actual class NativeClipboard {
    private var fallback: String? = null

    var text: String?
        get() {
            val w = GlfwBridge.window ?: return fallback
            return glfwGetClipboardString(w)?.toKString()
        }
        set(value) {
            val w = GlfwBridge.window
            if (w == null) fallback = value else glfwSetClipboardString(w, value ?: "")
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
