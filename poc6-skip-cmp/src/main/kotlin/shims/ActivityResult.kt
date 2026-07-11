// POC 6 Jalon 2/3: androidx.activity.result (aar-only). Loose stubs.
package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContract

class ActivityResultLauncher<I> {
    fun launch(input: I) {}
    fun unregister() {}
}

fun <I, O> androidx.activity.ComponentActivity.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    callback: (O) -> Unit,
): ActivityResultLauncher<I> = ActivityResultLauncher()
