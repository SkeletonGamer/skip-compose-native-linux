// POC 6 : cale compile-only de java.security.MessageDigest pour K/N Linux. SkipFoundation s'en sert
// dans Digest.kt (SHA256 / SHA384 / SHA512 / MD5 / SHA1) via MessageDigest.getInstance(algorithme).
// java.security.SecureRandom vit dans JavaSecurity.kt (meme package), ne pas le redefinir.
package java.security

class MessageDigest private constructor() {
    // update(ByteArray) : accumule des octets dans l'etat du digest.
    fun update(input: ByteArray): Unit = TODO("K/N stub")

    // digest() : finalise et renvoie l'empreinte.
    fun digest(): ByteArray = TODO("K/N stub")

    // digest(ByteArray) : accumule puis finalise en une passe.
    fun digest(input: ByteArray): ByteArray = TODO("K/N stub")

    // reset() : reinitialise l'etat du digest.
    fun reset(): Unit = TODO("K/N stub")

    companion object {
        // getInstance(algorithme) : fabrique un digest pour "SHA-256", "MD5", "SHA1"...
        fun getInstance(algorithm: String): MessageDigest = TODO("K/N stub")
    }
}
