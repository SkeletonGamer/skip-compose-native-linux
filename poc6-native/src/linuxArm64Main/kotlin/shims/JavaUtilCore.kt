// POC 6 Jalon 4: java.util companion types SkipFoundation references, as K/N compile-only shims.
package java.util

typealias ArrayList<E> = kotlin.collections.ArrayList<E>
typealias LinkedHashMap<K, V> = kotlin.collections.LinkedHashMap<K, V>
typealias HashMap<K, V> = kotlin.collections.HashMap<K, V>
typealias HashSet<E> = kotlin.collections.HashSet<E>
typealias LinkedList<E> = kotlin.collections.ArrayList<E>
typealias List<E> = kotlin.collections.List<E>
typealias Set<E> = kotlin.collections.Set<E>
typealias Map<K, V> = kotlin.collections.Map<K, V>
typealias Collection<E> = kotlin.collections.Collection<E>
typealias Iterator<E> = kotlin.collections.Iterator<E>

class Optional<T> private constructor(private val value: T?) {
    fun isPresent(): Boolean = value != null
    fun isEmpty(): Boolean = value == null
    fun get(): T = value ?: throw NoSuchElementException("Optional.get on empty")
    fun orElse(other: T): T = value ?: other
    companion object {
        fun <T> of(value: T): Optional<T> = Optional(value)
        fun <T> ofNullable(value: T?): Optional<T> = Optional(value)
        fun <T> empty(): Optional<T> = Optional(null)
    }
}

class Date : Comparable<Date> {
    private var millis: Long
    constructor() { millis = 0L }
    constructor(time: Long) { millis = time }
    fun getTime(): Long = millis
    fun setTime(time: Long) { millis = time }
    fun before(other: Date): Boolean = millis < other.millis
    fun after(other: Date): Boolean = millis > other.millis
    fun toInstant(): java.time.Instant = java.time.Instant.ofEpochMilli(millis)
    fun clone(): Any = Date(millis)
    override fun compareTo(other: Date): Int = millis.compareTo(other.millis)
    companion object {
        fun from(instant: java.time.Instant): Date = Date(instant.toEpochMilli())
    }
    override fun equals(other: Any?): Boolean = other is Date && other.millis == millis
    override fun hashCode(): Int = millis.hashCode()
    override fun toString(): String = "Date($millis)"
}

class Locale {
    val language: String
    val country: String
    constructor(language: String, country: String = "") { this.language = language; this.country = country }
    fun getLanguage(): String = language
    fun getCountry(): String = country
    fun getDisplayName(): String = TODO("K/N Locale.getDisplayName stub")
    fun getDisplayName(inLocale: Locale): String = TODO("K/N stub")
    fun getDisplayLanguage(inLocale: Locale): String = TODO("K/N stub")
    fun getDisplayCountry(inLocale: Locale): String = TODO("K/N stub")
    fun getVariant(): String = ""
    fun getScript(): String = ""
    fun getDisplayScript(): String = ""
    fun getDisplayScript(inLocale: Locale): String = ""
    fun getDisplayVariant(inLocale: Locale): String = ""
    fun toLanguageTag(): String = if (country.isEmpty()) language else "$language-$country"
    fun clone(): Any = Locale(language, country)
    override fun toString(): String = toLanguageTag()

    class Builder {
        private var lang = ""
        private var region = ""
        fun setLanguage(language: String?): Builder { lang = language ?: ""; return this }
        fun setRegion(region: String?): Builder { this.region = region ?: ""; return this }
        fun setScript(script: String?): Builder = this
        fun setVariant(variant: String?): Builder = this
        fun build(): Locale = Locale(lang, region)
    }

