// Cales compile-only reproduisant la surface `java.io.*` utilisee par le code
// Kotlin transpile de SkipFoundation, afin de compiler en Kotlin/Native Linux
// (ou le paquet JVM `java.io` n'existe pas). Aucun comportement reel : les
// corps sont des `TODO("K/N stub")`. Seuls les membres reference sont declares.
package java.io

// InputStream : flux d'octets en lecture. Abstrait comme sur la JVM ; les
// sous-classes (ex. ByteArrayInputStream) fournissent `read()`.
abstract class InputStream : AutoCloseable {
    // Lit un octet, ou -1 en fin de flux.
    abstract fun read(): Int

    // Lit dans un tampon, renvoie le nombre d'octets lus ou -1.
    open fun read(b: ByteArray): Int = TODO("K/N stub")

    open fun read(b: ByteArray, off: Int, len: Int): Int = TODO("K/N stub")

    open fun readAllBytes(): ByteArray = TODO("K/N stub")

    open fun readBytes(): ByteArray = TODO("K/N stub")

    override fun close() {
        TODO("K/N stub")
    }
}

// OutputStream : flux d'octets en ecriture. Abstrait comme sur la JVM ; les
// sous-classes (ex. ByteArrayOutputStream) fournissent `write(Int)`.
abstract class OutputStream {
    abstract fun write(b: Int)

    open fun write(b: ByteArray) {
        TODO("K/N stub")
    }

    open fun write(b: ByteArray, off: Int, len: Int) {
        TODO("K/N stub")
    }

    open fun flush() {
        TODO("K/N stub")
    }

    open fun close() {
        TODO("K/N stub")
    }
}

// ByteArrayInputStream : lit depuis un tableau d'octets en memoire.
open class ByteArrayInputStream(buf: ByteArray) : InputStream() {
    override fun read(): Int = TODO("K/N stub")
}

// ByteArrayOutputStream : accumule des octets en memoire.
open class ByteArrayOutputStream : OutputStream() {
    override fun write(b: Int) {
        TODO("K/N stub")
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        TODO("K/N stub")
    }

    fun toByteArray(): ByteArray = TODO("K/N stub")

    fun size(): Int = TODO("K/N stub")
}

// File : chemin du systeme de fichiers. Construit depuis une chaine ou une URI.
open class File {
    constructor(pathname: String) {
        TODO("K/N stub")
    }

    constructor(uri: java.net.URI) {
        TODO("K/N stub")
    }

    constructor(parent: File, child: String) {
        TODO("K/N stub")
    }

    constructor(parent: String, child: String) {
        TODO("K/N stub")
    }

    fun getPath(): String = TODO("K/N stub")

    fun getName(): String = TODO("K/N stub")

    fun exists(): Boolean = TODO("K/N stub")

    fun isDirectory(): Boolean = TODO("K/N stub")

    fun toURI(): java.net.URI = TODO("K/N stub")

    fun getAbsolutePath(): String = TODO("K/N stub")

    fun readBytes(): ByteArray = TODO("K/N stub")
}

// IOException : erreur d'entree/sortie (levee et attrapee par le code transpile).
open class IOException(message: String? = null) : kotlin.Exception(message ?: "")
