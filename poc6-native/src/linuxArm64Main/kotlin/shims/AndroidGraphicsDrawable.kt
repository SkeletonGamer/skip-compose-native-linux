package android.graphics.drawable

import android.content.res.Resources
import android.graphics.Bitmap

// Cales compile-only pour Kotlin/Native Linux : surface `android.graphics.drawable`
// lue par le code transpile de SkipUI (`AsyncImage.kt`, decodage PDF). Le
// `BitmapDrawable` construit y est en pratique inutilise cote CMP (retour null).

/// Classe de base des drawables Android. Aucun membre n'est lu directement.
open class Drawable

/// Drawable adosse a un `Bitmap`. Seul le constructeur (ressources nullables +
/// bitmap) est reference.
open class BitmapDrawable(res: Resources?, bitmap: Bitmap) : Drawable()
