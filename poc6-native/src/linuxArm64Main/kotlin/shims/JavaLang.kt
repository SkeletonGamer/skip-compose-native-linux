// POC 6 Jalon 4: java.lang types SkipFoundation references, mapped to Kotlin equivalents or stubbed.
package java.lang

import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr

typealias Number = kotlin.Number
typealias Object = kotlin.Any
typealias Runnable = () -> Unit
typealias Comparable<T> = kotlin.Comparable<T>

// java.lang.Thread : SkipFoundation's Thread se compare a currentThread() et lit la pile.
class Thread {
    fun getName(): String = TODO("K/N Thread stub")
    fun getStackTrace(): Array<StackTraceElement> = TODO("K/N Thread stub")
    fun start(): Unit = TODO("K/N Thread stub")
    fun join(): Unit = TODO("K/N Thread stub")
    companion object {
        fun currentThread(): Thread = TODO("K/N Thread stub")
        fun sleep(millis: Long): Unit = TODO("K/N Thread stub")
    }
}

class StackTraceElement {
    override fun toString(): String = TODO("K/N stub")
}

// java.lang.Integer / Byte : references pour les checks `is Integer` (parseur JSON) et le static
// java.lang.Byte.toUnsignedInt (Digest). Sur K/N il n'y a pas de boxing, ces checks sont morts.
class Integer {
    companion object {
        fun parseInt(s: String): Int = TODO("K/N stub")
        fun valueOf(i: Int): Integer = TODO("K/N stub")
    }
}

class Byte {
    companion object {
        fun toUnsignedInt(x: kotlin.Byte): Int = x.toInt() and 0xFF
    }
}

// java.lang.reflect.Method (via Class.getDeclaredMethod) : reflexion, stub en K/N.
class Method {
    fun invoke(obj: Any?, vararg args: Any?): Any? = TODO("K/N Method stub")
}
typealias StringBuilder = kotlin.text.StringBuilder
typealias Exception = kotlin.Exception
typealias RuntimeException = kotlin.RuntimeException
typealias IllegalArgumentException = kotlin.IllegalArgumentException
typealias IllegalStateException = kotlin.IllegalStateException
typealias NumberFormatException = kotlin.NumberFormatException
typealias IndexOutOfBoundsException = kotlin.IndexOutOfBoundsException
typealias CharSequence = kotlin.CharSequence

// java.lang.Class: on K/N there is no classloader, so forName is a stub. The type still needs to exist
// because java.nio.file.Files.readAttributes takes a java.lang.Class<A>, and SkipFoundation's ProcessInfo
// does reflective Class.forName lookups (which simply fail at runtime on K/N, like android.jar stubs).
class Class<T>(private val nm: String = "", private val simple: String = "") {
    fun getName(): String = nm
    fun getSimpleName(): String = simple.ifEmpty { nm.substringAfterLast('.') }
    // Accede comme propriete Kotlin (`cls.java.name`) dans le code transpile.
    val name: String get() = getName()
    val simpleName: String get() = getSimpleName()
    fun getDeclaredMethod(name: String, vararg parameterTypes: Class<*>): Method = TODO("K/N Class stub")
    fun getMethod(name: String, vararg parameterTypes: Class<*>): Method = TODO("K/N Class stub")
    companion object {
        fun forName(name: String): Class<*> = TODO("K/N Class.forName stub")
    }
}

// java.lang.Runtime : SkipFoundation's ProcessInfo lit availableProcessors().
class Runtime {
    fun availableProcessors(): Int = TODO("K/N Runtime stub")
    fun freeMemory(): Long = TODO("K/N Runtime stub")
    fun totalMemory(): Long = TODO("K/N Runtime stub")
    fun maxMemory(): Long = TODO("K/N Runtime stub")
    companion object {
        fun getRuntime(): Runtime = Runtime()
    }
}

object System {
    // Temps reel (epoch millis) via posix gettimeofday ; nanoTime derive.
    fun currentTimeMillis(): Long = kotlinx.cinterop.memScoped {
        val tv = alloc<platform.posix.timeval>()
        platform.posix.gettimeofday(tv.ptr, null)
        tv.tv_sec.toLong() * 1000L + tv.tv_usec.toLong() / 1000L
    }
    fun nanoTime(): Long = currentTimeMillis() * 1_000_000L
    // Defauts sensés cote desktop (rien ne leve : le bootstrap ProcessInfo lit ces proprietes).
    fun getProperty(key: String): String = when (key) {
        "os.name" -> "Linux"
        "os.arch" -> "aarch64"
        "java.io.tmpdir" -> "/tmp"
        "user.home" -> "/root"
        "user.name" -> "skip"
        "user.dir" -> "/app"
        "file.separator" -> "/"
        "path.separator" -> ":"
        "line.separator" -> "\n"
        else -> ""
    }
    fun getProperty(key: String, default: String): String = getProperty(key).ifEmpty { default }
    fun getProperties(): java.util.Properties = java.util.Properties()
    fun getenv(name: String): String? = null
    fun getenv(): Map<String, String> = emptyMap()
    fun lineSeparator(): String = "\n"
    fun <T> arraycopy(src: T, srcPos: Int, dest: T, destPos: Int, length: Int): Unit = TODO("K/N stub")
    fun identityHashCode(obj: Any?): Int = obj?.hashCode() ?: 0
}
