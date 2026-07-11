// POC 6 Jalon 2: material3.adaptive (aar-only). Report a single COMPACT window.
package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

class WindowAdaptiveInfo(val windowSizeClass: WindowSizeClass)

@Composable
fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo = WindowAdaptiveInfo(WindowSizeClass())
