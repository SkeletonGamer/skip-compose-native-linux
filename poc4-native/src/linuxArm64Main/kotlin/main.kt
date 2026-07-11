// POC 4: a minimal `ui-glfw`: a real Compose composition (compiler plugin + Recomposer + a custom
// Applier building UI nodes) rendered by skiko into a real GLFW window, on Kotlin/Native Linux, no JVM.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import glfw.GLFW_MOUSE_BUTTON_LEFT
import glfw.GLFW_PRESS
import glfw.GLFW_TRUE
import glfw.GLFW_VISIBLE
import glfw.glfwCreateWindow
import glfw.glfwDestroyWindow
import glfw.glfwGetMouseButton
import glfw.glfwInit
import glfw.glfwMakeContextCurrent
import glfw.glfwPollEvents
import glfw.glfwSwapBuffers
import glfw.glfwSwapInterval
import glfw.glfwTerminate
import glfw.glfwWindowHint
import glfw.glfwWindowShouldClose
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.Typeface
import platform.posix.usleep

private const val GL_RGBA8 = 0x8058

// --- Minimal UI node the composition builds through the Applier (a stand-in LayoutNode). ---
class UiNode {
    var x = 0f; var y = 0f; var w = 0f; var h = 0f
    var argb = 0xFF000000.toInt()
    var text: String? = null
    val children = mutableListOf<UiNode>()
}

class UiApplier(root: UiNode) : AbstractApplier<UiNode>(root) {
    override fun insertTopDown(index: Int, instance: UiNode) { current.children.add(index, instance) }
    override fun insertBottomUp(index: Int, instance: UiNode) {}
    override fun remove(index: Int, count: Int) { repeat(count) { current.children.removeAt(index) } }
    override fun move(from: Int, to: Int, count: Int) {}
    override fun onClear() { current.children.clear() }
}

@Composable
fun Box(x: Float, y: Float, w: Float, h: Float, color: Int) {
    ComposeNode<UiNode, UiApplier>(factory = { UiNode() }) {
        set(x) { this.x = it }
        set(y) { this.y = it }
        set(w) { this.w = it }
        set(h) { this.h = it }
        set(color) { this.argb = it }
    }
}

@Composable
fun Label(x: Float, y: Float, text: String) {
    ComposeNode<UiNode, UiApplier>(factory = { UiNode() }) {
        set(x) { this.x = it }
        set(y) { this.y = it }
        set(text) { this.text = it }
    }
}

// The composed UI: a header, a "button" box, and a live counter label.
@Composable
fun Ui(count: Int) {
    Box(40f, 40f, 320f, 56f, 0xFFEDE7F6.toInt())
    Label(60f, 76f, "Compose on Kotlin/Native Linux")
    Box(40f, 120f, 200f, 64f, 0xFF5E3BB4.toInt())
    Label(70f, 160f, "count: $count")
}

private fun drawTree(node: UiNode, canvas: Canvas, font: Font) {
    val t = node.text
    if (t != null) {
        canvas.drawString(t, node.x, node.y, font, Paint().apply { color = 0xFF1A1A1A.toInt() })
    } else if (node.w > 0f && node.h > 0f) {
        canvas.drawRRect(RRect.makeXYWH(node.x, node.y, node.w, node.h, 14f), Paint().apply { color = node.argb })
    }
    node.children.forEach { drawTree(it, canvas, font) }
}

private fun writePng(image: Image, path: String) {
    val data = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG) ?: return
    val bytes = data.bytes
    val f = platform.posix.fopen(path, "wb") ?: return
    bytes.usePinned { platform.posix.fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
    platform.posix.fclose(f)
    println("wrote $path (${bytes.size} bytes)")
}

fun main() = runBlocking {
    val width = 420
    val height = 240

    if (glfwInit() == 0) { println("glfwInit failed (no display?)"); return@runBlocking }
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
    val window = glfwCreateWindow(width, height, "POC4 ui-glfw (K/N Linux)", null, null)
        ?: run { println("window null"); glfwTerminate(); return@runBlocking }
    glfwMakeContextCurrent(window)
    glfwSwapInterval(0)

    val context = DirectContext.makeGL()
    val renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, GL_RGBA8)
    val surface = Surface.makeFromBackendRenderTarget(
        context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB,
    ) ?: run { println("skiko surface null"); return@runBlocking }
    val canvas = surface.canvas

    val typeface: Typeface = FontMgr.default.makeFromFile("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 0)
        ?: run { println("font load failed"); return@runBlocking }
    val font = Font(typeface, 18f)

    // Compose.
    val root = UiNode()
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + clock)
    val composition = Composition(UiApplier(root), recomposer)
    val count = mutableStateOf(0)
    composition.setContent { Ui(count.value) }
    val recomposeJob = launch(clock) { recomposer.runRecomposeAndApplyChanges() }

    fun capture(name: String) {
        val bmp = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(width, height) }
        if (surface.readPixels(bmp, 0, 0)) writePng(Image.makeFromBitmap(bmp), "/out/$name")
    }

    var frame = 0
    val maxFrames = 700
    var wasDown = false
    while (glfwWindowShouldClose(window) == 0 && frame < maxFrames) {
        // Input: detect a left-click press edge (polled) → recompose with count+1.
        val down = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        if (down && !wasDown) {
            count.value += 1
            Snapshot.sendApplyNotifications()
            println("POC4: click -> count=${count.value}")
        }
        wasDown = down

        clock.sendFrame(frame.toLong() * 16_000_000L)
        yield() // let the recomposer apply recomposition to the node tree

        canvas.clear(0xFFFFFFFF.toInt())
        drawTree(root, canvas, font)
        context.flush()
        glfwSwapBuffers(window)
        glfwPollEvents()

        if (frame == 40) capture("poc4-ui-before.png")   // initial (count=0)
        if (frame == maxFrames - 40) capture("poc4-ui-after.png") // after external click(s)
        usleep(8_000u)
        frame++
    }
    println("POC4: interactive Compose UI on K/N Linux, no JVM: final count=${count.value}")

    recomposer.close()
    recomposeJob.cancel()
    surface.close(); context.close(); glfwDestroyWindow(window); glfwTerminate()
}
