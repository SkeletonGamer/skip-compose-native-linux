// POC 6 Jalon 4: java.time.format surface (compile-only K/N stub).
package java.time.format

import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalField

enum class ResolverStyle { STRICT, SMART, LENIENT }

enum class TextStyle { FULL, FULL_STANDALONE, SHORT, SHORT_STANDALONE, NARROW, NARROW_STANDALONE }

class DateTimeParseException(message: String) : RuntimeException(message)

class DateTimeFormatter {
    fun format(temporal: TemporalAccessor): String = TODO("K/N java.time.format stub")
    fun parse(text: CharSequence): TemporalAccessor = TODO("K/N java.time.format stub")
    fun withResolverStyle(resolverStyle: ResolverStyle): DateTimeFormatter = TODO("K/N java.time.format stub")
    fun withLocale(locale: java.util.Locale): DateTimeFormatter = TODO("K/N java.time.format stub")
    fun withZone(zone: java.time.ZoneId): DateTimeFormatter = TODO("K/N java.time.format stub")

    companion object {
        fun ofPattern(pattern: String): DateTimeFormatter = TODO("K/N java.time.format stub")
        fun ofPattern(pattern: String, locale: java.util.Locale): DateTimeFormatter = TODO("K/N java.time.format stub")
    }
}

class DateTimeFormatterBuilder {
    fun appendValue(field: TemporalField): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendValue(field: TemporalField, width: Int): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendValue(field: TemporalField, minWidth: Int, maxWidth: Int, signStyle: Any?): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendLiteral(literal: String): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendLiteral(literal: Char): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendPattern(pattern: String): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendOffset(pattern: String, noOffsetText: String): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendInstant(): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun appendFraction(field: TemporalField, minWidth: Int, maxWidth: Int, decimalPoint: Boolean): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun parseCaseInsensitive(): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun optionalStart(): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun optionalEnd(): DateTimeFormatterBuilder = TODO("K/N java.time.format stub")
    fun toFormatter(): DateTimeFormatter = TODO("K/N java.time.format stub")
    fun toFormatter(locale: java.util.Locale): DateTimeFormatter = TODO("K/N java.time.format stub")
}
