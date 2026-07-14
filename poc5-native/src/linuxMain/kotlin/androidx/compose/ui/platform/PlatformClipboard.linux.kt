// POC 5: actuals Linux du presse-papiers. Implémentation en mémoire (le mediator réel
// branchera X11/Wayland). Suffit à compiler ui:ui et à faire tourner l'app témoin.
package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.AnnotatedString

// Presse-papiers natif : ici un simple porteur en mémoire (pas de NSPasteboard).
actual class NativeClipboard {
    var text: String? = null
}

private val sharedNativeClipboard = NativeClipboard()

@Suppress("DEPRECATION")
private class LinuxPlatformClipboardManager : ClipboardManager {
    override fun getText(): AnnotatedString? = sharedNativeClipboard.text?.let { AnnotatedString(it) }
    override fun setText(annotatedString: AnnotatedString) { sharedNativeClipboard.text = annotatedString.text }
    override fun hasText(): Boolean = !sharedNativeClipboard.text.isNullOrEmpty()
    override fun getClip(): ClipEntry? = null
    @Suppress("GetterSetterNames")
    override fun setClip(clipEntry: ClipEntry?) = Unit
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
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata non implémenté (POC). Utiliser nativeClipboard.")

    internal var plainText: String? = null

    @ExperimentalComposeUiApi
    fun getPlainText(): String? = plainText

    companion object {
        @ExperimentalComposeUiApi
        fun withPlainText(text: String): ClipEntry = ClipEntry().apply { plainText = text }
    }
}
