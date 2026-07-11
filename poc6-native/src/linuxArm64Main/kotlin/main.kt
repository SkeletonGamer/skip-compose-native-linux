// POC 6 Jalon 5: entry point that drives the transpiled SwiftUI ContentView (via the real SkipUI) through
// the real JetBrains ComposeScene into a GLFW window, on Kotlin/Native Linux, no JVM. Same mediator as
// POC 5, but the content is Skip's transpiled `ContentView`, not a hand-written material3 sample.
// NOTE: the java.*/android.* shims throw TODO() at runtime, so this LINKS (for the size measurement) and
// will crash at runtime on the first shim reached, until the runtime is made functional.
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import glfw.GLFW_TRUE
import glfw.GLFW_VISIBLE
import glfw.glfwCreateWindow
import glfw.glfwDestroyWindow
import glfw.glfwInit
import glfw.glfwMakeContextCurrent
import glfw.glfwPollEvents
import glfw.glfwSwapBuffers
import glfw.glfwSwapInterval
import glfw.glfwTerminate
import glfw.glfwWindowHint
import glfw.glfwWindowShouldClose
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface as SkiaSurface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import platform.posix.usleep

private const val GL_RGBA8 = 0x8058

private fun logln(msg: String) { println(msg); platform.posix.fflush(null) }

// Pic RSS (VmHWM) lu dans /proc/self/status, pour la mesure memoire du Jalon 5.
private fun logPeakRss() {
    val f = platform.posix.fopen("/proc/self/status", "r") ?: return
    kotlinx.cinterop.memScoped {
        val buf = allocArray<kotlinx.cinterop.ByteVar>(256)
        while (platform.posix.fgets(buf, 256, f) != null) {
            val line = buf.toKString()
            if (line.startsWith("VmHWM:")) { logln("POC6 peak RSS: ${line.trim()}") }
        }
    }
    platform.posix.fclose(f)
}

// The composed UI: the transpiled SwiftUI ContentView, rendered by the real SkipUI.
@Composable
private fun App() {
    MaterialTheme {
        Surface {
            witness.module.MinimalContentView().Compose(skip.ui.ComposeContext())
        }
    }
}

private fun writePng(surface: SkiaSurface, width: Int, height: Int, path: String) {
    val bmp = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(width, height) }
    if (!surface.readPixels(bmp, 0, 0)) return
    val data = Image.makeFromBitmap(bmp).encodeToData(EncodedImageFormat.PNG) ?: return
    val bytes = data.bytes
    val f = platform.posix.fopen(path, "wb") ?: return
    bytes.usePinned { platform.posix.fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
    platform.posix.fclose(f)
    println("wrote $path (${bytes.size} bytes)")
}

fun main() = runBlocking {
    val width = 420
    val height = 640

    logln("POC6: start")
    if (glfwInit() == 0) { logln("glfwInit failed (no display?)"); return@runBlocking }
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
    val window = glfwCreateWindow(width, height, "POC6 SkipUI (K/N Linux)", null, null)
        ?: run { logln("window null"); glfwTerminate(); return@runBlocking }
    glfwMakeContextCurrent(window)
    glfwSwapInterval(0)

    val context = DirectContext.makeGL()
    val renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, GL_RGBA8)
    val surface = SkiaSurface.makeFromBackendRenderTarget(
        context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB,
    ) ?: run { logln("skiko surface null"); return@runBlocking }
    val composeCanvas = surface.canvas.asComposeCanvas()

    val density = Density(2f)
    val size = IntSize(width, height)
    val winInfo = WindowInfoImpl().apply {
        isWindowFocused = true
        containerSize = size
    }
    val platformContext = object : PlatformContext by PlatformContext.Empty() {
        override val windowInfo: WindowInfo get() = winInfo
    }
    val frameRecomposer = FrameRecomposer(coroutineContext)
    val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        density = density,
        size = size,
        platformContext = platformContext,
    )
    scene.setContent { App() }
    logln("POC6: scene + transpiled ContentView set, entering loop")

    // Interactivity check (no JVM): synthesize a tap on the "Increment" button (~(118,120) px in the
    // 420x640 canvas), then render again. If the click drives a recompose, "Count: 0" becomes "Count: 1".
    val buttonPos = Offset(118f, 120f)
    var frame = 0
    val maxFrames = 60
    while (glfwWindowShouldClose(window) == 0 && frame < maxFrames) {
        val frameNanos = frame.toLong() * 16_000_000L
        val timeMillis = frame.toLong() * 16L
        // press then release a few frames apart so the tap gesture detector completes.
        if (frame == 8) scene.sendPointerEvent(PointerEventType.Press, buttonPos, timeMillis = timeMillis)
        if (frame == 12) scene.sendPointerEvent(PointerEventType.Release, buttonPos, timeMillis = timeMillis)
        androidx.compose.ui.drivePostDelayed(frameNanos)
        frameRecomposer.performFrame(frameNanos)
        scene.measureAndLayout()
        composeCanvas.let { scene.draw(it) }
        context.flush()
        glfwSwapBuffers(window)
        glfwPollEvents()
        if (frame == 5) writePng(surface, width, height, "/out/poc6-skipui-native.png")
        if (frame == 40) writePng(surface, width, height, "/out/poc6-skipui-native-after-click.png")
        usleep(8_000u)
        frame++
    }
    println("POC6 Jalon 5: transpiled SwiftUI ContentView via real SkipUI on K/N Linux, no JVM")
    logPeakRss()

    scene.close()
    frameRecomposer.close()
    surface.close(); context.close(); glfwDestroyWindow(window); glfwTerminate()
}
