// A SECOND embedder for the same Compose stack, on GTK4 instead of GLFW.
//
// Why this exists. On the Kotlin Slack, Jake Wharton named the design objection to Compose on native Linux:
//
//   "The use of expect/actual instead of polymorphism means that it assumes there is only a single,
//    canonical UI toolkit for each build target and that's simply not true for Linux. If you actualize to
//    GTK then it would be impossible to use for Qt [...]"
//
// and Ivan Matkov (JetBrains) agreed, adding that changing it internally "is not so easy". The claim is
// testable, so this file tests it: it drives the very same compiled compose.ui/foundation/material3 klib and
// the very same 42 Linux actuals, but from GTK4 rather than GLFW. Whatever is genuinely nailed to a toolkit
// has to show up here, and the linker says so out loud: the "gtk" executable deliberately does NOT link
// -lglfw, so any part of Compose still reaching for GLFW becomes an undefined symbol.
//
// GTK4 is a plain C library, so cinterop binds it with no shim (see native/gtk4.def). Qt would not be so
// kind: it is C++ only, and Kotlin/Native cinterop speaks C and Objective-C, so Qt would need a hand-written
// extern "C" layer owning the QObjects. That is a cost worth knowing before anyone promises Qt support.
@file:OptIn(ExperimentalForeignApi::class)

package gtkmain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsOwner
import testsupport.SemanticsExport
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import platform.posix.fflush
import gtk.GdkGLContext
import gtk.GtkGLArea
import gtk.GtkGestureClick
import gtk.GtkWidget
import gtk.GtkWindow
import gtk.GdkFrameClock
import gtk.GDK_GL_API_GLES
import gtk.g_main_loop_new
import gtk.g_main_loop_quit
import gtk.g_main_loop_run
import gtk.gtk_gesture_click_new
import gtk.gtk_gl_area_new
import gtk.gtk_gl_area_queue_render
import gtk.gtk_gl_area_set_allowed_apis
import gtk.gtk_gl_area_set_has_depth_buffer
import gtk.gtk_gl_area_set_has_stencil_buffer
import gtk.gtk_init
import gtk.gtk_widget_add_controller
import gtk.gtk_widget_add_tick_callback
import gtk.gtk_window_new
import gtk.gtk_window_present
import gtk.gtk_window_set_child
import gtk.gtk_window_set_default_size
import gtk.gtk_window_set_title
import gtk.poc_gtk_current_fbo
import gtk.poc_gtk_samples
import gtk.poc_gtk_is_srgb
import gtk.poc_gtk_stencil_bits
import gtk.poc_signal_connect

private const val GL_RGBA8 = 0x8058
private const val GL_SRGB8_ALPHA8 = 0x8C43
private const val WHITE = -1 // opaque white (0xFFFFFFFF)
private const val WIDTH = 800
private const val HEIGHT = 600

private fun logln(msg: String) { println(msg); fflush(null) }

// GTK callbacks go through staticCFunction, which captures nothing, so the embedder's state has to be
// global. The GLFW mediator has the same constraint and solves it the same way.
private lateinit var scene: ComposeScene
private lateinit var frameRecomposer: FrameRecomposer
private var skiaContext: DirectContext? = null
private var renderTarget: BackendRenderTarget? = null
private var surface: Surface? = null
private var mainLoop: COpaquePointer? = null
private var frame = 0
private var lastFbo = -1

/** The counter is the whole point: it proves a real material3 Button recomposed under GTK. */
private var clicks by mutableStateOf(0)

@Composable
private fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Compose on GTK4", style = MaterialTheme.typography.headlineSmall)
            Text("Same compose klib as the GLFW build. Only the embedder changed.")
            Text("Count: $clicks", modifier = Modifier.testTag("count"))
            Button(onClick = { clicks++ }, modifier = Modifier.testTag("increment")) {
                Text("Increment")
            }
        }
    }
}

