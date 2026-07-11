// POC 6 Jalon 4 : androidx.compose.material.ContentAlpha + pull-refresh, non compilés dans la pile K/N
// (seul material-ripple l'est). Cales compile-only pour SkipUI.
package androidx.compose.material

import androidx.compose.runtime.Composable

object ContentAlpha {
    val high: Float @Composable get() = 1.0f
    val medium: Float @Composable get() = 0.74f
    val disabled: Float @Composable get() = 0.38f
}

// Annotation d'API experimentale material (le module material n'est pas compile en K/N).
@RequiresOptIn
annotation class ExperimentalMaterialApi

// IconButton material (le module material n'est pas compile ; material3 l'est mais SkipUI importe celui de material).
class IconButtonColors

object IconButtonDefaults {
    @Composable
    fun iconButtonColors(): IconButtonColors = IconButtonColors()
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonColors(),
    content: @Composable () -> Unit,
): Unit = TODO("K/N material IconButton stub")

