// POC 6 Jalon 4 : androidx.compose.material3.adaptive (+ navigationsuite), non compilé dans la pile K/N.
// Cales compile-only pour le TabView de SkipUI.
package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable

class WindowAdaptiveInfo {
    val windowSizeClass: androidx.window.core.layout.WindowSizeClass get() = TODO("K/N adaptive stub")
}

@Composable
fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo = TODO("K/N adaptive stub")
