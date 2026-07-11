// POC 6 Jalon 4 : SkipFoundation/Number.kt appelle les methodes java.lang.Number (doubleValue(),
// intValue()...) sur les nombres. En K/N, kotlin.Number expose toDouble()/toInt()... a la place.
// On fournit les alias en extensions, dans le package skip.foundation pour etre en portee sans editer
// les fichiers transpiles. java.lang.Number est un typealias vers kotlin.Number (cf. JavaLang.kt).
package skip.foundation

fun Number.doubleValue(): Double = toDouble()
fun Number.floatValue(): Float = toFloat()
fun Number.intValue(): Int = toInt()
fun Number.longValue(): Long = toLong()
fun Number.shortValue(): Short = toShort()
fun Number.byteValue(): Byte = toByte()
