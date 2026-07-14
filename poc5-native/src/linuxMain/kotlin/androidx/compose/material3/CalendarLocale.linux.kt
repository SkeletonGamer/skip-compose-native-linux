// Linux actual for the DatePicker's locale. macOS aliases NSLocale; here it carries a language tag,
// taken from the POSIX locale environment (LC_TIME first, since dates are what this drives, then the
// general locale) instead of being hardcoded to en-US.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import kotlinx.cinterop.toKString
import platform.posix.getenv

actual class CalendarLocale internal constructor(internal val languageTag: String)

private fun systemDateLanguageTag(): String {
    for (name in listOf("LC_ALL", "LC_TIME", "LANG")) {
        val raw = getenv(name)?.toKString()?.takeIf { it.isNotBlank() } ?: continue
        val bare = raw.substringBefore('.').substringBefore('@')
        if (bare.isEmpty() || bare == "C" || bare == "POSIX") continue
        return bare.replace('_', '-')
    }
    return "en-US"
}

@Composable
@ReadOnlyComposable
internal actual fun defaultLocale(): CalendarLocale = CalendarLocale(systemDateLanguageTag())
