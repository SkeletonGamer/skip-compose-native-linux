package coil3.size

// Cale compile-only pour Kotlin/Native Linux : surface de coil3.size referencee par
// AsyncImage.kt (dimensions et echelle d'une requete). Compile-only, aucun corps reel.

// Dimension d'une requete : soit un nombre de pixels, soit "indefini". Seul Pixels est
// reference (cast `as? Dimension.Pixels` + `.px`).
sealed class Dimension {
    class Pixels(val px: Int): Dimension()
}

// Taille cible d'une requete de decodage. Referencee via Options.size.width / .height.
class Size(val width: Dimension, val height: Dimension)

// Strategie de mise a l'echelle. Referencee via Options.scale (passee a
// DecodeUtils.computeSizeMultiplier).
enum class Scale {
    FIT,
    FILL
}
