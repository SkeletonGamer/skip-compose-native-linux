// A THIRD embedder for the same Compose stack, on Qt6.
//
// GTK settled the question (Jalon 13): once the clipboard moved behind a seam, Compose named no toolkit and
// the GTK binary linked zero GLFW. Qt is the control experiment. It asks two things GTK could not:
//
//  1. Does the seam actually generalise, or was it quietly shaped around GTK? Nothing in Compose changed for
//     this file. If the design is right, the Qt binary links first try, with no GLFW and no edits.
//  2. What does a C++ toolkit really cost? cinterop binds C and Objective-C only, so Qt cannot be bound at
//     all: it needs a hand-written extern "C" shim that owns the QObjects and turns Qt's virtual-method
//     events back into function pointers (native/qtshim/, ~95 lines of C++). GTK4, being plain C, cost none
//     of that. That gap is the honest number to quote to anyone who asks for Qt support.
//
// This is also the third clipboard backend: Compose gets Qt's system clipboard through the very same seam
// the GLFW mediator fills with GLFW's.
@file:OptIn(ExperimentalForeignApi::class)

package qtmain

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.LinuxClipboardBackend
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
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
import qtshim.qt_clipboard_get
import qtshim.qt_clipboard_set
import qtshim.qt_quit
import qtshim.qt_start
import testsupport.SemanticsExport

private const val GL_RGBA8 = 0x8058
private const val WHITE = -1
private const val WIDTH = 800
private const val HEIGHT = 600

private fun logln(msg: String) { println(msg); fflush(null) }

// staticCFunction captures nothing, so the embedder's state is global. Same constraint as GLFW and GTK.
private lateinit var scene: ComposeScene
private lateinit var frameRecomposer: FrameRecomposer
private var skiaContext: DirectContext? = null
private var renderTarget: BackendRenderTarget? = null
private var surface: Surface? = null
private var frame = 0
private var lastFbo = -1

private var clicks by mutableStateOf(0)

/** Qt's system clipboard, plugged into Compose through the seam. Compose never learns that Qt exists. */
private class QtClipboardBackend : LinuxClipboardBackend {
    override fun getText(): String? = qt_clipboard_get()?.toKString()?.takeIf { it.isNotEmpty() }
    override fun setText(text: String?) { qt_clipboard_set(text ?: "") }
}

@Composable
private fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Compose on Qt6", style = MaterialTheme.typography.headlineSmall)
            Text("Same compose klib as the GLFW and GTK builds. Only the embedder changed.")
            Text("Count: $clicks", modifier = Modifier.testTag("count"))
            Button(onClick = { clicks++ }, modifier = Modifier.testTag("increment")) {
                Text("Increment")
            }
        }
    }
}

private fun writePng(path: String) {
    val s = surface ?: return logln("[qt] no surface to snapshot")
    val bmp = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(WIDTH, HEIGHT) }
    if (!s.readPixels(bmp, 0, 0)) return logln("[qt] readPixels failed")
    val data = Image.makeFromBitmap(bmp).encodeToData(EncodedImageFormat.PNG)
        ?: return logln("[qt] PNG encode returned null")
    val bytes = data.bytes
    val f = platform.posix.fopen(path, "wb") ?: return logln("[qt] cannot open $path")
    bytes.usePinned { platform.posix.fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
    platform.posix.fclose(f)
    logln("[qt] wrote $path (${bytes.size} bytes)")
}

private fun onRender(fbo: Int) {
    if (skiaContext == null) {
        skiaContext = DirectContext.makeGL()
        logln("[qt] skia DirectContext created on Qt's GL context")
    }
    if (surface == null || fbo != lastFbo) {
        lastFbo = fbo
        renderTarget?.close()
        surface?.close()
        // Qt, like GTK, is asked rather than assumed: QOpenGLContext::defaultFramebufferObject() is not
        // always 0 (it is not on iOS, for one), so the shim hands it over every frame.
        logln("[qt] Qt's default framebuffer: fbo=$fbo")
        renderTarget = BackendRenderTarget.makeGL(WIDTH, HEIGHT, 0, 8, fbo, GL_RGBA8)
        surface = Surface.makeFromBackendRenderTarget(
            skiaContext!!, renderTarget!!, SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB, SurfaceProps(),
        )
    }
    val s = surface ?: return

    // Qt draws its own decorations and widgets through the same GL context, so Skia's cached GL state can be
    // stale, exactly as under GTK. Cheap insurance; the GLFW mediator needs none of it.
    skiaContext!!.resetAll()

    val frameNanos = frame * 16_000_000L
    androidx.compose.ui.drivePostDelayed(frameNanos)
    frameRecomposer.performFrame(frameNanos)
    scene.measureAndLayout()
    s.canvas.clear(WHITE)
    scene.draw(s.canvas.asComposeCanvas())
    skiaContext!!.flush()
    s.flushAndSubmit()

    frame++
    if (frame == 5) SemanticsExport.dump("/out/qt-tags.txt")
    if (frame == 30) {
        val c = SemanticsExport.centerOf("increment")
        if (c == null) {
            logln("[qt] the 'increment' tag is not laid out; cannot click")
        } else {
            logln("[qt] clicking the material3 Button at (${c.first}, ${c.second})")
            val at = Offset(c.first, c.second)
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at, button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, button = PointerButton.Primary)
        }
    }
    if (frame == 45) {
        writePng("/out/qt-compose.png")
        logln("[qt] clicks=$clicks")
        qt_quit()
    }
}

private fun onMouse(pressed: Int, x: Double, y: Double) {
    val at = Offset(x.toFloat(), y.toFloat())
    scene.sendPointerEvent(PointerEventType.Move, at)
    scene.sendPointerEvent(
        if (pressed == 1) PointerEventType.Press else PointerEventType.Release,
        at,
        button = PointerButton.Primary,
    )
}

fun main() = runBlocking {
    logln("[qt] starting the Qt6 embedder")

    // The third toolkit fills the same seam. Nothing in androidx/ knows Qt exists.
    NativeClipboard.backend = QtClipboardBackend()

    frameRecomposer = FrameRecomposer(coroutineContext)
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
    logln("[qt] compose scene created (the SAME klib the GLFW and GTK builds use)")

    qt_start(WIDTH, HEIGHT, "Compose on Qt6", staticCFunction(::onRender), staticCFunction(::onMouse))
    logln("[qt] done, clicks=$clicks")
}
