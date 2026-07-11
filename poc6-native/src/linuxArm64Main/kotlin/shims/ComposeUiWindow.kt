// POC 6 Jalon 4 : androidx.compose.ui.window.DialogWindowProvider (Android-only : SkipUI y accede pour
// regler la fenetre de dialogue). Absent de la pile K/N. Cale compile-only.
package androidx.compose.ui.window

interface DialogWindowProvider {
    val window: android.view.Window
}
