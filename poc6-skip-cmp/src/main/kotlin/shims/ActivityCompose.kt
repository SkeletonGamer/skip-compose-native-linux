// POC 6 Jalon 3: androidx.activity.compose.BackHandler (aar-only). No-op composable.
package androidx.activity.compose

import androidx.compose.runtime.Composable

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {}
