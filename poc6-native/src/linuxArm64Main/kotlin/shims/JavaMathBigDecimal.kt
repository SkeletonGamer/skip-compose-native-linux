// POC 6 : cale compile-only de java.math.BigDecimal pour K/N Linux. SkipFoundation aliase
// Decimal / NSDecimalNumber sur java.math.BigDecimal (Number.kt) et l'utilise dans le decodage /
// encodage JSON (JSONDecoder, JSONEncoder, JSONSerialization+Parser) et le Scanner. Seuls les
// membres reellement references par ces fichiers transpiles sont reproduits ici.
// java.math.BigInteger vit dans JavaMathBigInteger.kt (meme package), ne pas le redefinir.
package java.math

class BigDecimal {
    // BigDecimal(String) : construit depuis une representation textuelle (JSONDecoder, Number, Scanner).
    constructor(value: String) { TODO("K/N stub") }

    // BigDecimal(double) : construit depuis un Double (JSONDecoder.unwrapDecimal fallback).
    constructor(value: Double) { TODO("K/N stub") }

    // toPlainString() : rendu sans notation scientifique (JSONEncoder).
    fun toPlainString(): String = TODO("K/N stub")

    // toString() : rendu textuel (JSONSerialization+Parser).
    override fun toString(): String = TODO("K/N stub")
}