private fun writePng(path: String) {
    val s = surface ?: return logln("[gtk] no surface to snapshot")
    // A GPU-backed snapshot does not encode directly (encodeToData returns null): read the pixels back into
    // a raster bitmap first, exactly as the GLFW mediator does.
    val bmp = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(WIDTH, HEIGHT) }
    if (!s.readPixels(bmp, 0, 0)) return logln("[gtk] readPixels failed")
    val data = Image.makeFromBitmap(bmp).encodeToData(EncodedImageFormat.PNG)
        ?: return logln("[gtk] PNG encode returned null")
    val bytes = data.bytes
    val f = platform.posix.fopen(path, "wb") ?: return logln("[gtk] cannot open $path")
    bytes.usePinned { platform.posix.fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
    platform.posix.fclose(f)
    logln("[gtk] wrote $path (${bytes.size} bytes)")
}

@Suppress("UNUSED_PARAMETER")
private fun onRender(area: CPointer<GtkGLArea>?, ctx: CPointer<GdkGLContext>?, data: COpaquePointer?): Int {
    // GtkGLArea renders into an FBO it owns, NOT framebuffer 0: "GtkGLArea sets up its own GdkGLContext,
    // and creates a custom GL framebuffer that the widget will do GL rendering onto" (GTK4 docs). Skia has
    // to target that FBO, and the sanctioned way to learn its id is to read GL_FRAMEBUFFER_BINDING here.
    // The GLFW mediator passes 0 and is right to; doing the same here would render into nothing.
    val fbo = poc_gtk_current_fbo()

    if (skiaContext == null) {
        skiaContext = DirectContext.makeGL()
        logln("[gtk] skia DirectContext created on the GTK GL context")
    }
    if (surface == null || fbo != lastFbo) {
        lastFbo = fbo
        renderTarget?.close()
        surface?.close()
        val samples = poc_gtk_samples()
        val stencil = poc_gtk_stencil_bits()
        val srgb = poc_gtk_is_srgb() != 0
        logln("[gtk] GTK's framebuffer: fbo=$fbo samples=$samples stencilBits=$stencil srgb=$srgb")
        // Match Skia's idea of the target to GTK's actual encoding, or the glyph anti-aliasing is applied in
        // the wrong gamma and every letter comes out fat.
        val fbFormat = if (srgb) GL_SRGB8_ALPHA8 else GL_RGBA8
        renderTarget = BackendRenderTarget.makeGL(WIDTH, HEIGHT, samples, stencil, fbo, fbFormat)
        surface = Surface.makeFromBackendRenderTarget(
            skiaContext!!, renderTarget!!, SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB, SurfaceProps(),
        )
        logln("[gtk] skia surface bound to GTK's FBO id=$fbo")
    }

    val s = surface ?: return 1
    // GTK draws the widget tree with its OWN GL renderer between our frames, so the GL state Skia cached is
    // stale by the time we come back. Skia has to be told, or it renders with someone else's bindings and
    // the glyph masks come out fat and smeared. The GLFW mediator never needs this: nobody else touches GL
    // there. This is the one real cost of sharing a GL context with a toolkit that also draws.
    skiaContext!!.resetAll()
    // Exactly the frame protocol the GLFW mediator runs, unchanged: pump the scheduler, recompose, lay out,
    // clear (Compose only paints what it owns), draw, flush.
    val frameNanos = frame * 16_000_000L
    androidx.compose.ui.drivePostDelayed(frameNanos)
    frameRecomposer.performFrame(frameNanos)
    scene.measureAndLayout()
    s.canvas.clear(WHITE)
    scene.draw(s.canvas.asComposeCanvas())
    skiaContext!!.flush()
    s.flushAndSubmit()

    frame++
    // Headless proof: click the real material3 Button, then snapshot, then leave.
    if (frame == 5) SemanticsExport.dump("/out/gtk-tags.txt")
    if (frame == 30) {
        // Aim at the tag, never at a guessed pixel: the layout moves whenever the UI gains a line.
        val c = SemanticsExport.centerOf("increment")
        if (c == null) {
            logln("[gtk] the 'increment' tag is not laid out; cannot click")
        } else {
            val at = Offset(c.first, c.second)
            logln("[gtk] clicking the material3 Button at (${c.first}, ${c.second})")
            // Move first: Compose hit-tests on the pointer position, so a press with no prior move lands
            // on whatever was last under the (0,0) cursor, which is nothing.
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at, button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, button = PointerButton.Primary)
        }
    }
    if (frame == 45) {
        writePng("/out/gtk-compose.png")
        logln("[gtk] clicks=$clicks")
        mainLoop?.let { g_main_loop_quit(it.reinterpret()) }
    }
    return 1
}

