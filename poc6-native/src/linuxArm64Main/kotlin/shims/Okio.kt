package okio

// Cale compile-only pour Kotlin/Native Linux : surface d'okio.ByteString reellement
// utilisee par le code transpile (URLSessionTask.kt, messages WebSocket).

class ByteString {
    fun toByteArray(): ByteArray = TODO("K/N stub")

    companion object {
        // import okio.ByteString.Companion.toByteString
        fun ByteArray.toByteString(): ByteString = TODO("K/N stub")
    }
}

// Surface okio referencee par les stubs coil3 (ImageSource, Fetcher).
interface Source
interface Sink

interface BufferedSource : Source {
    fun readByteArray(): ByteArray
    fun readUtf8(): String
    fun readByte(): Byte
    fun peek(): BufferedSource
    fun buffer(): BufferedSource
    fun close()
}

class Path {
    fun toFile(): java.io.File = TODO("K/N okio stub")
    companion object {
        fun String.toPath(): Path = TODO("K/N okio stub")
    }
}

abstract class FileSystem {
    abstract fun source(file: Path): Source
    companion object {
        val SYSTEM: FileSystem get() = TODO("K/N okio stub")
    }
}

// Fonctions top-level okio (import okio.source / okio.buffer) : InputStream -> Source -> BufferedSource.
fun java.io.InputStream.source(): Source = TODO("K/N okio stub")
fun Source.buffer(): BufferedSource = TODO("K/N okio stub")

