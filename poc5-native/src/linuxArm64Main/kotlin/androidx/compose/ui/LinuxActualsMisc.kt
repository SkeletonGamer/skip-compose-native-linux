// POC 5 Jalon 4: the two non-postDelayed actuals that lived in the excluded Actuals.nonJvm.kt
// (PostDelayedDispatcher is gone with the excluded Actuals.skiko.kt that declared its expect).
package androidx.compose.ui

import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

internal actual fun classKeyForObject(a: Any): Any = a::class

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun InspectorInfo.tryPopulateReflectively(element: ModifierNodeElement<*>) {
}
