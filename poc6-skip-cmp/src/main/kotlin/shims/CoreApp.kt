// POC 6 Jalon 2/3: androidx.core.app.ActivityCompat (aar-only). Loose stubs.
package androidx.core.app

object ActivityCompat {
    fun shouldShowRequestPermissionRationale(activity: android.app.Activity, permission: String): Boolean = false
    fun requestPermissions(activity: android.app.Activity, permissions: Array<String>, requestCode: Int) {}
    fun checkSelfPermission(context: android.content.Context, permission: String): Int = 0
}
