// POC 6 K/N Linux : androidx.core.content (pas d'artefact K/N Linux).
// Cale compile-only ; seul checkSelfPermission est referencé par SkipUI.
package androidx.core.content

object ContextCompat {
    fun startActivity(context: android.content.Context, intent: android.content.Intent, options: android.os.Bundle?) { }
    fun checkSelfPermission(context: android.content.Context, permission: String): Int = TODO("K/N stub")
}
