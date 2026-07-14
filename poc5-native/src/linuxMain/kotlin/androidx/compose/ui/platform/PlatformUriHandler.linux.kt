// Linux actual for opening URIs: hands off to xdg-open, the freedesktop standard.
//
// The JVM desktop backend does the same on Linux (PlatformUriHandler.desktop.kt falls back to
// Runtime.exec("xdg-open", uri)). fork + execvp rather than system(), so the uri never reaches a shell
// and cannot be turned into command injection.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.compose.ui.platform

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import platform.posix._exit
import platform.posix.execvp
import platform.posix.fork
import platform.posix.waitpid

private class LinuxUriHandler : UriHandler {
    override fun openUri(uri: String) {
        when (val pid = fork()) {
            -1 -> return // fork failed; nothing sensible to do here but stay alive
            0 -> {
                // Child: become xdg-open. execvp only returns if it failed.
                memScoped {
                    val argv = allocArray<CPointerVar<ByteVar>>(3)
                    argv[0] = "xdg-open".cstr.ptr
                    argv[1] = uri.cstr.ptr
                    argv[2] = null
                    execvp("xdg-open", argv)
                }
                _exit(127)
            }
            else -> {
                // Parent: reap the child so it does not linger as a zombie. xdg-open returns as soon as
                // the desktop has taken the uri, so this does not block on the browser starting.
                waitpid(pid, null, 0)
            }
        }
    }
}

internal actual fun createPlatformUriHandler(): UriHandler = LinuxUriHandler()
