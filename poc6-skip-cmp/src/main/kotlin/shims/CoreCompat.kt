// POC 6 Jalon 2: androidx.core.content.ContextCompat (aar-only). Loose stubs.
package androidx.core.content

object ContextCompat {
    fun startActivity(context: android.content.Context, intent: android.content.Intent, options: android.os.Bundle?) {}
    fun getSystemService(context: android.content.Context, clazz: Class<*>): Any? = null
    fun checkSelfPermission(context: android.content.Context, permission: String): Int = 0
}
