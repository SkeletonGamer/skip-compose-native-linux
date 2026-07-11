// POC 6 Jalon 4: java.time.temporal surface (compile-only K/N stub).
package java.time.temporal

interface TemporalField

interface TemporalAccessor {
    fun isSupported(field: TemporalField): Boolean
    fun get(field: TemporalField): Int
    fun getLong(field: TemporalField): Long
}

// Subset of ChronoField referenced by SkipFoundation's date formatters.
enum class ChronoField : TemporalField {
    NANO_OF_SECOND,
    SECOND_OF_MINUTE,
    MINUTE_OF_HOUR,
    HOUR_OF_DAY,
    DAY_OF_WEEK,
    DAY_OF_MONTH,
    DAY_OF_YEAR,
    MONTH_OF_YEAR,
    YEAR,
    ALIGNED_WEEK_OF_YEAR,
    ALIGNED_WEEK_OF_MONTH;

    fun getFrom(temporal: TemporalAccessor): Long = temporal.getLong(this)
}
