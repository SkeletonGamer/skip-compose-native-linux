// POC 6 Jalon 2: androidx.core.util.Consumer (aar-only).
package androidx.core.util

fun interface Consumer<T> {
    fun accept(t: T)
}