    companion object {
        val ROOT = Locale("")
        val US = Locale("en", "US")
        val ENGLISH = Locale("en")
        private var default = US
        fun getDefault(): Locale = default
        fun setDefault(locale: Locale) { default = locale }
        fun forLanguageTag(tag: String): Locale {
            val parts = tag.split("-", "_")
            return Locale(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
        }
        fun getAvailableLocales(): Array<Locale> = arrayOf(US)
    }
}

open class TimeZone {
    open var id: String = "UTC"
    open fun getID(): String = id
    open fun getRawOffset(): Int = 0
    open fun getOffset(millis: Long): Int = 0
    open fun getDisplayName(): String = id
    open fun getDisplayName(daylight: Boolean, style: Int): String = id
    open fun getDisplayName(daylight: Boolean, style: Int, locale: Locale): String = id
    open fun getDisplayName(locale: Locale): String = id
    open fun useDaylightTime(): Boolean = false
    open fun inDaylightTime(date: Date): Boolean = false
    open fun getDSTSavings(): Int = 0
    open fun toZoneId(): java.time.ZoneId = TODO("K/N TimeZone.toZoneId stub")
    open fun clone(): Any = getTimeZone(id)

    companion object {
        const val SHORT = 0
        const val LONG = 1
        private val utc = TimeZone().apply { id = "UTC" }
        fun getDefault(): TimeZone = utc
        fun setDefault(zone: TimeZone?) { }
        fun getTimeZone(id: String?): TimeZone = TimeZone().apply { this.id = id ?: "UTC" }
        fun getAvailableIDs(): Array<String> = arrayOf("UTC")
    }
}

class SimpleTimeZone : TimeZone {
    constructor(rawOffset: Int, id: String) : super() { this.id = id }
    override fun getRawOffset(): Int = 0
}

class Currency private constructor(private val code: String) {
    fun getCurrencyCode(): String = code
    fun getSymbol(): String = code
    fun getSymbol(locale: Locale): String = code
    fun getDisplayName(): String = code
    fun getDisplayName(locale: Locale): String = code
    fun getDefaultFractionDigits(): Int = 2
    companion object {
        fun getInstance(currencyCode: String): Currency = Currency(currencyCode)
        fun getInstance(locale: Locale): Currency = Currency("USD")
        fun getAvailableCurrencies(): Set<Currency> = setOf(Currency("USD"), Currency("EUR"))
    }
}

class UUID : Comparable<UUID> {
    private val value: String
    private constructor(v: String) { value = v }
    override fun compareTo(other: UUID): Int = value.compareTo(other.value)
    constructor(mostSigBits: Long, leastSigBits: Long) {
        val hex = mostSigBits.toULong().toString(16).padStart(16, '0') + leastSigBits.toULong().toString(16).padStart(16, '0')
        value = "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
    }
    fun toString2(): String = value
    override fun toString(): String = value
    companion object {
        fun randomUUID(): UUID {
            val hex = "0123456789abcdef"
            val sb = StringBuilder()
            for (i in 0 until 32) {
                sb.append(hex[kotlin.random.Random.nextInt(16)])
                if (i == 7 || i == 11 || i == 15 || i == 19) sb.append('-')
            }
            return UUID(sb.toString())
        }
        fun fromString(name: String): UUID = UUID(name)
    }
}

object Base64 {
    fun getEncoder(): Encoder = Encoder
    fun getDecoder(): Decoder = Decoder
    object Encoder {
        fun encode(src: ByteArray): ByteArray = TODO("K/N Base64 stub")
        fun encodeToString(src: ByteArray): String = TODO("K/N Base64 stub")
    }
    object Decoder {
        fun decode(src: String): ByteArray = TODO("K/N Base64 stub")
        fun decode(src: ByteArray): ByteArray = TODO("K/N Base64 stub")
    }
}

class MissingResourceException(message: String, className: String = "", key: String = "") : RuntimeException(message)

// java.util.Properties : SkipFoundation's ProcessInfo lit System.getProperties(). Map de String vers String.
// LinkedHashMap est final en Kotlin, donc delegation plutot qu'heritage.
class Properties : Map<Any?, Any?> by LinkedHashMap<Any?, Any?>() {
    fun getProperty(key: String): String? = get(key) as String?
    fun getProperty(key: String, default: String): String = (get(key) as String?) ?: default
    fun stringPropertyNames(): Set<String> = keys.mapNotNull { it as? String }.toSet()
}
