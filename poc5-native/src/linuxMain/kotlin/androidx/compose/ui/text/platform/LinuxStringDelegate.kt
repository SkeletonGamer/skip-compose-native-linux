// Linux actual for ui-text's platform string ops (darwin uses NSString/ICU).
//
// The locale parameter used to be ignored, which quietly breaks the languages where case mapping is not
// context-free: in Turkish, uppercasing "i" must give "İ" (dotted), not "I". ICU does this correctly when
// it is present; the Kotlin stdlib (root locale) is the fallback.
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.Locale
import icu.IcuApi

private fun Locale.icuId(): String = toLanguageTag().replace('-', '_')

internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    object : PlatformStringDelegate {
        override fun toUpperCase(string: String, locale: Locale): String =
            IcuApi.uppercase(string, locale.icuId()) ?: string.uppercase()

        override fun toLowerCase(string: String, locale: Locale): String =
            IcuApi.lowercase(string, locale.icuId()) ?: string.lowercase()

        override fun capitalize(string: String, locale: Locale): String =
            if (string.isEmpty()) string
            else toUpperCase(string.substring(0, 1), locale) + string.substring(1)

        override fun decapitalize(string: String, locale: Locale): String =
            if (string.isEmpty()) string
            else toLowerCase(string.substring(0, 1), locale) + string.substring(1)
    }
