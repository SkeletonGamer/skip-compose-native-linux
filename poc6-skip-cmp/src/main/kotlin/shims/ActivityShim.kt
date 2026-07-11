// POC 6 Jalon 2/3: androidx.activity.ComponentActivity (aar-only). Stub over the android.jar Activity.
package androidx.activity

open class ComponentActivity : android.app.Activity() {
    fun addOnNewIntentListener(listener: androidx.core.util.Consumer<android.content.Intent>) {}
    fun removeOnNewIntentListener(listener: androidx.core.util.Consumer<android.content.Intent>) {}
}
