// POC 6 Jalon 4 : K/N n'a pas de kotlin.text.Charset. On fournit une vraie classe java.nio.charset.Charset
// (StringEncoding la porte comme type). L'encodage effectif se fait en UTF-8 cote extensions (K/N n'a pas
// d'encodage multi-charset dans la stdlib) ; un portage reel brancherait iconv.
package java.nio.charset

open class Charset(private val nameValue: String) {
    fun name(): String = nameValue
    fun displayName(): String = nameValue
    override fun toString(): String = nameValue
}
