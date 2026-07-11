// Jalon 3 (windowing brick): render a skiko Skia rectangle into a REAL GLFW window on a GL context,
// in Kotlin/Native Linux. No Compose, no JVM. Run under a display server (Xvfb).
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

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
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import platform.posix.usleep

private const val GL_RGBA8 = 0x8058

fun renderInWindow(width: Int = 400, height: Int = 300, frames: Int = 300) {
    if (glfwInit() == 0) {
        println("renderInWindow: glfwInit failed (no display server?): skipping window path")
        return
    }
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
    val window = glfwCreateWindow(width, height, "POC3 K/N Linux (skiko GL)", null, null)
    if (window == null) {
        println("renderInWindow: glfwCreateWindow returned null")
        glfwTerminate()
        return
    }
    glfwMakeContextCurrent(window)
    glfwSwapInterval(1)

    val context = DirectContext.makeGL()
    val renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, GL_RGBA8)
    val surface = Surface.makeFromBackendRenderTarget(
        context,
        renderTarget,
        SurfaceOrigin.BOTTOM_LEFT,
        SurfaceColorFormat.RGBA_8888,
        ColorSpace.sRGB,
    )
    if (surface == null) {
        println("renderInWindow: skiko Surface.makeFromBackendRenderTarget returned null")
        return
    }
    val canvas: Canvas = surface.canvas
    val paint = Paint().apply { color = Color.makeRGB(90, 60, 180) }

    var drawn = 0
    while (glfwWindowShouldClose(window) == 0 && drawn < frames) {
        canvas.clear(Color.WHITE)
        canvas.drawRect(Rect.makeXYWH(50f, 50f, 300f, 200f), paint)
        context.flush()
        glfwSwapBuffers(window)
        glfwPollEvents()
        usleep(16_000u)
        drawn++
    }
    println("renderInWindow: drew $drawn frames of a skiko rectangle in a real GLFW window (K/N Linux, no JVM)")

    // Read the GL surface back into a raster Bitmap (a GPU image can't encode directly), then PNG: 
    // proof of the on-window content, independent of X presentation under headless Xvfb.
    val bitmap = org.jetbrains.skia.Bitmap()
    bitmap.allocN32Pixels(width, height)
    val ok = surface.readPixels(bitmap, 0, 0)
    val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
    val data = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
    if (ok && data != null) {
        val bytes = data.bytes
        val f = platform.posix.fopen("/out/poc3-window-gl.png", "wb")
        if (f != null) {
            bytes.usePinned { platform.posix.fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
            platform.posix.fclose(f)
            println("renderInWindow: wrote /out/poc3-window-gl.png (${bytes.size} bytes) read back from the GL window surface")
        }
    } else {
        println("renderInWindow: GL readPixels ok=$ok data=${data != null}")
    }

    surface.close()
    context.close()
    glfwDestroyWindow(window)
    glfwTerminate()
}
