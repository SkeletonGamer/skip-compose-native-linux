// POC 6 K/N Linux : androidx.activity.compose (pas d'artefact K/N Linux).
// BackHandler est seulement importé par SkipUI (View.kt / Navigation.kt) : le symbole doit exister.
package androidx.activity.compose

@androidx.compose.runtime.Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit): Unit = TODO("K/N stub")