@Suppress("UNUSED_PARAMETER")
private fun onTick(w: CPointer<GtkWidget>?, clock: CPointer<GdkFrameClock>?, data: COpaquePointer?): Int {
    gtk_gl_area_queue_render(w?.reinterpret())
    return 1 // G_SOURCE_CONTINUE
}

@Suppress("UNUSED_PARAMETER")
private fun onPressed(g: CPointer<GtkGestureClick>?, n: Int, x: Double, y: Double, data: COpaquePointer?) {
    scene.sendPointerEvent(PointerEventType.Press, Offset(x.toFloat(), y.toFloat()), button = PointerButton.Primary)
}

@Suppress("UNUSED_PARAMETER")
private fun onReleased(g: CPointer<GtkGestureClick>?, n: Int, x: Double, y: Double, data: COpaquePointer?) {
    scene.sendPointerEvent(PointerEventType.Release, Offset(x.toFloat(), y.toFloat()), button = PointerButton.Primary)
}

fun main() = runBlocking {
    logln("[gtk] starting the GTK4 embedder")
    gtk_init()

    val window = gtk_window_new()!!
    gtk_window_set_title(window.reinterpret<GtkWindow>(), "Compose on GTK4")
    gtk_window_set_default_size(window.reinterpret<GtkWindow>(), WIDTH, HEIGHT)

    val area = gtk_gl_area_new()!!
    // GLES, not desktop GL: the same reason the GLFW build links -lGLESv2 and not -lGL (libGL carries GLX,
    // which drags libX11 in and would break a Wayland-only system).
    gtk_gl_area_set_allowed_apis(area.reinterpret<GtkGLArea>(), GDK_GL_API_GLES)
    gtk_gl_area_set_has_depth_buffer(area.reinterpret<GtkGLArea>(), 0)
    gtk_gl_area_set_has_stencil_buffer(area.reinterpret<GtkGLArea>(), 1) // Skia needs stencil
    gtk_window_set_child(window.reinterpret<GtkWindow>(), area)

    frameRecomposer = FrameRecomposer(coroutineContext)
    // The embedder supplies the platform behaviour, exactly as the GLFW mediator does. Nothing here is
    // GTK-specific except this file: the compose klib is byte-for-byte the one the GLFW build links.
    val platformContext = object : PlatformContext by PlatformContext.Empty() {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
                SemanticsExport.owner = semanticsOwner
            }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
                if (SemanticsExport.owner === semanticsOwner) SemanticsExport.owner = null
            }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) = Unit
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) = Unit
        }
    }
    scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        density = Density(1f),
        size = IntSize(WIDTH, HEIGHT),
        platformContext = platformContext,
    )
    scene.setContent { App() }
    logln("[gtk] compose scene created (the SAME klib the GLFW build uses)")

    poc_signal_connect(area, "render", staticCFunction(::onRender), null)

    val click = gtk_gesture_click_new()!!
    poc_signal_connect(click, "pressed", staticCFunction(::onPressed), null)
    poc_signal_connect(click, "released", staticCFunction(::onReleased), null)
    gtk_widget_add_controller(area, click.reinterpret())

    gtk_widget_add_tick_callback(area, staticCFunction(::onTick), null, null)

    gtk_window_present(window.reinterpret<GtkWindow>())

    val loop = g_main_loop_new(null, 0)
    mainLoop = loop?.reinterpret()
    g_main_loop_run(loop)
    logln("[gtk] done, clicks=$clicks")
}
