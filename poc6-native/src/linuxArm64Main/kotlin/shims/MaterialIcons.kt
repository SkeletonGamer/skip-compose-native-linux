// Cale compile-only pour Material Icons en Kotlin/Native Linux.
// La lib material-icons-extended n'existe pas en K/N Linux. On reproduit ici la
// structure d'objets de `androidx.compose.material.icons.Icons` (styles imbriques
// Filled/Outlined/Rounded/Sharp/TwoTone + AutoMirrored) et un unique ImageVector
// placeholder partage par toutes les icones. Les proprietes d'extension par icone
// (Icons.Style.NomIcone) sont declarees dans les fichiers de style voisins, chacune
// renvoyant ce meme placeholder.

package androidx.compose.material.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object Icons {
    object Filled
    // `Default` est un alias de `Filled` dans le vrai material-icons.
    val Default = Filled
    object Outlined
    object Rounded
    object Sharp
    object TwoTone

    object AutoMirrored {
        object Filled
        val Default = Filled
        object Outlined
        object Rounded
        object Sharp
        object TwoTone
    }
}

// Unique ImageVector vide, construit une seule fois et partage par toutes les icones.
// Suffisant pour compiler et rendre un placeholder (pas de cache necessaire en K/N).
val _iconPlaceholder: ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).build()
