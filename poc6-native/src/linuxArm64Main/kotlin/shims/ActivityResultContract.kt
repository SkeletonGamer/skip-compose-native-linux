// POC 6 K/N Linux : androidx.activity.result.contract (pas d'artefact K/N Linux).
// Fichier distinct car un seul package par fichier ; seule RequestPermission est référencée par SkipUI.
package androidx.activity.result.contract

abstract class ActivityResultContract<I, O>

class ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>()
}
