// ICU4C, loaded at runtime instead of linked at build time.
//
// Why not just link it: ICU renames every exported symbol with its major version. The library exports
// `udat_open_72`, never `udat_open` (the headers do the renaming with a macro). A binary linked against
// ICU 72 therefore fails to start on a distro shipping ICU 74, which is unacceptable for a binary meant to
// be shipped. So we dlopen the library, discover which version is actually installed, and resolve the
// suffixed symbols with dlsym.
//
// If no ICU is found, `available` stays false and every caller falls back to the built-in behaviour
// (English names, table-driven RTL). The app must run on a system without ICU.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package icu

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.reinterpret
import platform.posix.RTLD_LAZY
import platform.posix.dlopen
import platform.posix.dlsym

/** Newest first: prefer the most recent ICU present on the system. */
private val ICU_VERSIONS = (78 downTo 50).toList()

object Icu {
    private var i18n: COpaquePointer? = null   // libicui18n: formatting, calendars
    private var uc: COpaquePointer? = null     // libicuuc: locales, string ops

    /** The version suffix ICU renames its symbols with, e.g. "_72". Empty if the build disabled renaming. */
    var suffix: String = ""
        private set

    var version: Int = 0
        private set

    val available: Boolean get() = i18n != null && uc != null

    /** A one-line description for logs, so a run tells you whether it had ICU and which one. */
    val describe: String
        get() = if (available) "ICU $version (symbols suffixed \"$suffix\")" else "ICU not found (fallback)"

    init { load() }

    private fun load() {
        // Kill switch, so the fallback path can actually be exercised. It cannot be tested by deleting the
        // ICU libraries: mesa's DRI drivers pull libicu in through libxml2, so removing it breaks GL and
        // the window never opens. On any Linux box that renders with GL, ICU is present regardless.
        if (platform.posix.getenv("POC5_DISABLE_ICU") != null) return

        for (v in ICU_VERSIONS) {
            val i = dlopen("libicui18n.so.$v", RTLD_LAZY) ?: continue
            val u = dlopen("libicuuc.so.$v", RTLD_LAZY) ?: continue
            // Confirm the suffix really is what the soname suggests: a library could ship unsuffixed
            // symbols (U_DISABLE_RENAMING). Probe a symbol that exists in every ICU build.
            val suffixed = dlsym(u, "u_errorName_$v") != null
            val plain = dlsym(u, "u_errorName") != null
            if (!suffixed && !plain) continue
            i18n = i
            uc = u
            version = v
            suffix = if (suffixed) "_$v" else ""
            return
        }
        // Last resort: an unversioned soname (typical of a -dev install or a bundled ICU).
        val i = dlopen("libicui18n.so", RTLD_LAZY) ?: return
        val u = dlopen("libicuuc.so", RTLD_LAZY) ?: return
        if (dlsym(u, "u_errorName") == null) return
        i18n = i
        uc = u
        suffix = ""
    }

    /** Resolves `name` + the version suffix from libicui18n. Null when absent. */
    fun <F : CFunction<*>> i18nFun(name: String): CPointer<F>? =
        i18n?.let { dlsym(it, name + suffix)?.reinterpret() }

    /** Resolves `name` + the version suffix from libicuuc. Null when absent. */
    fun <F : CFunction<*>> ucFun(name: String): CPointer<F>? =
        uc?.let { dlsym(it, name + suffix)?.reinterpret() }
}
