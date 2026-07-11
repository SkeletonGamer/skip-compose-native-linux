// POC 6 Jalon 4: java.util.Calendar / GregorianCalendar API surface for K/N. Compile-only shim (the
// android.jar equivalent for the native side): real field constants so index arithmetic is correct,
// a backing store for get/set, and stubbed time conversion. SkipFoundation's Calendar/DateComponents
// lean on this heavily; a production port would back it with kotlinx-datetime.
package java.util

open class Calendar {
    protected val fields = IntArray(17)
    var timeInMillis: Long = 0L

    open fun get(field: Int): Int = fields[field]
    open fun set(field: Int, value: Int) { fields[field] = value }
    open fun set(year: Int, month: Int, date: Int) { fields[YEAR] = year; fields[MONTH] = month; fields[DAY_OF_MONTH] = date }
    open fun set(year: Int, month: Int, date: Int, hourOfDay: Int, minute: Int) { set(year, month, date); fields[HOUR_OF_DAY] = hourOfDay; fields[MINUTE] = minute }
    open fun set(year: Int, month: Int, date: Int, hourOfDay: Int, minute: Int, second: Int) { set(year, month, date, hourOfDay, minute); fields[SECOND] = second }
    open fun add(field: Int, amount: Int): Unit = TODO("K/N Calendar.add stub")
    open fun roll(field: Int, amount: Int): Unit = TODO("K/N Calendar.roll stub")
    open fun roll(field: Int, up: Boolean): Unit = TODO("K/N Calendar.roll stub")
    open fun clear() { fields.fill(0) }
    open fun clear(field: Int) { fields[field] = 0 }

    open fun getTimeInMillis(): Long = timeInMillis
    open fun setTimeInMillis(millis: Long) { timeInMillis = millis }
    open fun getTime(): Date = Date(timeInMillis)
    open fun setTime(date: Date) { timeInMillis = date.getTime() }
    // Accede comme propriete Kotlin (`cal.time = ...`) dans le code transpile.
    open var time: Date
        get() = Date(timeInMillis)
        set(value) { timeInMillis = value.getTime() }

    open var firstDayOfWeek: Int = SUNDAY
    open fun getFirstDayOfWeek(): Int = firstDayOfWeek
    open fun setFirstDayOfWeek(value: Int) { firstDayOfWeek = value }
    open var minimalDaysInFirstWeek: Int = 1
    open fun getMinimalDaysInFirstWeek(): Int = minimalDaysInFirstWeek
    open fun setMinimalDaysInFirstWeek(value: Int) { minimalDaysInFirstWeek = value }

    open var timeZone: TimeZone = TimeZone.getDefault()
    open fun getTimeZone(): TimeZone = timeZone
    open fun setTimeZone(value: TimeZone) { timeZone = value }
    open var isLenient: Boolean = true
    open fun setLenient(value: Boolean) { isLenient = value }

    open fun getMaximum(field: Int): Int = TODO("K/N Calendar.getMaximum stub")
    open fun getMinimum(field: Int): Int = TODO("K/N Calendar.getMinimum stub")
    open fun getActualMaximum(field: Int): Int = TODO("K/N Calendar.getActualMaximum stub")
    open fun getActualMinimum(field: Int): Int = TODO("K/N Calendar.getActualMinimum stub")
    open fun getGreatestMinimum(field: Int): Int = TODO("K/N stub")
    open fun getLeastMaximum(field: Int): Int = TODO("K/N stub")
    open fun getWeekYear(): Int = TODO("K/N stub")
    open fun setWeekDate(weekYear: Int, weekOfYear: Int, dayOfWeek: Int): Unit = TODO("K/N stub")

    open fun before(other: Any?): Boolean = timeInMillis < (other as Calendar).timeInMillis
    open fun after(other: Any?): Boolean = timeInMillis > (other as Calendar).timeInMillis
    open fun clone(): Any = TODO("K/N Calendar.clone stub")

    companion object {
        const val ERA = 0
        const val YEAR = 1
        const val MONTH = 2
        const val WEEK_OF_YEAR = 3
        const val WEEK_OF_MONTH = 4
        const val DATE = 5
        const val DAY_OF_MONTH = 5
        const val DAY_OF_YEAR = 6
        const val DAY_OF_WEEK = 7
        const val DAY_OF_WEEK_IN_MONTH = 8
        const val AM_PM = 9
        const val HOUR = 10
        const val HOUR_OF_DAY = 11
        const val MINUTE = 12
        const val SECOND = 13
        const val MILLISECOND = 14
        const val ZONE_OFFSET = 15
        const val DST_OFFSET = 16
        const val YEAR_FOR_WEEK_OF_YEAR = 17
        const val FIELD_COUNT = 17

        const val SUNDAY = 1
        const val MONDAY = 2
        const val TUESDAY = 3
        const val WEDNESDAY = 4
        const val THURSDAY = 5
        const val FRIDAY = 6
        const val SATURDAY = 7

        const val JANUARY = 0
        const val FEBRUARY = 1
        const val MARCH = 2
        const val APRIL = 3
        const val MAY = 4
        const val JUNE = 5
        const val JULY = 6
        const val AUGUST = 7
        const val SEPTEMBER = 8
        const val OCTOBER = 9
        const val NOVEMBER = 10
        const val DECEMBER = 11
        const val UNDECIMBER = 12

        const val AM = 0
        const val PM = 1
        const val ERA_BC = 0
        const val ERA_AD = 1

        fun getInstance(): Calendar = GregorianCalendar()
        fun getInstance(zone: TimeZone): Calendar = GregorianCalendar(zone)
        fun getInstance(zone: TimeZone, locale: Locale): Calendar = GregorianCalendar(zone, locale)
        fun getInstance(locale: Locale): Calendar = GregorianCalendar()
    }
}

class GregorianCalendar : Calendar {
    constructor() : super()
    constructor(zone: TimeZone) : super() { timeZone = zone }
    constructor(zone: TimeZone, locale: Locale) : super() { timeZone = zone }
    constructor(year: Int, month: Int, dayOfMonth: Int) : super() { set(year, month, dayOfMonth) }

    fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    companion object {
        const val BC = 0
        const val AD = 1
    }
}
