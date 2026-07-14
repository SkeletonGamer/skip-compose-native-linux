// GLFW -> Compose input translation.
//
// GLFW callbacks are staticCFunction: they capture nothing, so they cannot touch the mediator's locals.
// They push into the global queue below, which the frame loop drains. GLFW dispatches callbacks from
// glfwPollEvents on the calling thread, so this is single-threaded by construction.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package glfwinput

import androidx.compose.ui.input.key.Key
import glfw.*

/** What the callbacks record, replayed into Compose by the mediator. */
sealed interface InputEvent {
    data class KeyPress(val key: Int, val action: Int, val mods: Int) : InputEvent
    data class Typed(val codePoint: Int) : InputEvent
    data class Scroll(val dx: Double, val dy: Double) : InputEvent
    data class MouseButton(val button: Int, val action: Int) : InputEvent
    data class MouseMove(val x: Double, val y: Double) : InputEvent
    data class Resize(val width: Int, val height: Int) : InputEvent
    data class Focus(val focused: Boolean) : InputEvent
    /** Files dropped onto the window. GLFW gives absolute paths. */
    data class FileDrop(val paths: List<String>) : InputEvent
}

object InputQueue {
    private val events = mutableListOf<InputEvent>()

    fun push(event: InputEvent) { events.add(event) }

    /** Hands over everything queued since the last frame and clears the queue. */
    fun drain(): List<InputEvent> {
        if (events.isEmpty()) return emptyList()
        val out = events.toList()
        events.clear()
        return out
    }
}

/**
 * GLFW key code -> Compose Key.
 *
 * Compose only ever compares keys against its own constants (Key.Backspace, Key.C, ...), never against
 * raw numbers, so the constants' underlying values are irrelevant: what matters is that a GLFW key maps
 * to the Key that Compose expects. That is why the ~300 constants of Key.linux.kt do not need rewriting,
 * even though they currently carry Apple key codes.
 */
fun glfwKeyToCompose(glfwKey: Int): Key = when (glfwKey) {
    GLFW_KEY_A -> Key.A; GLFW_KEY_B -> Key.B; GLFW_KEY_C -> Key.C; GLFW_KEY_D -> Key.D
    GLFW_KEY_E -> Key.E; GLFW_KEY_F -> Key.F; GLFW_KEY_G -> Key.G; GLFW_KEY_H -> Key.H
    GLFW_KEY_I -> Key.I; GLFW_KEY_J -> Key.J; GLFW_KEY_K -> Key.K; GLFW_KEY_L -> Key.L
    GLFW_KEY_M -> Key.M; GLFW_KEY_N -> Key.N; GLFW_KEY_O -> Key.O; GLFW_KEY_P -> Key.P
    GLFW_KEY_Q -> Key.Q; GLFW_KEY_R -> Key.R; GLFW_KEY_S -> Key.S; GLFW_KEY_T -> Key.T
    GLFW_KEY_U -> Key.U; GLFW_KEY_V -> Key.V; GLFW_KEY_W -> Key.W; GLFW_KEY_X -> Key.X
    GLFW_KEY_Y -> Key.Y; GLFW_KEY_Z -> Key.Z

    GLFW_KEY_0 -> Key.Zero; GLFW_KEY_1 -> Key.One; GLFW_KEY_2 -> Key.Two
    GLFW_KEY_3 -> Key.Three; GLFW_KEY_4 -> Key.Four; GLFW_KEY_5 -> Key.Five
    GLFW_KEY_6 -> Key.Six; GLFW_KEY_7 -> Key.Seven; GLFW_KEY_8 -> Key.Eight
    GLFW_KEY_9 -> Key.Nine

    GLFW_KEY_SPACE -> Key.Spacebar
    GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> Key.Enter
    GLFW_KEY_TAB -> Key.Tab
    GLFW_KEY_BACKSPACE -> Key.Backspace
    GLFW_KEY_DELETE -> Key.Delete
    GLFW_KEY_ESCAPE -> Key.Escape
    GLFW_KEY_INSERT -> Key.Insert

    GLFW_KEY_LEFT -> Key.DirectionLeft
    GLFW_KEY_RIGHT -> Key.DirectionRight
    GLFW_KEY_UP -> Key.DirectionUp
    GLFW_KEY_DOWN -> Key.DirectionDown
    GLFW_KEY_HOME -> Key.MoveHome
    GLFW_KEY_END -> Key.MoveEnd
    GLFW_KEY_PAGE_UP -> Key.PageUp
    GLFW_KEY_PAGE_DOWN -> Key.PageDown

    GLFW_KEY_LEFT_SHIFT -> Key.ShiftLeft; GLFW_KEY_RIGHT_SHIFT -> Key.ShiftRight
    GLFW_KEY_LEFT_CONTROL -> Key.CtrlLeft; GLFW_KEY_RIGHT_CONTROL -> Key.CtrlRight
    GLFW_KEY_LEFT_ALT -> Key.AltLeft; GLFW_KEY_RIGHT_ALT -> Key.AltRight
    GLFW_KEY_LEFT_SUPER -> Key.MetaLeft; GLFW_KEY_RIGHT_SUPER -> Key.MetaRight

    GLFW_KEY_MINUS -> Key.Minus; GLFW_KEY_EQUAL -> Key.Equals
    GLFW_KEY_COMMA -> Key.Comma; GLFW_KEY_PERIOD -> Key.Period
    GLFW_KEY_SLASH -> Key.Slash; GLFW_KEY_BACKSLASH -> Key.Backslash
    GLFW_KEY_SEMICOLON -> Key.Semicolon; GLFW_KEY_APOSTROPHE -> Key.Apostrophe
    GLFW_KEY_LEFT_BRACKET -> Key.LeftBracket; GLFW_KEY_RIGHT_BRACKET -> Key.RightBracket
    GLFW_KEY_GRAVE_ACCENT -> Key.Grave

    else -> Key.Unknown
}

// GLFW modifier bitmask (GLFW_MOD_*) accessors.
fun Int.hasShift(): Boolean = this and GLFW_MOD_SHIFT != 0
fun Int.hasCtrl(): Boolean = this and GLFW_MOD_CONTROL != 0
fun Int.hasAlt(): Boolean = this and GLFW_MOD_ALT != 0
fun Int.hasSuper(): Boolean = this and GLFW_MOD_SUPER != 0
