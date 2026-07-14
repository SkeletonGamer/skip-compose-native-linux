// The slice of the ICU4C C API this backend needs, resolved through dlsym (see IcuLoader).
//
// Every call is guarded: if ICU is missing the wrappers return null and the caller falls back. ICU strings
// are UTF-16 (UChar = uint16_t), which is also Kotlin's Char representation, so conversion is a copy.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package icu

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import kotlinx.cinterop.cstr

// UErrorCode: <= 0 is success (0 = U_ZERO_ERROR, negative values are warnings).
private fun ok(status: Int) = status <= 0

// UDateFormatStyle
private const val UDAT_NONE = -1
private const val UDAT_PATTERN = -2

// UCalendarAttribute
private const val UCAL_FIRST_DAY_OF_WEEK = 1

// UCalendarType
private const val UCAL_DEFAULT = 0

// ULayoutType (uloc_getCharacterOrientation)
private const val ULOC_LAYOUT_RTL = 1

private typealias UdatOpen = CFunction<(Int, Int, CPointer<kotlinx.cinterop.ByteVar>?, CPointer<UShortVar>?, Int, CPointer<UShortVar>?, Int, CPointer<IntVar>?) -> COpaquePointer?>
private typealias UdatFormat = CFunction<(COpaquePointer?, Double, CPointer<UShortVar>?, Int, COpaquePointer?, CPointer<IntVar>?) -> Int>
private typealias UdatParse = CFunction<(COpaquePointer?, CPointer<UShortVar>?, Int, CPointer<IntVar>?, CPointer<IntVar>?) -> Double>
private typealias UdatToPattern = CFunction<(COpaquePointer?, Int, CPointer<UShortVar>?, Int, CPointer<IntVar>?) -> Int>
private typealias UdatClose = CFunction<(COpaquePointer?) -> Unit>
private typealias UcalOpen = CFunction<(CPointer<UShortVar>?, Int, CPointer<kotlinx.cinterop.ByteVar>?, Int, CPointer<IntVar>?) -> COpaquePointer?>
private typealias UcalGetAttribute = CFunction<(COpaquePointer?, Int) -> Int>
private typealias UcalClose = CFunction<(COpaquePointer?) -> Unit>
private typealias UdatpgOpen = CFunction<(CPointer<kotlinx.cinterop.ByteVar>?, CPointer<IntVar>?) -> COpaquePointer?>
private typealias UdatpgBest = CFunction<(COpaquePointer?, CPointer<UShortVar>?, Int, CPointer<UShortVar>?, Int, CPointer<IntVar>?) -> Int>
private typealias UdatpgClose = CFunction<(COpaquePointer?) -> Unit>
private typealias UlocOrientation = CFunction<(CPointer<kotlinx.cinterop.ByteVar>?, CPointer<IntVar>?) -> Int>
private typealias UStrCase = CFunction<(CPointer<UShortVar>?, Int, CPointer<UShortVar>?, Int, CPointer<kotlinx.cinterop.ByteVar>?, CPointer<IntVar>?) -> Int>

object IcuApi {
    val available: Boolean get() = Icu.available

    private val udatOpen by lazy { Icu.i18nFun<UdatOpen>("udat_open") }
    private val udatFormat by lazy { Icu.i18nFun<UdatFormat>("udat_format") }
    private val udatParse by lazy { Icu.i18nFun<UdatParse>("udat_parse") }
    private val udatToPattern by lazy { Icu.i18nFun<UdatToPattern>("udat_toPattern") }
    private val udatClose by lazy { Icu.i18nFun<UdatClose>("udat_close") }
    private val ucalOpen by lazy { Icu.i18nFun<UcalOpen>("ucal_open") }
    private val ucalGetAttribute by lazy { Icu.i18nFun<UcalGetAttribute>("ucal_getAttribute") }
    private val ucalClose by lazy { Icu.i18nFun<UcalClose>("ucal_close") }
    private val udatpgOpen by lazy { Icu.i18nFun<UdatpgOpen>("udatpg_open") }
    private val udatpgBest by lazy { Icu.i18nFun<UdatpgBest>("udatpg_getBestPattern") }
    private val udatpgClose by lazy { Icu.i18nFun<UdatpgClose>("udatpg_close") }
    private val ulocOrientation by lazy { Icu.ucFun<UlocOrientation>("uloc_getCharacterOrientation") }
    private val uStrToUpper by lazy { Icu.ucFun<UStrCase>("u_strToUpper") }
    private val uStrToLower by lazy { Icu.ucFun<UStrCase>("u_strToLower") }

