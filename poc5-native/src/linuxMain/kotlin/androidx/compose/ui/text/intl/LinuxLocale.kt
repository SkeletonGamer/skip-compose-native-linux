// Linux actuals for ui-text's i18n expects, backed by the POSIX locale environment.
//
// The locale now comes from the environment the way every Linux program gets it (LC_ALL, then LC_MESSAGES,
// then LANG), instead of being hardcoded to en-US. Text direction is resolved from a table of RTL
// languages: the ISO 639 list of right-to-left scripts is short and stable, and it is exactly what the
// desktop backend derives from AWT's ComponentOrientation (whose own code carries a TODO asking to drop
// the AWT dependency for this).
//
// Not covered: full BCP-47 (grandfathered/extension subtags) and locale-aware collation, which would need
// ICU. Parsing here handles the forms POSIX actually emits: "fr_FR.UTF-8", "ar_EG", "zh_Hans_CN", "C".
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package androidx.compose.ui.text.intl

import icu.IcuApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

// Languages written right-to-left (ISO 639-1/639-3). Arabic, Hebrew, Persian, Urdu, Syriac, Divehi,
// Kurdish (Sorani), Pashto, Sindhi, Uyghur, Yiddish, N'Ko, Samaritan, Mandaic, Rohingya.
private val RTL_LANGUAGES = setOf(
    "ar", "he", "iw", "fa", "ur", "syr", "dv", "ckb", "ps", "sd", "ug", "yi", "ji", "nqo", "smp", "mid", "rhg",
)

// Scripts written right-to-left, when the tag carries one explicitly (e.g. "az-Arab").
private val RTL_SCRIPTS = setOf("Arab", "Hebr", "Syrc", "Thaa", "Nkoo", "Adlm", "Mand", "Samr")

/** Reads the POSIX locale environment, most specific first. Returns null when unset or set to C/POSIX. */
private fun systemLanguageTag(): String? {
    for (name in listOf("LC_ALL", "LC_MESSAGES", "LANG")) {
        val raw = getenv(name)?.toKString()?.takeIf { it.isNotBlank() } ?: continue
        // "fr_FR.UTF-8@euro" -> "fr_FR"
        val bare = raw.substringBefore('.').substringBefore('@')
        if (bare.isEmpty() || bare == "C" || bare == "POSIX") continue
        return bare.replace('_', '-')
    }
    return null
}

actual class Locale actual constructor(private val languageTag: String) {
    actual companion object {
        // Fall back to en-US when the environment says nothing (or says C/POSIX), which is what a Linux
        // desktop app does.
        actual val current: Locale get() = Locale(systemLanguageTag() ?: "en-US")
    }

    private val parts = languageTag.split("-", "_")

    actual val language: String get() = parts.getOrElse(0) { "" }
    actual val script: String get() = parts.firstOrNull { it.length == 4 } ?: ""
    actual val region: String get() = parts.drop(1).firstOrNull { it.length == 2 } ?: ""

    actual fun toLanguageTag(): String = languageTag

    actual override fun equals(other: Any?): Boolean = other is Locale && other.languageTag == languageTag
    actual override fun hashCode(): Int = languageTag.hashCode()
    actual override fun toString(): String = languageTag
}

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: LocaleList get() = LocaleList(listOf(Locale.current))
    }

internal actual fun Locale.isRtl(): Boolean = isRightToLeft()

/**
 * Same test, reachable from the mediator: it has to pick the scene's LayoutDirection, and Compose's own
 * isRtl() is internal to ui-text. The desktop backend derives this from AWT's ComponentOrientation.
 *
 * ICU answers from CLDR when present. The table is the fallback: it covers the RTL languages and scripts,
 * which are few and stable, but it will not know about a locale CLDR has and it does not.
 */
fun Locale.isRightToLeft(): Boolean {
    IcuApi.isRtl(toLanguageTag().replace('-', '_'))?.let { return it }
    // An explicit RTL script wins over the language (e.g. "az-Arab" is RTL, plain "az" is not).
    return script.takeIf { it.isNotEmpty() }?.let { it in RTL_SCRIPTS }
        ?: (language.lowercase() in RTL_LANGUAGES)
}
