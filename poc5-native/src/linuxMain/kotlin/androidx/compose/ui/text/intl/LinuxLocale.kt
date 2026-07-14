// POC 5: minimal Linux actuals for ui-text's i18n platform expects (the darwin equivalent uses
// NSLocale; a real backend would use ICU/fontconfig). Stub-level: enough to compile + basic behaviour.
package androidx.compose.ui.text.intl

actual class Locale actual constructor(private val languageTag: String) {
    actual companion object {
        actual val current: Locale get() = Locale("en-US")
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
        override val current: LocaleList get() = LocaleList(listOf(Locale("en-US")))
    }

internal actual fun Locale.isRtl(): Boolean = false