    /** Formats `utcMillis` with an ICU/CLDR pattern in `locale`. Null when ICU is unavailable or errors. */
    fun format(utcMillis: Long, pattern: String, locale: String): String? {
        val open = udatOpen ?: return null
        val fmt = udatFormat ?: return null
        val close = udatClose ?: return null
        return memScoped {
            val status = alloc<IntVar>().apply { value = 0 }
            val pat = pattern.toUChars(this)
            val utc = "UTC".toUChars(this)
            val df = open(UDAT_PATTERN, UDAT_PATTERN, locale.cstr.ptr, utc, 3, pat, pattern.length, status.ptr)
                ?: return@memScoped null
            if (!ok(status.value)) { close(df); return@memScoped null }
            val cap = 256
            val out = allocArray<UShortVar>(cap)
            status.value = 0
            val len = fmt(df, utcMillis.toDouble(), out, cap, null, status.ptr)
            close(df)
            if (!ok(status.value) || len <= 0) null else out.toKString(len)
        }
    }

    /** Turns a CLDR skeleton ("yMMMd") into the best pattern for `locale` ("d MMM y" in fr, "MMM d, y" in en). */
    fun bestPattern(skeleton: String, locale: String): String? {
        val open = udatpgOpen ?: return null
        val best = udatpgBest ?: return null
        val close = udatpgClose ?: return null
        return memScoped {
            val status = alloc<IntVar>().apply { value = 0 }
            val pg = open(locale.cstr.ptr, status.ptr) ?: return@memScoped null
            if (!ok(status.value)) { close(pg); return@memScoped null }
            val skel = skeleton.toUChars(this)
            val cap = 256
            val out = allocArray<UShortVar>(cap)
            status.value = 0
            val len = best(pg, skel, skeleton.length, out, cap, status.ptr)
            close(pg)
            if (!ok(status.value) || len <= 0) null else out.toKString(len)
        }
    }

    /** Parses `text` against `pattern` in `locale`, returning UTC millis. Null on failure. */
    fun parse(text: String, pattern: String, locale: String): Long? {
        val open = udatOpen ?: return null
        val parseFn = udatParse ?: return null
        val close = udatClose ?: return null
        return memScoped {
            val status = alloc<IntVar>().apply { value = 0 }
            val pat = pattern.toUChars(this)
            val utc = "UTC".toUChars(this)
            val df = open(UDAT_PATTERN, UDAT_PATTERN, locale.cstr.ptr, utc, 3, pat, pattern.length, status.ptr)
                ?: return@memScoped null
            if (!ok(status.value)) { close(df); return@memScoped null }
            val src = text.toUChars(this)
            val pos = alloc<IntVar>().apply { value = 0 }
            status.value = 0
            val millis = parseFn(df, src, text.length, pos.ptr, status.ptr)
            close(df)
            if (!ok(status.value)) null else millis.toLong()
        }
    }

    /** ISO first day of week (Monday == 1 ... Sunday == 7) for `locale`. Null when ICU is unavailable. */
    fun firstDayOfWeek(locale: String): Int? {
        val open = ucalOpen ?: return null
        val attr = ucalGetAttribute ?: return null
        val close = ucalClose ?: return null
        return memScoped {
            val status = alloc<IntVar>().apply { value = 0 }
            val cal = open(null, 0, locale.cstr.ptr, UCAL_DEFAULT, status.ptr) ?: return@memScoped null
            if (!ok(status.value)) { close(cal); return@memScoped null }
            val icuDay = attr(cal, UCAL_FIRST_DAY_OF_WEEK) // ICU: Sunday == 1 ... Saturday == 7
            close(cal)
            if (icuDay !in 1..7) null else if (icuDay == 1) 7 else icuDay - 1 // -> ISO
        }
    }

    /** True when `locale` is written right-to-left, straight from CLDR rather than a hand-kept table. */
    fun isRtl(locale: String): Boolean? {
        val fn = ulocOrientation ?: return null
        return memScoped {
            val status = alloc<IntVar>().apply { value = 0 }
            val layout = fn(locale.cstr.ptr, status.ptr)
            if (!ok(status.value)) null else layout == ULOC_LAYOUT_RTL
        }
    }

    fun uppercase(text: String, locale: String): String? = changeCase(text, locale, upper = true)
    fun lowercase(text: String, locale: String): String? = changeCase(text, locale, upper = false)

    private fun changeCase(text: String, locale: String, upper: Boolean): String? {
        val fn = (if (upper) uStrToUpper else uStrToLower) ?: return null
        return memScoped {
            val status = alloc<IntVar>().apply { value = 0 }
            val src = text.toUChars(this)
            val cap = text.length * 3 + 8 // case mapping can grow (e.g. the German sharp s)
            val out = allocArray<UShortVar>(cap)
            val len = fn(out, cap, src, text.length, locale.cstr.ptr, status.ptr)
            if (!ok(status.value) || len <= 0) null else out.toKString(len)
        }
    }
}

/** Kotlin Char is already UTF-16, so this is a straight copy into an ICU UChar buffer. */
private fun String.toUChars(scope: kotlinx.cinterop.MemScope): CPointer<UShortVar> {
    val buf = scope.allocArray<UShortVar>(length + 1)
    for (i in indices) buf[i] = this[i].code.toUShort()
    buf[length] = 0u
    return buf
}

private fun CPointer<UShortVar>.toKString(length: Int): String {
    val chars = CharArray(length)
    for (i in 0 until length) chars[i] = this[i].toInt().toChar()
    return chars.concatToString()
}
