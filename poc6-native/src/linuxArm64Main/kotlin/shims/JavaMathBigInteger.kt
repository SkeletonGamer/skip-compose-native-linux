// POC 6 Jalon 4: java.math.BigInteger for K/N, backed by the ionspin multiplatform bignum. SkipLib
// aliases java.math.BigInteger to represent Swift's Int128/UInt128; SkipFoundation also uses it. This
// wrapper mirrors the java.math.BigInteger method surface those transpiled files call.
package java.math

import com.ionspin.kotlin.bignum.integer.BigInteger as Ion

class BigInteger : Comparable<BigInteger> {
    internal val v: Ion

    internal constructor(value: Ion) { v = value }
    constructor(string: String) { v = Ion.parseString(string, 10) }
    constructor(string: String, radix: Int) { v = Ion.parseString(string, radix) }

    fun add(o: BigInteger) = BigInteger(v + o.v)
    fun subtract(o: BigInteger) = BigInteger(v - o.v)
    fun multiply(o: BigInteger) = BigInteger(v * o.v)
    fun divide(o: BigInteger) = BigInteger(v / o.v)
    fun remainder(o: BigInteger) = BigInteger(v % o.v)
    fun mod(o: BigInteger) = BigInteger(v.mod(o.v))
    fun negate() = BigInteger(v.negate())
    fun abs() = BigInteger(v.abs())
    fun pow(exponent: Int) = BigInteger(v.pow(exponent.toLong()))
    fun and(o: BigInteger) = BigInteger(v and o.v)
    fun or(o: BigInteger) = BigInteger(v or o.v)
    fun xor(o: BigInteger) = BigInteger(v xor o.v)
    fun shiftLeft(n: Int) = BigInteger(v shl n)
    fun shiftRight(n: Int) = BigInteger(v shr n)
    fun min(o: BigInteger) = if (v < o.v) this else o
    fun max(o: BigInteger) = if (v > o.v) this else o
    fun signum() = v.signum()

    fun toLong() = v.longValue(false)
    fun toInt() = v.intValue(false)
    fun toDouble() = v.doubleValue(false)
    fun toFloat() = v.floatValue(false)
    fun longValueExact() = v.longValue(true)
    fun intValueExact() = v.intValue(true)

    override fun compareTo(other: BigInteger) = v.compareTo(other.v)
    override fun equals(other: Any?) = other is BigInteger && v == other.v
    override fun hashCode() = v.hashCode()
    override fun toString() = v.toString()

    companion object {
        val ZERO = BigInteger(Ion.ZERO)
        val ONE = BigInteger(Ion.ONE)
        val TEN = BigInteger(Ion.TEN)
        fun valueOf(value: Long) = BigInteger(Ion.fromLong(value))
    }
}
