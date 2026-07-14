// IME probe: speak zwp_text_input_v3 to the Wayland compositor.
//
// The point being tested. Under Wayland the application does NOT talk to IBus. It speaks text-input-v3 to
// the COMPOSITOR, and the compositor relays to whatever input method is running (over input-method-v2).
// Verified on our wlroots compositor: it advertises zwp_text_input_manager_v3 and zwp_input_method_v2.
// JetBrains has the same problem open as JBR-5672.
//
// The tricky part is that GLFW owns the Wayland connection. We reuse its wl_display (glfwGetWaylandDisplay)
// and its wl_surface (glfwGetWaylandWindow), open our own registry on that same display to bind the
// text-input manager and the seat, and drive the protocol ourselves. GLFW keeps dispatching the display in
// its event loop, so our listeners fire from glfwPollEvents.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package waylandime

import glfw.glfwGetWaylandDisplay
import glfw.glfwGetWaylandWindow
import kotlinx.cinterop.*
import waylandtext.*

/** What the compositor sent back. The probe's whole point is that these fill up. */
object ImeState {
    var available = false          // did we find text-input-v3 on this compositor?
    var enabled = false            // did enable() go out?
    val preedits = mutableListOf<String>()   // text being composed (underlined in a real UI)
    val commits = mutableListOf<String>()    // text the IME finally committed
    var enteredSurface = false     // compositor told us the surface has text focus
}

// Opaque Wayland objects land in cnames.structs, same as GLFWwindow.
private var manager: CPointer<cnames.structs.zwp_text_input_manager_v3>? = null
private var seat: CPointer<cnames.structs.wl_seat>? = null
private var textInput: CPointer<cnames.structs.zwp_text_input_v3>? = null

// Listeners must outlive the call, so they live in nativeHeap, not memScoped.
private val registryListener = nativeHeap.alloc<wl_registry_listener>()
private val textInputListener = nativeHeap.alloc<zwp_text_input_v3_listener>()

private fun log(msg: String) { println(msg); platform.posix.fflush(null) }

/**
 * Binds text-input-v3 on the display GLFW already owns. Returns false when the compositor does not offer
 * the protocol (in which case a real implementation would fall back to XIM/IBus on X11).
 */
fun initWaylandTextInput(): Boolean {
    val display = glfwGetWaylandDisplay() ?: run {
        log("IME: not on Wayland (no wl_display) -- nothing to do")
        return false
    }
    val registry = wl_display_get_registry(display.reinterpret()) ?: return false

    // Registry callbacks are staticCFunction: they capture nothing, hence the globals above.
    registryListener.global = staticCFunction { _, reg, name, iface, version ->
        val id = iface?.toKString() ?: return@staticCFunction
        when (id) {
            "zwp_text_input_manager_v3" ->
                manager = ti_manager_bind(reg, name, minOf(version, 1u))
            "wl_seat" ->
                seat = ti_seat_bind(reg, name, minOf(version, 7u))
        }
    }
    registryListener.global_remove = staticCFunction { _, _, _ -> }
    ti_registry_add_listener(registry, registryListener.ptr, null)

    // Round-trip twice: the first flushes the registry advertisements, the second the binds.
    wl_display_roundtrip(display.reinterpret())
    wl_display_roundtrip(display.reinterpret())

    val mgr = manager
    val st = seat
    if (mgr == null || st == null) {
        log("IME: compositor does not advertise text-input-v3 (manager=$mgr seat=$st)")
        return false
    }
    log("IME: bound zwp_text_input_manager_v3 + wl_seat")

    val ti = ti_get_text_input(mgr, st) ?: return false
    textInput = ti

    // These are what a real IME integration feeds back into Compose's PlatformTextInputService.
    textInputListener.enter = staticCFunction { _, _, _ ->
        ImeState.enteredSurface = true
        println("IME: enter (the compositor gave this surface text focus)"); platform.posix.fflush(null)
    }
    textInputListener.leave = staticCFunction { _, _, _ ->
        ImeState.enteredSurface = false
    }
    textInputListener.preedit_string = staticCFunction { _, _, text, _, _ ->
        val s = text?.toKString() ?: ""
        ImeState.preedits.add(s)
        println("IME: preedit_string='$s'  (text being composed)"); platform.posix.fflush(null)
    }
    textInputListener.commit_string = staticCFunction { _, _, text ->
        val s = text?.toKString() ?: ""
        ImeState.commits.add(s)
        println("IME: commit_string='$s'  (IME committed this text)"); platform.posix.fflush(null)
    }
    textInputListener.delete_surrounding_text = staticCFunction { _, _, _, _ -> }
    textInputListener.done = staticCFunction { _, _, _ -> }
    ti_add_listener(ti, textInputListener.ptr, null)

    ImeState.available = true
    log("IME: created zwp_text_input_v3")
    return true
}

/** Tells the compositor this surface wants input. Without enable+commit no IME will ever engage. */
fun enableWaylandTextInput(cursorX: Int, cursorY: Int, cursorW: Int, cursorH: Int) {
    val ti = textInput ?: return
    ti_enable(ti)
    // Where to put the candidate window. A real integration feeds Compose's cursor rect here.
    ti_set_cursor_rectangle(ti, cursorX, cursorY, cursorW, cursorH)
    ti_commit(ti)   // nothing takes effect until commit
    ImeState.enabled = true
    log("IME: enable + commit sent to the compositor")
}

fun disableWaylandTextInput() {
    val ti = textInput ?: return
    ti_disable(ti)
    ti_commit(ti)
    ImeState.enabled = false
}
