// Wires the Wayland IME into Compose.
//
// Jalon 10 proved the protocol loop works: the app speaks zwp_text_input_v3 to the compositor, the
// compositor relays to the input method, and preedit/commit come back. But the text went nowhere, because
// nothing fed it into the text field. This is that missing half.
//
// WHICH CONTRACT. Compose has two text-input paths, and picking the wrong one silently does nothing:
//   - the legacy PlatformTextInputService (startInput/stopInput), and
//   - the modern PlatformContext.startInputMethod(request), a suspend function returning Nothing.
// material3's TextField goes through the MODERN one. Implementing only the legacy one produced a text field
// that took focus, showed its caret, and never enabled the IME: startInput() was never called (stopInput()
// was, which is what made the mistake visible). The default startInputMethod is `awaitCancellation()`, so
// the IME simply never engages.
//
// Threading: the Wayland listeners are staticCFunction, so they capture nothing and cannot call into
// Compose. They push into a queue (ImeState); the frame loop drains it on the compose thread, which is the
// same pattern the GLFW input callbacks use.
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package waylandime

import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import kotlinx.coroutines.awaitCancellation

/**
 * Holds the active input session and turns IME events into Compose edits.
 *
 * Compose calls [startInputMethod] when a text field takes focus, and cancels that coroutine when the field
 * loses it. Enabling the Wayland text-input is what wakes the input method up, and on a phone it is also
 * what raises the virtual keyboard: the compositor sees a field wants input and activates the IME.
 */
class LinuxTextInputService {

    private var onEditCommand: ((List<EditCommand>) -> Unit)? = null

    /** Never returns: it suspends until Compose cancels the session (the field lost focus). */
    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        onEditCommand = request.onEditCommand
        println("IME: Compose started an input session (a text field took focus)")
        platform.posix.fflush(null)

        // Tell the compositor where the caret is, so a candidate window or virtual keyboard can position
        // itself. Compose gives it to us in root coordinates.
        val rect = request.focusedRectInRoot()
        if (rect != null) {
            enableWaylandTextInput(
                cursorX = rect.left.toInt(),
                cursorY = rect.top.toInt(),
                cursorW = rect.width.toInt(),
                cursorH = rect.height.toInt(),
            )
        } else {
            enableWaylandTextInput(cursorX = 0, cursorY = 0, cursorW = 0, cursorH = 0)
        }

        try {
            awaitCancellation()
        } finally {
            // The field lost focus: turn the IME off, which also drops the virtual keyboard.
            println("IME: Compose ended the input session"); platform.posix.fflush(null)
            onEditCommand = null
            disableWaylandTextInput()
        }
    }

    /**
     * Drains what the IME sent and turns it into Compose edits. Called once per frame from the mediator, on
     * the compose thread: the Wayland callbacks themselves cannot touch Compose.
     */
    fun drain() {
        val callback = onEditCommand ?: return

        // Committed text first: it is final, and it ends any composition in progress.
        for (text in ImeState.takeCommits()) {
            callback(listOf(FinishComposingTextCommand(), CommitTextCommand(AnnotatedString(text), 1)))
        }

        // Then the preedit: text still being composed, which Compose renders underlined and replaces on the
        // next update. Only the newest one matters; earlier ones are already stale.
        ImeState.takeLastPreedit()?.let { text ->
            if (text.isEmpty()) {
                callback(listOf(FinishComposingTextCommand()))
            } else {
                callback(listOf(SetComposingTextCommand(AnnotatedString(text), 1)))
            }
        }
    }
}
