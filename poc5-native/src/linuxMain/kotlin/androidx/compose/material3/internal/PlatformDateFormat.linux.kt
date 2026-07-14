// Linux actual for the DatePicker's date formatter. macOS leans on NSDateFormatter; this uses ICU4C when
// it is present on the system, which is what actually gives localized month and weekday names (CLDR data
// cannot be derived, it has to come from somewhere).
//
// ICU is loaded at RUNTIME, not linked (see icu/IcuLoader.kt): it renames its symbols per major version
// (udat_open_72), so linking would tie the binary to one ICU release. Every ICU call here degrades to the
// kotlinx-datetime path below when ICU is absent, so the app still runs on a system without it: English
// names, region-derived week and clock conventions.
@file:OptIn(kotlin.time.ExperimentalTime::class)

package androidx.compose.material3.internal

import androidx.compose.material3.CalendarLocale
import icu.IcuApi
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

    // ICU wants a locale id like "fr_FR"; the tag we carry is "fr-FR".
    private val icuLocale: String get() = locale.languageTag.replace('-', '_')

    // "fr-FR" -> "FR". Empty when the tag carries no region ("fr").
    private val region: String
        get() = locale.languageTag.split("-", "_").drop(1).firstOrNull { it.length == 2 }?.uppercase() ?: ""

    // ISO 1..7 (Monday == 1). ICU knows this per territory; the region table is the fallback.
    actual val firstDayOfWeek: Int
        get() = IcuApi.firstDayOfWeek(icuLocale) ?: if (region in SUNDAY_FIRST_REGIONS) 7 else 1

    // Localized weekday names, straight from CLDR. Formats a known Monday..Sunday (2024-01-01 was a
    // Monday) with the "EEEE"/"EEE" patterns, which is how you ask ICU for day names.
    actual val weekdayNames: List<Pair<String, String>>
        get() {
            if (!IcuApi.available) return WEEKDAYS.map { it to it.substring(0, 3) }
            val monday = LocalDate(2024, 1, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            val names = (0..6).map { offset ->
                val millis = monday + offset * 86_400_000L
                val full = IcuApi.format(millis, "EEEE", icuLocale)
                val short = IcuApi.format(millis, "EEE", icuLocale)
                if (full == null || short == null) return WEEKDAYS.map { it to it.substring(0, 3) }
                full to short
            }
            return names
        }

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        cache: MutableMap<String, Any>,
    ): String = IcuApi.format(utcTimeMillis, pattern, icuLocale) ?: format(dateOf(utcTimeMillis), pattern)

    // A skeleton is locale-independent ("yMMMd"); ICU turns it into the right pattern for the locale
    // ("d MMM y" in French, "MMM d, y" in English). That mapping is exactly the CLDR data we cannot fake.
    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        cache: MutableMap<String, Any>,
    ): String {
        val pattern = IcuApi.bestPattern(skeleton, icuLocale)
        if (pattern != null) {
            IcuApi.format(utcTimeMillis, pattern, icuLocale)?.let { return it }
        }
        return format(dateOf(utcTimeMillis), skeletonToPattern(skeleton))
    }

    actual fun parse(
        date: String,
        pattern: String,
        locale: CalendarLocale,
        cache: MutableMap<String, Any>,
    ): CalendarDate? {
        IcuApi.parse(date, pattern, locale.languageTag.replace('-', '_'))?.let { millis ->
            val d = dateOf(millis)
            return CalendarDate(d.year, d.monthNumber, d.dayOfMonth, millis)
        }
        return parseNumeric(date, pattern)
    }

    // US puts the month first; most of the world puts the day first; CJK and ISO-style locales go
    // year-first. Previously hardcoded to the US form for every locale on earth.
    actual fun getDateInputFormat(): DateInputFormat = when (region) {
        "US" -> DateInputFormat("MM/dd/yyyy", '/')
        "CN", "JP", "KR", "TW", "HU", "LT" -> DateInputFormat("yyyy/MM/dd", '/')
        else -> DateInputFormat("dd/MM/yyyy", '/')
    }

    // ICU knows the locale's time convention: ask it for the best "hour+minute" pattern and look at which
    // hour symbol it chose. 'H'/'k' are 24-hour cycles, 'h'/'K' are 12-hour ones.
    actual fun is24HourFormat(): Boolean {
        val pattern = IcuApi.bestPattern("jm", icuLocale)
        if (pattern != null) return pattern.any { it == 'H' || it == 'k' }
        return region !in TWELVE_HOUR_REGIONS
    }

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
                    // CLDR: "yy" means two digits; "y" and "yyyy" mean the full year. This used to treat a
                    // lone "y" as two digits, turning 2026 into "26".
                    'y' -> if (n == 2) (date.year % 100).toString().padStart(2, '0') else date.year.toString()
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

    // Fallback only (ICU does this properly via udatpg_getBestPattern). A CLDR skeleton lists the fields
    // but says nothing about their order: "yMMMMd" is year+month+day, and turning it into a readable date
    // is exactly the locale knowledge we do not have without CLDR. So we rebuild a sensible order from the
    // region, the same way getDateInputFormat() does, rather than emitting the fields as they came in
    // (which produced "26 July 14").
    private fun skeletonToPattern(skeleton: String): String {
        val runs = mutableMapOf<Char, Int>()
        for (c in skeleton) if (c in "yMdE") runs[c] = (runs[c] ?: 0) + 1

        val year = runs['y']?.let { "y".repeat(it) }
        val month = runs['M']?.let { "M".repeat(it) }
        val day = runs['d']?.let { "d".repeat(it) }
        val weekday = runs['E']?.let { "E".repeat(it) }

        val core = when {
            month == null || day == null -> listOfNotNull(day, month, year).joinToString(" ")
            region == "US" -> listOfNotNull(month, day).joinToString(" ") + (year?.let { ", $it" } ?: "")
            region in setOf("CN", "JP", "KR", "TW") ->
                listOfNotNull(year, month, day).joinToString(" ")
            else -> listOfNotNull(day, month, year).joinToString(" ") // most of the world: day first
        }
        return listOfNotNull(weekday?.let { "$it," }, core.takeIf { it.isNotEmpty() }).joinToString(" ")
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
