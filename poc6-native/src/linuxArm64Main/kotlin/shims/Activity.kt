// POC 6 K/N Linux : androidx.activity (pas d'artefact K/N Linux).
// ComponentActivity : cale au-dessus du stub android.app.Activity, avec les listeners d'intent référencés.
package androidx.activity

open class ComponentActivity : android.app.Activity() {
    fun addOnNewIntentListener(listener: androidx.core.util.Consumer<android.content.Intent>): Unit = TODO("K/N stub")
    fun removeOnNewIntentListener(listener: androidx.core.util.Consumer<android.content.Intent>): Unit = TODO("K/N stub")
}
