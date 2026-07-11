// POC 6 K/N Linux : androidx.activity.result (pas d'artefact K/N Linux).
// Cales compile-only : lanceur + fabrique registerForActivityResult utilisés par SkipUI.
package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContract

// Referencé : launch(input). unregister n'est pas utilisé par SkipUI, donc non exposé.
class ActivityResultLauncher<I> {
    fun launch(input: I): Unit = TODO("K/N stub")
}

// Appelé en extension sur une ComponentActivity : registerForActivityResult(contract) { result -> }.
fun <I, O> androidx.activity.ComponentActivity.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    callback: (O) -> Unit,
): ActivityResultLauncher<I> = TODO("K/N stub")
