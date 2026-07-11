// POC 6 : cales compile-only pour la surface android.icu.util referencee par
// le code Kotlin transpile (SkipFoundation). Ces types n'existent pas en
// Kotlin/Native Linux ; on reproduit ici juste les signatures appelees, corps = TODO.
package android.icu.util

// java.util.Locale et java.util.Date sont fournis par un autre shim : on les qualifie.

// Utilise par RelativeDateTimeFormatter (forLocale / getDefault) et comme parametre
// de fabriques ICU. On garde la surface minimale reellement referencee, plus les
// membres attendus par la fabrique (constructeur String, toLocale).
class ULocale(localeID: String) {

    // Conversion vers une Locale JDK (non referencee dans les fichiers vus, gardee par surete).
    fun toLocale(): java.util.Locale = TODO("K/N android.icu stub")

    companion object {
        // RelativeDateTimeFormatter.updatePlatformValue : forLocale(locale.platformValue).
        fun forLocale(locale: java.util.Locale): ULocale = TODO("K/N android.icu stub")

        // RelativeDateTimeFormatter.updatePlatformValue : branche else quand locale est nul.
        fun getDefault(): ULocale = TODO("K/N android.icu stub")
    }
}

// Utilise par NumberFormatter (symbols.currency = Currency.getInstance(loc)).
class Currency {

    fun getCurrencyCode(): String = TODO("K/N android.icu stub")

    fun getSymbol(): String = TODO("K/N android.icu stub")

    fun getSymbol(locale: java.util.Locale): String = TODO("K/N android.icu stub")

    companion object {
        // NumberFormatter : Currency.getInstance(loc) ou loc est une java.util.Locale.
        fun getInstance(locale: java.util.Locale): Currency = TODO("K/N android.icu stub")

        // Surcharge par code ISO (non referencee dans les fichiers vus, gardee par surete).
        fun getInstance(currencyCode: String): Currency = TODO("K/N android.icu stub")
    }
}
