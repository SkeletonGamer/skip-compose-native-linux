// POC 5 Jalon 4/6: the real `ui-glfw` mediator. Drives an actual JetBrains `ComposeScene` (compiled for
// Kotlin/Native Linux at Jalons 2-3) into a GLFW window via a skiko GL surface, no JVM.
//
// Lot 1 turns this from a render demo into a usable window: real keyboard, mouse wheel, hover, cursors,
// resize, HiDPI and a real monotonic clock, all fed from GLFW callbacks. The content exercises each of
// them (a text field to type into, a scrollable list, a copy button that writes to the system clipboard).
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.ui.InternalComposeUiApi::class,
)

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.LinuxCursor
import androidx.compose.ui.input.pointer.LinuxCursorShape
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.intl.isRightToLeft
import testsupport.SemanticsExport
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import glfw.*
import glfwinput.InputEvent
import glfwinput.InputQueue
import glfwinput.glfwKeyToCompose
import glfwinput.hasAlt
import glfwinput.hasCtrl
import glfwinput.hasShift
import glfwinput.hasSuper
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.get
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import linuxglfw.GlfwBridge
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.getenv
import platform.posix.timespec
import platform.posix.usleep
import kotlinx.cinterop.toKString

private const val GL_RGBA8 = 0x8058
private const val WHITE = 0xFFFFFFFF.toInt()

// stdout is block-buffered when redirected (no TTY); flush so progress is visible live.
private fun logln(msg: String) { println(msg); platform.posix.fflush(null) }

/** Real monotonic time. The old loop used `frame * 16ms`, so animations advanced in frames, not seconds. */
private fun nowNanos(): Long = memScoped {
    val ts = alloc<timespec>()
    clock_gettime(CLOCK_MONOTONIC, ts.ptr)
    ts.tv_sec * 1_000_000_000L + ts.tv_nsec
}

// The composed UI. Every widget here exists to exercise one thing Lot 1 wires up.
@Composable
private fun App(clipboard: Clipboard) {
    var count by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var dropped by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val locale = androidx.compose.ui.text.intl.Locale.current
    MaterialTheme {
        Column(
            Modifier.dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = remember {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            dropped = event.files.joinToString(", ") { it.substringAfterLast('/') }
                            return true
                        }
                    }
                },
            ).padding(16.dp)
        ) {
            Text("Compose material3 on Kotlin/Native Linux, no JVM")
            // Locale comes from the POSIX environment now, not a hardcoded en-US. With LANG=ar_EG the
            // whole column mirrors, because the scene's LayoutDirection follows it.
            Text("locale: ${locale.toLanguageTag()}", Modifier.padding(top = 4.dp))
            // Proof that CLDR data is reaching material3: the month/weekday names below come from ICU,
            // and they are the thing that cannot be derived without it.
            Text("date: ${localizedDateSample(locale)}", Modifier.padding(top = 2.dp))

            // Keyboard: typing here proves glfwSetCharCallback -> scene.sendKeyEvent -> Compose.
            // The field takes focus on start. That is what makes Compose call startInput() on our
            // PlatformTextInputService, which enables the Wayland IME. It also matters for the test: under
            // a headless compositor there is no pointer, so the field could never be clicked into.
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("type here") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    .focusRequester(focusRequester)
                    .testTag("field"),
            )
            // Echoed so a screenshot can prove what was typed.
            Text("typed: [$text]", modifier = Modifier.padding(top = 4.dp))

            Row(Modifier.padding(top = 8.dp)) {
                Button(onClick = { count++ }, modifier = Modifier.testTag("count")) { Text("count: $count") }
                // Clipboard: writes to the real X11/Wayland clipboard via GLFW.
                Button(
                    onClick = { scope.launch { clipboard.setClipEntry(clipEntryOf("copied:$text")) } },
                    modifier = Modifier.padding(start = 8.dp).testTag("copy"),
                ) { Text("copy") }
            }

            // Drag and drop: a real target. Files dropped on the window arrive here through
            // rootDragAndDropNode. No Kotlin/Native Compose target has drag and drop at all (the macOS
            // actual cannot even construct a DragAndDropEvent), so this had to be built from the actual up.
            Text("dropped: [$dropped]", Modifier.padding(top = 8.dp).testTag("dropped"))

            // Scroll: the mouse wheel must move this. Both the ScrollConfig and the GLFW scroll
            // callback had to exist for this to work; either one missing means it stays at 0.
            Text("first visible item: ${listState.firstVisibleItemIndex}", Modifier.padding(top = 8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().height(120.dp).testTag("list"),
            ) {
                items((1..40).toList()) { i -> Text("item $i", Modifier.padding(4.dp)) }
            }
        }
    }
}

