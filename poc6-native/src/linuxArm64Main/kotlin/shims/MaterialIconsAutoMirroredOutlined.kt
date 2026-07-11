// Cales compile-only pour Material Icons en Kotlin/Native Linux.
// La lib material-icons-extended n'existe pas en K/N Linux : chaque icone
// referencee par le code transpile est exposee comme propriete d'extension
// qui renvoie un unique ImageVector placeholder partage (_iconPlaceholder).
// Fichier genere : une propriete par icone distincte reellement referencee.

package androidx.compose.material.icons.automirrored.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons._iconPlaceholder
import androidx.compose.ui.graphics.vector.ImageVector

val Icons.AutoMirrored.Outlined.ArrowBack: ImageVector get() = _iconPlaceholder
val Icons.AutoMirrored.Outlined.ArrowForward: ImageVector get() = _iconPlaceholder
val Icons.AutoMirrored.Outlined.ExitToApp: ImageVector get() = _iconPlaceholder
val Icons.AutoMirrored.Outlined.KeyboardArrowLeft: ImageVector get() = _iconPlaceholder
val Icons.AutoMirrored.Outlined.KeyboardArrowRight: ImageVector get() = _iconPlaceholder
val Icons.AutoMirrored.Outlined.List: ImageVector get() = _iconPlaceholder
val Icons.AutoMirrored.Outlined.Send: ImageVector get() = _iconPlaceholder
