// POC 6 Jalon 4: java.time core surface (compile-only K/N stub). SkipFoundation's ISO8601 / relative
// date formatters lean on these; a production port would back them with kotlinx-datetime.
package java.time

import java.time.temporal.TemporalAccessor
import java.time.zone.ZoneRules

class Instant : TemporalAccessor {
    override fun isSupported(field: java.time.temporal.TemporalField): Boolean = TODO("K/N java.time stub")
    override fun get(field: java.time.temporal.TemporalField): Int = TODO("K/N java.time stub")
    override fun getLong(field: java.time.temporal.TemporalField): Long = TODO("K/N java.time stub")
    fun toEpochMilli(): Long = TODO("K/N java.time stub")
    fun getEpochSecond(): Long = TODO("K/N java.time stub")
    fun getNano(): Int = TODO("K/N java.time stub")
    fun atZone(zone: ZoneId?): ZonedDateTime = TODO("K/N java.time stub")

    companion object {
        fun ofEpochMilli(epochMilli: Long): Instant = TODO("K/N java.time stub")
        fun ofEpochSecond(epochSecond: Long, nanoAdjustment: Long = 0): Instant = TODO("K/N java.time stub")
        fun from(temporal: TemporalAccessor): Instant = TODO("K/N java.time stub")
        fun now(): Instant = TODO("K/N java.time stub")
    }
}

open class ZoneId {
    open fun getId(): String = TODO("K/N java.time stub")
    open fun getRules(): ZoneRules = TODO("K/N java.time stub")
    open val rules: ZoneRules get() = getRules()
    open fun getDisplayName(style: java.time.format.TextStyle, locale: java.util.Locale?): String = TODO("K/N java.time stub")
    override fun toString(): String = getId()

    companion object {
        fun of(zoneId: String): ZoneId = TODO("K/N java.time stub")
        fun ofOffset(prefix: String, offset: ZoneOffset): ZoneId = TODO("K/N java.time stub")
        fun getAvailableZoneIds(): Set<String> = TODO("K/N java.time stub")
        fun systemDefault(): ZoneId = TODO("K/N java.time stub")
    }
}

class ZoneOffset : ZoneId() {
    fun getTotalSeconds(): Int = TODO("K/N java.time stub")
    companion object {
        val UTC: ZoneOffset = TODO("K/N java.time stub")
        fun ofTotalSeconds(totalSeconds: Int): ZoneOffset = TODO("K/N java.time stub")
        fun of(offsetId: String): ZoneOffset = TODO("K/N java.time stub")
    }
}

class ZonedDateTime : TemporalAccessor {
    override fun isSupported(field: java.time.temporal.TemporalField): Boolean = TODO("K/N java.time stub")
    override fun get(field: java.time.temporal.TemporalField): Int = TODO("K/N java.time stub")
    override fun getLong(field: java.time.temporal.TemporalField): Long = TODO("K/N java.time stub")
    fun toInstant(): Instant = TODO("K/N java.time stub")
    val offset: ZoneOffset get() = TODO("K/N java.time stub")
    fun format(formatter: java.time.format.DateTimeFormatter): String = TODO("K/N java.time stub")
    fun getYear(): Int = TODO("K/N java.time stub")
    fun getMonthValue(): Int = TODO("K/N java.time stub")
    fun getDayOfMonth(): Int = TODO("K/N java.time stub")

    companion object {
        fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime = TODO("K/N java.time stub")
        fun now(): ZonedDateTime = TODO("K/N java.time stub")
    }
}

class LocalDateTime : TemporalAccessor {
    override fun isSupported(field: java.time.temporal.TemporalField): Boolean = TODO("K/N java.time stub")
    override fun get(field: java.time.temporal.TemporalField): Int = TODO("K/N java.time stub")
    override fun getLong(field: java.time.temporal.TemporalField): Long = TODO("K/N java.time stub")
    fun format(formatter: java.time.format.DateTimeFormatter): String = TODO("K/N java.time stub")

    companion object {
        fun now(): LocalDateTime = TODO("K/N java.time stub")
        fun ofInstant(instant: Instant, zone: ZoneId): LocalDateTime = TODO("K/N java.time stub")
        fun parse(text: CharSequence, formatter: java.time.format.DateTimeFormatter): LocalDateTime = TODO("K/N java.time stub")
    }
}