// Small helper: ClipEntry's factory is @ExperimentalComposeUiApi and lives on the companion.
private fun clipEntryOf(text: String) =
    androidx.compose.ui.platform.ClipEntry.withPlainText(text)

// Formats a fixed date (2026-07-14) through material3's own PlatformDateFormat, the same class the
// DatePicker uses. With ICU present this comes out as "14 juillet 2026" under fr_FR and "July 14, 2026"
// under en_US; without ICU both fall back to English.
private fun localizedDateSample(locale: androidx.compose.ui.text.intl.Locale): String {
    val fmt = androidx.compose.material3.internal.PlatformDateFormat(
        androidx.compose.material3.CalendarLocale(locale.toLanguageTag())
    )
    val millis = 1_784_000_000_000L // 2026-07-14T05:33:20Z
    val date = fmt.formatWithSkeleton(millis, "yMMMMd", mutableMapOf())
    val firstDay = fmt.weekdayNames.firstOrNull()?.first ?: "?"
    return "$date | week starts: $firstDay"
}

private fun writePng(surface: Surface, width: Int, height: Int, path: String) {
    val bmp = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(width, height) }
    if (!surface.readPixels(bmp, 0, 0)) return
    val data = Image.makeFromBitmap(bmp).encodeToData(EncodedImageFormat.PNG) ?: return
    val bytes = data.bytes
    val f = platform.posix.fopen(path, "wb") ?: return
    bytes.usePinned { platform.posix.fwrite(it.addressOf(0), 1u, bytes.size.toULong(), f) }
    platform.posix.fclose(f)
    logln("wrote $path (${bytes.size} bytes)")
}

// --- GLFW callbacks. staticCFunction captures nothing, so they only touch the global InputQueue. ---

private val keyCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Int, Int, Int, Int, Unit> { _, key, _, action, mods ->
    InputQueue.push(InputEvent.KeyPress(key, action, mods))
}
private val charCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, UInt, Unit> { _, codepoint ->
    InputQueue.push(InputEvent.Typed(codepoint.toInt()))
}
private val scrollCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Double, Double, Unit> { _, dx, dy ->
    InputQueue.push(InputEvent.Scroll(dx, dy))
}
private val mouseBtnCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Int, Int, Int, Unit> { _, button, action, _ ->
    InputQueue.push(InputEvent.MouseButton(button, action))
}
private val cursorPosCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Double, Double, Unit> { _, x, y ->
    InputQueue.push(InputEvent.MouseMove(x, y))
}
private val fbSizeCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Int, Int, Unit> { _, w, h ->
    InputQueue.push(InputEvent.Resize(w, h))
}
private val focusCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Int, Unit> { _, focused ->
    InputQueue.push(InputEvent.Focus(focused == GLFW_TRUE))
}
// Files dropped onto the window. GLFW hands us a C array of UTF-8 paths, valid only for the duration of
// the callback, so they are copied out immediately.
private val dropCb = staticCFunction<CPointer<cnames.structs.GLFWwindow>?, Int, CPointer<CPointerVar<ByteVar>>?, Unit> { _, count, paths ->
    val list = mutableListOf<String>()
    if (paths != null) {
        for (i in 0 until count) paths[i]?.toKString()?.let { list.add(it) }
    }
    InputQueue.push(InputEvent.FileDrop(list))
}

