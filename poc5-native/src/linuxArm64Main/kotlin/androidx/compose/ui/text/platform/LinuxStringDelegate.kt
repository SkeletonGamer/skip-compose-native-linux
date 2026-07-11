// POC 5: minimal Linux actual for ui-text's platform string ops (darwin uses NSString/ICU).
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.Locale

internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    object : PlatformStringDelegate {
        override fun toUpperCase(string: String, locale: Locale): String = string.uppercase()
        override fun toLowerCase(string: String, locale: Locale): String = string.lowercase()
        override fun capitalize(string: String, locale: Locale): String =
            string.replaceFirstChar { it.uppercase() }
        override fun decapitalize(string: String, locale: Locale): String =
            string.replaceFirstChar { it.lowercase() }
    }
