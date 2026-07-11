package coil3.request

// Cale compile-only pour Kotlin/Native Linux : surface de coil3.request referencee par
// SkipUI. coil3 ne publie pas de variante linuxArm64 ; declarations pour COMPILER.

// Resultat d'erreur d'un chargement. Seul `.throwable` est reference
// (AsyncImagePainter.State.Error.result.throwable).
class ErrorResult {
    val throwable: Throwable
        get() = TODO("K/N coil3 stub")
}

// Options d'une requete transmises aux Fetcher / Decoder. Membres reellement lus par
// AsyncImage.kt : context, size, scale.
class Options {
    val context: coil3.PlatformContext
        get() = TODO("K/N coil3 stub")
    val size: coil3.size.Size
        get() = TODO("K/N coil3 stub")
    val scale: coil3.size.Scale
        get() = TODO("K/N coil3 stub")
}

// Requete de chargement d'image. Seul le type est importe (aucune construction dans le
// code transpile POC 6). Le Builder respecte le pattern coil : chainage fluide puis build().
class ImageRequest {
    class Builder(context: coil3.PlatformContext) {
        fun data(data: Any?): Builder = TODO("K/N coil3 stub")
        fun build(): ImageRequest = TODO("K/N coil3 stub")
    }
}