fun main() = runBlocking {
    var width = 640
    var height = 480

    logln("POC5: start")
    if (glfwInit() == 0) { logln("glfwInit failed (no display?)"); return@runBlocking }

    // GLFW 3.4 picks X11 or Wayland at runtime (it dlopens the backend, so neither is in our NEEDED list).
    // Say which one we actually got: the app has to work on both, and Budgie 10.10 / Ubuntu Budgie 26.04
    // ship no X11 session at all any more.
    val platformName = when (val p = glfwGetPlatform()) {
        GLFW_PLATFORM_WAYLAND -> "Wayland"
        GLFW_PLATFORM_X11 -> "X11"
        GLFW_PLATFORM_NULL -> "null (headless)"
        else -> "unknown($p)"
    }
    logln("POC5: glfw init ok, platform = $platformName")
    logln("POC5: wayland supported = ${glfwPlatformSupported(GLFW_PLATFORM_WAYLAND) == GLFW_TRUE}, " +
        "x11 supported = ${glfwPlatformSupported(GLFW_PLATFORM_X11) == GLFW_TRUE}")
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
    val window = glfwCreateWindow(width, height, "POC5 ui-glfw material3 (K/N Linux)", null, null)
        ?: run { logln("window null"); glfwTerminate(); return@runBlocking }
    glfwMakeContextCurrent(window)
    glfwSwapInterval(0)
    logln("POC5: window + GL context ok")

    // Publish the window + cursors so the compose actuals (pointer icon) can reach them.
    GlfwBridge.window = window

    // Install the system clipboard. Compose no longer knows GLFW exists: its Linux actual keeps an in-process
    // clipboard and exposes a seam, and the embedder that owns the window fills it. That is what lets the very
    // same compose klib link into the GTK embedder with no GLFW at all (see gtkmain/GtkMain.kt).
    androidx.compose.ui.platform.NativeClipboard.backend = linuxglfw.GlfwClipboardBackend()

    GlfwBridge.cursors = mapOf(
        LinuxCursorShape.Default to glfwCreateStandardCursor(GLFW_ARROW_CURSOR),
        LinuxCursorShape.Text to glfwCreateStandardCursor(GLFW_IBEAM_CURSOR),
        LinuxCursorShape.Hand to glfwCreateStandardCursor(GLFW_HAND_CURSOR),
        LinuxCursorShape.Crosshair to glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR),
    )

    glfwSetKeyCallback(window, keyCb)
    glfwSetCharCallback(window, charCb)
    glfwSetScrollCallback(window, scrollCb)
    glfwSetMouseButtonCallback(window, mouseBtnCb)
    glfwSetCursorPosCallback(window, cursorPosCb)
    glfwSetFramebufferSizeCallback(window, fbSizeCb)
    glfwSetWindowFocusCallback(window, focusCb)
    glfwSetDropCallback(window, dropCb)
    logln("POC5: glfw callbacks wired (key, char, scroll, mouse, move, resize, focus, file drop)")

    // HiDPI: the framebuffer can be larger than the window. Density(1f) used to be hardcoded, so the UI
    // was drawn at 1x on a 2x screen.
    val contentScale = memScoped {
        val sx = alloc<FloatVar>()
        val sy = alloc<FloatVar>()
        glfwGetWindowContentScale(window, sx.ptr, sy.ptr)
        if (sx.value > 0f) sx.value else 1f
    }
    memScoped {
        val fw = alloc<IntVar>(); val fh = alloc<IntVar>()
        glfwGetFramebufferSize(window, fw.ptr, fh.ptr)
        if (fw.value > 0) { width = fw.value; height = fh.value }
    }
    logln("POC5: content scale = $contentScale, framebuffer = ${width}x$height")

    val context = DirectContext.makeGL()

    // Surface + render target are recreated on resize; hold them in vars.
    var renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, GL_RGBA8)
    var surface = Surface.makeFromBackendRenderTarget(
        context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB,
    ) ?: run { logln("skiko surface null"); return@runBlocking }
    var composeCanvas = surface.canvas.asComposeCanvas()
    logln("POC5: skiko GL surface ok")

    val density = Density(contentScale)
    val winInfo = WindowInfoImpl().apply {
        isWindowFocused = true
        containerSize = IntSize(width, height)
    }
    // The IME. Compose calls startInput() on this when a text field takes focus, which is what enables the
    // Wayland text-input protocol (and, on a phone, raises the virtual keyboard). Without handing it to the
    // PlatformContext, Compose keeps using EmptyPlatformTextInputService and no IME can ever engage.
    val textInput = waylandime.LinuxTextInputService()

    // setPointerIcon is what makes the cursor actually change shape over a text field or a button.
    val platformContext = object : PlatformContext by PlatformContext.Empty() {
        override val windowInfo: WindowInfo get() = winInfo
        // The MODERN text-input contract. material3's TextField goes through this, not the legacy
        // PlatformTextInputService: implementing only the legacy one gave a focused field whose IME never
        // engaged, because the default startInputMethod here is awaitCancellation().
        override suspend fun startInputMethod(
            request: androidx.compose.ui.platform.PlatformTextInputMethodRequest
        ): Nothing = textInput.startInputMethod(request)
        override fun setPointerIcon(pointerIcon: PointerIcon) {
            val kind = (pointerIcon as? LinuxCursor)?.shape ?: LinuxCursorShape.Default
            glfwSetCursor(window, GlfwBridge.cursors[kind])
            logln("POC5: cursor -> $kind")
        }
        // Compose's semantics tree, which Modifier.testTag() feeds. The mediator publishes it so tests can
        // locate widgets by name rather than by pixel. This is also the tree an AT-SPI bridge would use.
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
    // Text direction follows the system locale, the way the desktop backend derives it from AWT's
    // ComponentOrientation. Without this the scene is always Ltr, so an Arabic or Hebrew locale would
    // still lay out left-to-right.
    val locale = androidx.compose.ui.text.intl.Locale.current
    val layoutDirection =
        if (locale.isRightToLeft()) LayoutDirection.Rtl else LayoutDirection.Ltr
    logln("POC5: locale = ${locale.toLanguageTag()}, layout direction = $layoutDirection")
    // ICU is dlopen'd, not linked, so say which one (if any) this run actually found.
    logln("POC5: ${icu.Icu.describe}")
    logln("POC5: date sample = ${localizedDateSample(locale)}")

    val frameRecomposer = FrameRecomposer(coroutineContext)
    val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        density = density,
        size = IntSize(width, height),
        layoutDirection = layoutDirection,
        platformContext = platformContext,
    )

    // createPlatformClipboard() is `internal` to compose.ui, and the mediator is compiled into the same
    // module (that is the whole point of the extract-and-compile route), so it is reachable from here.
    // Bind the IME protocol BEFORE composing. Compose focuses the text field during setContent, which
    // starts an input session immediately: if the protocol is not bound yet, that enable() lands on
    // nothing and the IME never engages (observed exactly that way).
    if (platformName == "Wayland") {
        waylandime.initWaylandTextInput()
    } else {
        logln("POC5: IME not wired (not on Wayland; X11 would need a separate XIM/IBus backend)")
    }

    val clipboard = androidx.compose.ui.platform.createPlatformClipboard()
    scene.setContent { App(clipboard) }
    logln("POC5: scene + material3 content set, entering loop")

    // Duration: default is the short demo (headless capture). POC5_RUN_SECONDS keeps the window alive so
    // an external driver (xdotool/xclip) can send real X11 input at it.
    val runSeconds = getenv("POC5_RUN_SECONDS")?.toKString()?.toIntOrNull()
    val startNanos = nowNanos()
    val deadlineNanos = runSeconds?.let { startNanos + it * 1_000_000_000L }
    val maxFrames = if (runSeconds == null) 120 else Int.MAX_VALUE
    val headless = runSeconds == null

    var frame = 0
    var cursorX = 0.0
    var cursorY = 0.0

    while (glfwWindowShouldClose(window) == 0 && frame < maxFrames) {
        if (deadlineNanos != null && nowNanos() > deadlineNanos) break

        // Replay everything GLFW handed us since the last frame into Compose.
        for (event in InputQueue.drain()) {
            when (event) {
                is InputEvent.MouseMove -> {
                    cursorX = event.x; cursorY = event.y
                    // Hover: without a Move event, buttons never highlight and the cursor never changes.
                    scene.sendPointerEvent(PointerEventType.Move, Offset(cursorX.toFloat(), cursorY.toFloat()))
                }
                is InputEvent.MouseButton -> {
                    val type = if (event.action == GLFW_PRESS) PointerEventType.Press else PointerEventType.Release
                    val button = when (event.button) {
                        GLFW_MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
                        GLFW_MOUSE_BUTTON_MIDDLE -> PointerButton.Tertiary
                        else -> PointerButton.Primary
                    }
                    scene.sendPointerEvent(type, Offset(cursorX.toFloat(), cursorY.toFloat()), button = button)
                }
                is InputEvent.Scroll -> {
                    scene.sendPointerEvent(
                        eventType = PointerEventType.Scroll,
                        position = Offset(cursorX.toFloat(), cursorY.toFloat()),
                        scrollDelta = Offset(-event.dx.toFloat(), -event.dy.toFloat()),
                    )
                    logln("POC5: scroll dy=${event.dy} at ($cursorX,$cursorY)")
                }
                is InputEvent.KeyPress -> {
                    // Non-printable keys and shortcuts. Printable characters arrive via Typed below with
                    // their code point; sending them here with codePoint 0 would not count as typed.
                    val type = when (event.action) {
                        GLFW_RELEASE -> KeyEventType.KeyUp
                        else -> KeyEventType.KeyDown // PRESS and REPEAT
                    }
                    scene.sendKeyEvent(
                        KeyEvent(
                            key = glfwKeyToCompose(event.key),
                            type = type,
                            codePoint = 0,
                            isCtrlPressed = event.mods.hasCtrl(),
                            isMetaPressed = event.mods.hasSuper(),
                            isAltPressed = event.mods.hasAlt(),
                            isShiftPressed = event.mods.hasShift(),
                        )
                    )
                }
                is InputEvent.Typed -> {
                    // A real character. Compose's isTypedEvent looks at utf16CodePoint, so this is what
                    // actually inserts text into a field.
                    scene.sendKeyEvent(
                        KeyEvent(
                            key = androidx.compose.ui.input.key.Key.Unknown,
                            type = KeyEventType.KeyDown,
                            codePoint = event.codePoint,
                        )
                    )
                    logln("POC5: typed codepoint=${event.codePoint}")
                }
                is InputEvent.Focus -> winInfo.isWindowFocused = event.focused
                is InputEvent.FileDrop -> {
                    // Deliver the drop to Compose. The node wants the sequence a real drag produces:
                    // started -> entered -> drop -> ended. Without started/entered, no dragAndDropTarget
                    // has been offered the transfer and the drop is refused.
                    val pos = Offset(cursorX.toFloat(), cursorY.toFloat())
                    val dnd = scene.rootDragAndDropNode
                    val startEvent = androidx.compose.ui.draganddrop.DragAndDropEvent(pos, event.paths)
                    if (dnd.acceptDragAndDropTransfer(startEvent)) {
                        // The full sequence a real drag produces. onMoved is what makes Compose hit-test
                        // the pointer position and pick the target under it; without it the drop is
                        // delivered to nobody and comes back refused.
                        dnd.onStarted(startEvent)
                        dnd.onEntered(startEvent)
                        dnd.onMoved(startEvent)
                        val accepted = dnd.onDrop(startEvent)
                        dnd.onEnded(startEvent)
                        logln("POC5: file drop (${event.paths.size} file(s)) accepted=$accepted")
                    } else {
                        logln("POC5: file drop (${event.paths.size} file(s)) -- no target accepted it")
                    }
                }
                is InputEvent.Resize -> {
                    if (event.width > 0 && event.height > 0 && (event.width != width || event.height != height)) {
                        width = event.width; height = event.height
                        // Surface and render target are bound to the old size: rebuild them, then tell
                        // Compose. Skipping any of these three left the old rendering stretched.
                        surface.close(); renderTarget.close()
                        renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, GL_RGBA8)
                        surface = Surface.makeFromBackendRenderTarget(
                            context, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
                            SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB,
                        ) ?: break
                        composeCanvas = surface.canvas.asComposeCanvas()
                        scene.size = IntSize(width, height)
                        winInfo.containerSize = IntSize(width, height)
                        logln("POC5: resized to ${width}x$height")
                    }
                }
            }
        }

        // Drag and drop: nothing in the harness can perform a real XDND drag (xdotool cannot, and no
        // Debian package provides a drag source), so the drop is injected here. What this DOES exercise is
        // everything downstream of the window system: the DragAndDropEvent actual, rootDragAndDropNode, and
        // Compose routing it to a dragAndDropTarget. The GLFW callback that feeds it (glfwSetDropCallback)
        // is standard wiring but is NOT covered by a test.
        if (frame == 70) {
            // Drop in the middle of the window. Without a mouse the cursor sits at (0,0), which is outside
            // the target once padding is applied, and Compose rightly refuses the drop.
            cursorX = (width / 2).toDouble()
            cursorY = (height / 2).toDouble()
            InputQueue.push(InputEvent.FileDrop(listOf("/tmp/report.pdf", "/tmp/photo.png")))
        }

        // Headless demo: no real mouse, so inject a click on the count button to prove hit-testing.
        if (headless) {
            val buttonCenter = Offset(60f, 210f)
            if (frame == 40) scene.sendPointerEvent(PointerEventType.Press, buttonCenter, button = PointerButton.Primary)
            if (frame == 44) {
                scene.sendPointerEvent(PointerEventType.Release, buttonCenter, button = PointerButton.Primary)
                logln("POC5: injected synthetic click on the material3 Button")
            }
        }

        // Turn what the IME sent into Compose edits. The Wayland listeners are staticCFunction and cannot
        // call into Compose, so they queue; this drains the queue on the compose thread.
        textInput.drain()

        val frameNanos = nowNanos()
        androidx.compose.ui.drivePostDelayed(frameNanos)
        frameRecomposer.performFrame(frameNanos)
        scene.measureAndLayout()
        // Clear first: Compose only paints what it owns, so without this every frame is drawn on top of
        // the last one. A static screen hides it; a text field or a scrolling list turns into mush.
        surface.canvas.clear(WHITE)
        scene.draw(composeCanvas)
        context.flush()
        glfwSwapBuffers(window)
        glfwPollEvents()

        // Publish where every tagged widget actually is, once the first layout has settled. Tests read this
        // and click by tag, so a UI change cannot silently make them click on the wrong thing.
        if (frame == 3) {
            SemanticsExport.dump("/out/tags.txt")
            logln("POC5: semantics tags exported to /out/tags.txt")
        }

        if (headless) {
            if (frame % 20 == 0) logln("POC5: frame $frame")
            if (frame == 5) writePng(surface, width, height, "/out/poc5-material3-before.png")
            if (frame == maxFrames - 5) writePng(surface, width, height, "/out/poc5-material3-after.png")
        } else if (frame % 120 == 0) {
            // Interactive run: keep dumping the current state so the test driver can capture it.
            writePng(surface, width, height, "/out/poc5-live.png")
        }
        usleep(8_000u)
        frame++
    }

    // Final frame for the interactive run: what the UI looked like after all the injected input.
    if (!headless) writePng(surface, width, height, "/out/poc5-final.png")
    logln("POC5: exiting after $frame frames")

    scene.close()
    frameRecomposer.close()
    surface.close(); renderTarget.close(); context.close()
    glfwDestroyWindow(window); glfwTerminate()
}
