// POC 6 Jalon 4: java.util.stream.Stream + Collectors surface (compile-only K/N stub). java.nio.file.Files
// walk/list return these in SkipFoundation's FileManager.
package java.util.stream

interface Collector<T, A, R>

object Collectors {
    fun <T> toList(): Collector<T, Any?, List<T>> = TODO("K/N Collectors stub")
    fun <T> toSet(): Collector<T, Any?, Set<T>> = TODO("K/N Collectors stub")
}

interface Stream<T> {
    fun filter(predicate: (T) -> Boolean): Stream<T>
    fun <R> map(mapper: (T) -> R): Stream<R>
    fun forEach(action: (T) -> Unit)
    operator fun iterator(): Iterator<T>
    fun count(): Long
    fun close()
    fun sorted(): Stream<T>
    fun <R> collect(collector: Collector<in T, *, R>): R
    fun findFirst(): java.util.Optional<T>
}
