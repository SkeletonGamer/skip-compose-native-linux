// POC 6 Jalon 4 : javax.crypto (HMAC) utilise par Digest de SkipFoundation. Cale compile-only ; un
// portage reel brancherait une lib crypto native.
package javax.crypto

class Mac {
    fun init(key: Any): Unit = TODO("K/N javax.crypto stub")
    fun update(input: ByteArray): Unit = TODO("K/N javax.crypto stub")
    fun doFinal(input: ByteArray): ByteArray = TODO("K/N javax.crypto stub")
    fun doFinal(): ByteArray = TODO("K/N javax.crypto stub")
    companion object {
        fun getInstance(algorithm: String): Mac = TODO("K/N javax.crypto stub")
    }
}
