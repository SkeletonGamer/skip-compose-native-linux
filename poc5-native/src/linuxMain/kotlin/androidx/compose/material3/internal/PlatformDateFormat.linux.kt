// Linux actual for the DatePicker's date formatter. macOS leans on NSDateFormatter; this is written on
// kotlinx-datetime.
//
// What the locale now drives: first day of week, 12h vs 24h clock, and the order of the date input fields
// (region-derived, following CLDR territory data). What it still does NOT drive: the month and weekday
// NAMES, which stay English. Those need CLDR data, i.e. ICU4C, and no amount of arithmetic substitutes
// for it. A locale-aware DatePicker therefore still needs the ICU work (see FINDINGS, Lot 3).
@file:OptIn(kotlin.time.ExperimentalTime::class)

package androidx.compose.material3.internal

import androidx.compose.material3.CalendarLocale
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val WEEKDAYS = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
)

// Regions where the week starts on Sunday, and where the 12-hour clock is the norm. These follow the
// CLDR territory data. Deriving them from the region is the part of localization that does NOT need ICU;
// the month and weekday NAMES do, which is why they are still English below.
private val SUNDAY_FIRST_REGIONS = setOf(
    "US", "CA", "JP", "IL", "KR", "TW", "HK", "MO", "BR", "MX", "CO", "PE", "VE", "AR", "CL",
    "PH", "IN", "ZA", "EG", "SA", "AE", "JO", "KW", "QA", "BH", "OM", "YE", "IQ", "SY", "LY",
)
private val TWELVE_HOUR_REGIONS = setOf(
    "US", "CA", "AU", "NZ", "PH", "IN", "PK", "BD", "EG", "SA", "MY", "GB", "IE", "MX", "CO",
)

internal actual class PlatformDateFormat actual constructor(private val locale: CalendarLocale) {

    // "fr-FR" -> "FR". Empty when the tag carries no region ("fr").
    private val region: String
        get() = locale.languageTag.split("-", "_").drop(1).firstOrNull { it.length == 2 }?.uppercase() ?: ""

    // ISO 1..7 (Monday == 1). Used to be hardcoded to Monday, which contradicted the US date format the
    // same class returned.
    actual val firstDayOfWeek: Int get() = if (region in SUNDAY_FIRST_REGIONS) 7 else 1

    actual val weekdayNames: List<Pair<String, String>>
        get() = WEEKDAYS.map { it to it.substring(0, 3) }

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        cache: MutableMap<String, Any>,
    ): String = format(dateOf(utcTimeMillis), pattern)

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        cache: MutableMap<String, Any>,
    ): String = format(dateOf(utcTimeMillis), skeletonToPattern(skeleton))

    actual fun parse(
        date: String,
        pattern: String,
        locale: CalendarLocale,
        cache: MutableMap<String, Any>,
    ): CalendarDate? = parseNumeric(date, pattern)

    // US puts the month first; most of the world puts the day first; CJK and ISO-style locales go
    // year-first. Previously hardcoded to the US form for every locale on earth.
    actual fun getDateInputFormat(): DateInputFormat = when (region) {
        "US" -> DateInputFormat("MM/dd/yyyy", '/')
        "CN", "JP", "KR", "TW", "HU", "LT" -> DateInputFormat("yyyy/MM/dd", '/')
        else -> DateInputFormat("dd/MM/yyyy", '/')
    }

    actual fun is24HourFormat(): Boolean = region !in TWELVE_HOUR_REGIONS

    private fun dateOf(millis: Long): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

    // Minimal CLDR-ish token formatter for the y/M/d/E fields the DatePicker uses.
    private fun format(date: LocalDate, pattern: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            if (c !in "yMdE") { sb.append(c); i++; continue }
            var n = 0
            while (i < pattern.length && pattern[i] == c) { n++; i++ }
            sb.append(
                when (c) {
                    'y' -> if (n <= 2) (date.year % 100).toString().padStart(2, '0') else date.year.toString()
                    'M' -> when {
                        n >= 4 -> MONTHS[date.monthNumber - 1]
                        n == 3 -> MONTHS[date.monthNumber - 1].substring(0, 3)
                        n == 2 -> date.monthNumber.toString().padStart(2, '0')
                        else -> date.monthNumber.toString()
                    }
                    'd' -> if (n >= 2) date.dayOfMonth.toString().padStart(2, '0') else date.dayOfMonth.toString()
                    else -> { // 'E'
                        val name = WEEKDAYS[date.dayOfWeek.ordinal] // DayOfWeek: MONDAY == 0
                        if (n >= 4) name else name.substring(0, 3)
                    }
                }
            )
        }
        return sb.toString()
    }

    // A skeleton has no delimiters; insert a space between field runs for readability.
    private fun skeletonToPattern(skeleton: String): String {
        val sb = StringBuilder()
        var prev = ' '
        for (c in skeleton) {
            if (prev in "yMdE" && c in "yMdE" && c != prev) sb.append(' ')
            sb.append(c)
            prev = c
        }
        return sb.toString()
    }

    private fun parseNumeric(date: String, pattern: String): CalendarDate? {
        val delimiter = pattern.firstOrNull { it !in "yMdE" } ?: return null
        val fields = pattern.split(delimiter).map { it.firstOrNull() ?: ' ' }
        val parts = date.split(delimiter)
        if (parts.size != fields.size) return null
        var year = 0; var month = 0; var day = 0
        for (idx in fields.indices) {
            val value = parts[idx].toIntOrNull() ?: return null
            when (fields[idx]) {
                'y' -> year = value
                'M' -> month = value
                'd' -> day = value
            }
        }
        if (month !in 1..12 || day !in 1..31) return null
        val millis = LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        return CalendarDate(year, month, day, millis)
    }
}
