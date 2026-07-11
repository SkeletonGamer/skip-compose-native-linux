// POC 5 Jalon 4: the real `ui-glfw` mediator. Drives an actual JetBrains `ComposeScene` (compiled for
// Kotlin/Native Linux at Jalons 2-3) into a GLFW window via a skiko GL surface, no JVM. The content is
// real material3 (MaterialTheme + Button + Text), not the hand-written stand-in of POC 4.
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import glfw.GLFW_MOUSE_BUTTON_LEFT
import glfw.GLFW_PRESS
import glfw.GLFW_TRUE
import glfw.GLFW_VISIBLE
import glfw.glfwCreateWindow
import glfw.glfwDestroyWindow
import glfw.glfwGetCursorPos
import glfw.glfwGetMouseButton
import glfw.glfwInit
import glfw.glfwMakeContextCurrent
import glfw.glfwPollEvents
import glfw.glfwSwapBuffers
import glfw.glfwSwapInterval
import glfw.glfwTerminate
import glfw.glfwWindowHint
import glfw.glfwWindowShouldClose
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import platform.posix.usleep

private const val GL_RGBA8 = 0x8058

// stdout is block-buffered when redirected (no TTY); flush so progress is visible live.
private fun logln(msg: String) { println(msg); platform.posix.fflush(null) }

// The composed UI: real material3 driven by the real ComposeScene.
@Composable
private fun App() {
    var count by remember { mutableStateOf(0) }
    MaterialTheme {
        Column(androidx.compose.ui.Modifier.padding(24.dp)) {
            Text("Compose material3 on Kotlin/Native Linux, no JVM")
            Button(onClick = { count++ }, modifier = androidx.compose.ui.Modifier.padding(top = 16.dp)) {
                Text("count: $count")
            }
        }
    }
}

private fun writePng(surface: Surface, width: Int, height: Int, path: String) {
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
    val width = 520
    val height = 300

    logln("POC5: start")
    if (glfwInit() == 0) { logln("glfwInit failed (no display?)"); return@runBlocking }
    logln("POC5: glfw init ok")
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
    val window = glfwCreateWindow(width, height, "POC5 ui-glfw material3 (K/N Linux)", null, null)
        ?: run { logln("window null"); glfwTerminate(); return@runBlocking }
    glfwMakeContextCurrent(window)
    glfwSwapInterval(0)
    logln("POC5: window + GL context ok")

    val context = DirectContext.makeGL()
    val renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, GL_RGBA8)
    val surface = Surface.makeFromBackendRenderTarget(
        context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB,
    ) ?: run { logln("skiko surface null"); return@runBlocking }
    val composeCanvas = surface.canvas.asComposeCanvas()
    logln("POC5: skiko GL surface ok")

    // Mediator wiring: FrameRecomposer + PlatformContext (window info) + the real ComposeScene.
    val density = Density(1f)
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
    logln("POC5: scene + material3 content set, entering loop")

    var frame = 0
    val maxFrames = 120
    var wasDown = false
    while (glfwWindowShouldClose(window) == 0 && frame < maxFrames) {
        // Input: left-click edge -> Press then Release at the cursor position (drives material3 Button).
        val down = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
        if (down != wasDown) {
            memScoped {
                val xp = alloc<DoubleVar>()
                val yp = alloc<DoubleVar>()
                glfwGetCursorPos(window, xp.ptr, yp.ptr)
                val pos = Offset(xp.value.toFloat(), yp.value.toFloat())
                scene.sendPointerEvent(
                    eventType = if (down) PointerEventType.Press else PointerEventType.Release,
                    position = pos,
                    button = PointerButton.Primary,
                )
            }
        }
        wasDown = down

        // Headless (Xvfb) has no real mouse: inject a synthetic click at the Button center to prove
        // the real material3 Button's onClick fires through the real hit-testing + layout (count 0 -> 1).
        val buttonCenter = Offset(90f, 88f)
        if (frame == 40) scene.sendPointerEvent(PointerEventType.Press, buttonCenter, button = PointerButton.Primary)
        if (frame == 44) {
            scene.sendPointerEvent(PointerEventType.Release, buttonCenter, button = PointerButton.Primary)
            logln("POC5: injected synthetic click on the material3 Button")
        }

        // One Compose frame: drain due postDelayed callbacks (RectManager debounce) on this thread,
        // then advance recomposition, layout, and draw into the skiko canvas.
        val frameNanos = frame.toLong() * 16_000_000L
        androidx.compose.ui.drivePostDelayed(frameNanos)
        frameRecomposer.performFrame(frameNanos)
        scene.measureAndLayout()
        composeCanvas.let { scene.draw(it) }
        context.flush()
        glfwSwapBuffers(window)
        glfwPollEvents()

        if (frame % 20 == 0) logln("POC5: frame $frame")
        if (frame == 5) writePng(surface, width, height, "/out/poc5-material3-before.png")
        if (frame == maxFrames - 5) writePng(surface, width, height, "/out/poc5-material3-after.png")
        usleep(8_000u)
        frame++
    }
    println("POC5 Jalon 4: real material3 ComposeScene on K/N Linux, no JVM")

    scene.close()
    frameRecomposer.close()
    surface.close(); context.close(); glfwDestroyWindow(window); glfwTerminate()
}
