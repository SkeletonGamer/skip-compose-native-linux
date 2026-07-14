// POC 5 Jalon 3: actual Linux du formateur de dates du DatePicker. macOS s'appuie sur NSDateFormatter ;
// ici une implementation portee sur kotlinx-datetime avec des libelles anglais. Suffisante pour compiler
// material3 et afficher un DatePicker ; un mediator reel brancherait ICU/fontconfig pour la vraie i18n.
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

internal actual class PlatformDateFormat actual constructor(private val locale: CalendarLocale) {

    actual val firstDayOfWeek: Int get() = 1 // Monday (ISO 1..7)

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

    actual fun getDateInputFormat(): DateInputFormat = DateInputFormat("MM/dd/yyyy", '/')

    actual fun is24HourFormat(): Boolean = false

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
