// POC 6 Jalon 2: Android DialogWindowProvider (access the platform Window from a dialog).
package androidx.compose.ui.window

interface DialogWindowProvider {
    val window: android.view.Window
}
