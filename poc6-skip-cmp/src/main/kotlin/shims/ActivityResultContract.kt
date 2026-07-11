// POC 6 Jalon 2/3: androidx.activity.result.contract (aar-only). Loose stubs.
package androidx.activity.result.contract

abstract class ActivityResultContract<I, O>

class ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>()
    class RequestMultiplePermissions : ActivityResultContract<Array<String>, Map<String, Boolean>>()
    class StartActivityForResult : ActivityResultContract<android.content.Intent, Any?>()
}
