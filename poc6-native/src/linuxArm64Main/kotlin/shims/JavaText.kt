// POC 6 Jalon 4 : surface API java.text que SkipFoundation reference, en shim compile-only K/N
// (l'equivalent natif d'un android.jar : signatures et types de retour corrects, corps stubes).
// Un portage de production adosserait ces types a ICU4C / kotlinx-datetime.
package java.text

// Formatage de date/heure. SimpleDateFormat en herite (voir plus bas).
// SkipFoundation lit/ecrit timeZone, calendar, isLenient ; appelle format/parse ; et instancie
// via les fabriques statiques getDateInstance/getTimeInstance/getDateTimeInstance.
open class DateFormat {
    open var timeZone: java.util.TimeZone = java.util.TimeZone.getDefault()
    open fun getTimeZone(): java.util.TimeZone = timeZone
    open fun setTimeZone(value: java.util.TimeZone) { timeZone = value }

    open var calendar: java.util.Calendar = java.util.Calendar.getInstance()
    open var isLenient: Boolean = true

    open fun format(date: java.util.Date): String = TODO("K/N java.text stub")
    open fun parse(source: String): java.util.Date = TODO("K/N java.text stub")
    override fun toString(): String = TODO("K/N java.text stub")

    companion object {
        // Vraies valeurs des styles java.text.DateFormat (DEFAULT est un alias de MEDIUM).
        const val FULL: Int = 0
        const val LONG: Int = 1
        const val MEDIUM: Int = 2
        const val SHORT: Int = 3
        const val DEFAULT: Int = 2

        fun getDateInstance(style: Int): DateFormat = TODO("K/N java.text stub")
        fun getDateInstance(style: Int, locale: java.util.Locale): DateFormat = TODO("K/N java.text stub")
        fun getTimeInstance(style: Int): DateFormat = TODO("K/N java.text stub")
        fun getTimeInstance(style: Int, locale: java.util.Locale): DateFormat = TODO("K/N java.text stub")
        fun getDateTimeInstance(dateStyle: Int, timeStyle: Int): DateFormat = TODO("K/N java.text stub")
        fun getDateTimeInstance(dateStyle: Int, timeStyle: Int, locale: java.util.Locale): DateFormat = TODO("K/N java.text stub")
    }
}

// Formateur base sur un motif. SkipFoundation lit le motif via toPattern/toLocalizedPattern
// et le pose via applyPattern.
open class SimpleDateFormat : DateFormat {
    constructor() : super()
    constructor(pattern: String) : super()
    constructor(pattern: String, locale: java.util.Locale) : super()

    open fun toPattern(): String = TODO("K/N java.text stub")
    open fun toLocalizedPattern(): String = TODO("K/N java.text stub")
    open fun applyPattern(pattern: String): Unit = TODO("K/N java.text stub")
    open fun applyLocalizedPattern(pattern: String): Unit = TODO("K/N java.text stub")
}

// Noms localises des mois, jours et AM/PM. SkipFoundation (Calendar) lit ces tableaux.
open class DateFormatSymbols {
    open fun getMonths(): Array<String> = TODO("K/N java.text stub")
    open fun getShortMonths(): Array<String> = TODO("K/N java.text stub")
    open fun getWeekdays(): Array<String> = TODO("K/N java.text stub")
    open fun getShortWeekdays(): Array<String> = TODO("K/N java.text stub")
    open fun getAmPmStrings(): Array<String> = TODO("K/N java.text stub")
    open fun getEras(): Array<String> = TODO("K/N java.text stub")

    companion object {
        fun getInstance(locale: java.util.Locale): DateFormatSymbols = TODO("K/N java.text stub")
    }
}

// Comparaison de chaines sensible a la locale. SkipFoundation regle la finesse via setStrength
// puis compare deux chaines.
open class Collator {
    open fun setStrength(newStrength: Int): Unit = TODO("K/N java.text stub")
    open fun compare(source: String, target: String): Int = TODO("K/N java.text stub")

    companion object {
        // Vraies valeurs des niveaux de finesse java.text.Collator.
        const val SECONDARY: Int = 1
        const val TERTIARY: Int = 2

        fun getInstance(locale: java.util.Locale): Collator = TODO("K/N java.text stub")
    }
}

// Symboles numeriques localises. SkipFoundation lit les separateurs decimal et de milliers.
open class DecimalFormatSymbols {
    open fun getDecimalSeparator(): Char = TODO("K/N java.text stub")
    open fun getGroupingSeparator(): Char = TODO("K/N java.text stub")

    companion object {
        fun getInstance(locale: java.util.Locale): DecimalFormatSymbols = TODO("K/N java.text stub")
    }
}

// Formatage numerique. SkipFoundation n'en lit que la devise associee, via getCurrencyInstance.
open class NumberFormat {
    open val currency: java.util.Currency? get() = TODO("K/N java.text stub")

    companion object {
        fun getCurrencyInstance(locale: java.util.Locale): NumberFormat? = TODO("K/N java.text stub")
    }
}
