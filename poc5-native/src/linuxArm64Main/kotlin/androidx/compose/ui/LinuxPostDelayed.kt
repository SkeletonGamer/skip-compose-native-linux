// POC 5 Jalon 4: Linux actual for compose.ui's postDelayed. The skiko actual launches on
// Dispatchers.Main, which is absent on Kotlin/Native Linux. Here we queue callbacks and let the
// GLFW frame loop drain the due ones on the compose thread (drivePostDelayed), which is exactly
// where RectManager's debounce callbacks belong.
package androidx.compose.ui

private class DelayedTask(val dueNanos: Long, val block: () -> Unit) {
    var cancelled = false
}

// Accessed only on the single compose/mediator thread, so a plain list is enough.
private val pendingTasks = mutableListOf<DelayedTask>()
private var frameClockNanos = 0L

internal actual fun postDelayed(delayMillis: Long, block: () -> Unit): Any {
    val task = DelayedTask(frameClockNanos + delayMillis * 1_000_000L, block)
    pendingTasks.add(task)
    return task
}

internal actual fun removePost(token: Any?) {
    (token as? DelayedTask)?.cancelled = true
}

/**
 * Runs the callbacks whose delay has elapsed, on the calling (compose) thread. The `ui-glfw`
 * mediator calls this once per frame with the current frame time.
 */
internal fun drivePostDelayed(nowNanos: Long) {
    frameClockNanos = nowNanos
    if (pendingTasks.isEmpty()) return
    val due = pendingTasks.filter { !it.cancelled && it.dueNanos <= nowNanos }
    pendingTasks.removeAll { it.cancelled || it.dueNanos <= nowNanos }
    due.forEach { it.block() }
}
