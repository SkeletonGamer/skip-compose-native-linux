// POC 6 Jalon 4 : sur la JVM, `Charsets` (kotlin.text) et l'encodage String<->ByteArray par charset
// existent. K/N ne les a pas. On fournit un `Charsets` de type java.nio.charset.Charset et les extensions
// d'encodage, en package skip.foundation pour etre en portee. L'implementation reelle est UTF-8 (les
// autres charsets compilent mais decoderaient en UTF-8 ; un portage reel utiliserait iconv).
package skip.foundation

import java.nio.charset.Charset

object Charsets {
    val UTF_8 = Charset("UTF-8")
    val UTF_16 = Charset("UTF-16")
    val UTF_16BE = Charset("UTF-16BE")
    val UTF_16LE = Charset("UTF-16LE")
    val UTF_32 = Charset("UTF-32")
    val UTF_32BE = Charset("UTF-32BE")
    val UTF_32LE = Charset("UTF-32LE")
    val US_ASCII = Charset("US-ASCII")
    val ISO_8859_1 = Charset("ISO-8859-1")
}

fun String.toByteArray(charset: Charset): ByteArray = encodeToByteArray()
fun ByteArray.toString(charset: Charset): String = decodeToString()
