// POC 6 : cales compile-only pour la surface android.icu.text referencee par
// le code Kotlin transpile (SkipFoundation : NumberFormatter, RelativeDateTimeFormatter,
// Formatter, String). Ces types n'existent pas en Kotlin/Native Linux ; on reproduit
// ici uniquement les signatures et types de retour reellement appeles, corps = TODO.
package android.icu.text

// java.util.Locale, java.util.Date et java.math.BigDecimal sont fournis par d'autres
// shims : on les qualifie sans les recreer. android.icu.util.Currency / ULocale sont
// dans AndroidIcuUtil.kt : on les qualifie de meme.

// Utilise par NumberFormatter (platformValue) et FormatStyle (formatter).
// Toutes les fabriques renvoient un DecimalFormat (le code fait `as DecimalFormat`).
class DecimalFormat {

    // Constructeurs listes par la tache (non construits directement dans les fichiers vus,
    // le code passe par les fabriques ; gardes pour la surface API).
    constructor(pattern: String)
    constructor(pattern: String, symbols: DecimalFormatSymbols)

    // NumberFormatter.description : platformValue.description.
    val description: String
        get() = TODO("K/N android.icu stub")

    // NumberFormatter.multiplier : lu en Int (caste en Number), ecrit avec un Int.
    var multiplier: Int
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var positivePrefix: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var negativePrefix: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var positiveSuffix: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var negativeSuffix: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var maximumFractionDigits: Int
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var minimumFractionDigits: Int
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var maximumIntegerDigits: Int
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var minimumIntegerDigits: Int
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    // Accede a la fois en propriete (platformValue.decimalFormatSymbols) et via
    // getDecimalFormatSymbols()/setDecimalFormatSymbols() plus bas.
    var decimalFormatSymbols: DecimalFormatSymbols
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    fun getGroupingSize(): Int = TODO("K/N android.icu stub")
    fun setGroupingSize(newValue: Int): Unit = TODO("K/N android.icu stub")

    fun isParseBigDecimal(): Boolean = TODO("K/N android.icu stub")
    fun setParseBigDecimal(newValue: Boolean): Unit = TODO("K/N android.icu stub")

    fun isDecimalSeparatorAlwaysShown(): Boolean = TODO("K/N android.icu stub")
    fun setDecimalSeparatorAlwaysShown(newValue: Boolean): Unit = TODO("K/N android.icu stub")

    fun isGroupingUsed(): Boolean = TODO("K/N android.icu stub")
    fun setGroupingUsed(newValue: Boolean): Unit = TODO("K/N android.icu stub")

    // NumberFormatter.applySymbol : getDecimalFormatSymbols() renvoie une copie nullable,
    // re-injectee via setDecimalFormatSymbols().
    fun getDecimalFormatSymbols(): DecimalFormatSymbols? = TODO("K/N android.icu stub")
    fun setDecimalFormatSymbols(symbols: DecimalFormatSymbols): Unit = TODO("K/N android.icu stub")

    fun applyPattern(pattern: String): Unit = TODO("K/N android.icu stub")
    fun applyLocalizedPattern(pattern: String): Unit = TODO("K/N android.icu stub")
    fun toPattern(): String = TODO("K/N android.icu stub")
    fun toLocalizedPattern(): String = TODO("K/N android.icu stub")

    // format(Number) est la forme reellement appelee ; Double/Long sont fournis par la tache.
    fun format(number: Number): String = TODO("K/N android.icu stub")
    fun format(number: Double): String = TODO("K/N android.icu stub")
    fun format(number: Long): String = TODO("K/N android.icu stub")

    // NumberFormatter.number(from:) : parse(string) as? Number.
    fun parse(text: String): Number? = TODO("K/N android.icu stub")

