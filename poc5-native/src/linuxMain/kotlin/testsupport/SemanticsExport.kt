// Exports the position of every tagged widget, so tests can click by name instead of by pixel.
//
// The input tests drive the app with real X11 events, which means they need screen coordinates. Hardcoding
// them makes the tests break whenever the UI gains a line (it happened: adding two Text rows shifted every
// button by ~45px and the tests silently clicked on labels). Instead the mediator walks Compose's own
// semantics tree, the one Modifier.testTag() writes into, and dumps each tag with its real bounds. The
// test reads that file and aims at the centre of the tag it wants.
//
// The same tree is what an AT-SPI accessibility bridge would consume, so this is not throwaway plumbing.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package testsupport

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

object SemanticsExport {
    /** Set by the mediator when Compose attaches a semantics owner. */
    var owner: SemanticsOwner? = null

    /**
     * Writes one line per testTag: `tag x y width height`, in window coordinates.
     *
     * Deliberately not JSON: the test image would then need a JSON parser, and python3-minimal ships
     * without the `json` module (which failed silently and sent every click to the middle of the screen).
     * A space-separated line is read by awk, which is always there.
     */
    fun dump(path: String) {
        val root = owner?.unmergedRootSemanticsNode ?: return
        val lines = mutableListOf<String>()
        collect(root, lines)
        if (lines.isEmpty()) return

        val text = lines.joinToString("\n", postfix = "\n")
        val bytes = text.encodeToByteArray()
        val f = fopen(path, "wb") ?: return
        bytes.usePinned { fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
        fclose(f)
    }

    private fun collect(node: SemanticsNode, out: MutableList<String>) {
        node.config.getOrNull(SemanticsProperties.TestTag)?.let { tag ->
            val b = node.boundsInWindow
            out.add("$tag ${b.left.toInt()} ${b.top.toInt()} ${b.width.toInt()} ${b.height.toInt()}")
        }
        for (child in node.children) collect(child, out)
    }
}
