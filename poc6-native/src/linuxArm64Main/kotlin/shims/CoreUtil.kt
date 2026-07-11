// POC 6 K/N Linux : androidx.core.util (pas d'artefact K/N Linux).
// SkipUI implémente Consumer<Intent> (OnNewIntentListener) : interface fonctionnelle, sans corps.
package androidx.core.util

fun interface Consumer<T> {
    fun accept(t: T)
}