    companion object {
        fun getIntegerInstance(): DecimalFormat = TODO("K/N android.icu stub")
        fun getIntegerInstance(locale: java.util.Locale): DecimalFormat = TODO("K/N android.icu stub")

        fun getNumberInstance(): DecimalFormat = TODO("K/N android.icu stub")
        fun getNumberInstance(locale: java.util.Locale): DecimalFormat = TODO("K/N android.icu stub")

        fun getCurrencyInstance(): DecimalFormat = TODO("K/N android.icu stub")
        fun getCurrencyInstance(locale: java.util.Locale): DecimalFormat = TODO("K/N android.icu stub")

        fun getPercentInstance(): DecimalFormat = TODO("K/N android.icu stub")
        fun getPercentInstance(locale: java.util.Locale): DecimalFormat = TODO("K/N android.icu stub")

        fun getScientificInstance(): DecimalFormat = TODO("K/N android.icu stub")
        fun getScientificInstance(locale: java.util.Locale): DecimalFormat = TODO("K/N android.icu stub")
    }
}

// Utilise par NumberFormatter via platformValue.decimalFormatSymbols.
// Les separateurs sont des Char (parfois nullables selon l'acces vu dans le grep),
// les symboles textuels des String.
class DecimalFormatSymbols {

    // Constructeurs listes par la tache (localises).
    constructor(locale: java.util.Locale)
    constructor(locale: android.icu.util.ULocale)

    var currency: android.icu.util.Currency
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var groupingSeparator: Char
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var percent: Char
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var zeroDigit: Char?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var minusSign: Char?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var decimalSeparator: Char?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var monetaryDecimalSeparator: Char?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var monetaryGroupingSeparator: Char?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var currencySymbol: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var internationalCurrencySymbol: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    var exponentSeparator: String?
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    // NumberFormatter : symbole unique d'infini (positif et negatif), non nul.
    var infinity: String
        get() = TODO("K/N android.icu stub")
        set(value) { TODO("K/N android.icu stub") }

    // Appeles en methodes (getNaN()/setNaN()), pas en propriete, dans NumberFormatter.
    fun getNaN(): String = TODO("K/N android.icu stub")
    fun setNaN(value: String?): Unit = TODO("K/N android.icu stub")
}

// Utilise par RelativeDateTimeFormatter (platformValue). Les enums imbriques sont
// references en qualifie (RelativeDateTimeFormatter.Style.SHORT) et via import
// (RelativeDateTimeFormatter.AbsoluteUnit / Direction / RelativeUnit) : donc imbriques.
class RelativeDateTimeFormatter {

    // format(Direction, AbsoluteUnit) : cas "absolu" (NOW, DAY, ...).
    fun format(direction: Direction, unit: AbsoluteUnit): String = TODO("K/N android.icu stub")

    // format(Double, Direction, RelativeUnit) : cas "relatif" quantifie.
    fun format(quantity: Double, direction: Direction, unit: RelativeUnit): String = TODO("K/N android.icu stub")

    enum class Direction {
        PLAIN,
        LAST,
        NEXT,
        THIS
    }

    enum class AbsoluteUnit {
        YEAR,
        MONTH,
        WEEK,
        DAY,
        NOW
    }

    enum class RelativeUnit {
        YEARS,
        MONTHS,
        WEEKS,
        DAYS,
        HOURS,
        MINUTES,
        SECONDS
    }

    enum class Style {
        LONG,
        SHORT
    }

    companion object {
        // updatePlatformValue : getInstance(ulocale, null, style, capitalization).
        // Le second parametre (NumberFormat en ICU) est passe null : type nullable large.
        fun getInstance(
            locale: android.icu.util.ULocale,
            numberFormat: Any?,
            style: Style,
            capitalizationContext: DisplayContext
        ): RelativeDateTimeFormatter = TODO("K/N android.icu stub")
    }
}

// Utilise par Formatter.Context.capitalization. Enum de premier niveau (acces DisplayContext.X).
enum class DisplayContext {
    CAPITALIZATION_NONE,
    CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE,
    CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
    CAPITALIZATION_FOR_UI_LIST_OR_MENU,
    CAPITALIZATION_FOR_STANDALONE
}

// Utilise par String.localizedStringWithFormat : MessageFormat(format, locale).format(args).
class MessageFormat {
    constructor(pattern: String, locale: java.util.Locale)

    fun format(arguments: Array<out Any?>): String = TODO("K/N android.icu stub")
}
