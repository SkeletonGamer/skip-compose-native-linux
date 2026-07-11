package coil3.decode

// Cale compile-only pour Kotlin/Native Linux : surface de coil3.decode referencee par
// AsyncImage.kt (ImageSource, DataSource, Decoder, DecodeResult, DecodeUtils). coil3
// n'existe pas en linuxArm64 ; declarations pour COMPILER, aucun corps reel.

// Origine des donnees d'une image. Seule l'entree DISK est referencee.
enum class DataSource {
    DISK
}

// Resultat de decodage (image + metadonnees). Type de retour de Decoder.decode().
interface DecodeResult

// Source de donnees d'image adossee a une source okio, avec acces fichier temporaire.
// Referencee par AssetURLFetcher (construction) et PdfDecoder (`source()`, `file()`).
class ImageSource(source: okio.BufferedSource, fileSystem: okio.FileSystem) {
    fun source(): okio.BufferedSource = TODO("K/N coil3 stub")
    fun file(): okio.Path = TODO("K/N coil3 stub")
}

// Decode une SourceFetchResult en image. PdfDecoder l'implemente.
interface Decoder {
    suspend fun decode(): DecodeResult?

    // Fabrique de Decoder pour un resultat de fetch donne.
    interface Factory {
        fun create(result: coil3.fetch.SourceFetchResult, options: coil3.request.Options, imageLoader: coil3.ImageLoader): Decoder?
    }
}

// Utilitaires de decodage. Seul computeSizeMultiplier est reference (variante Double).
object DecodeUtils {
    fun computeSizeMultiplier(
        srcWidth: Double,
        srcHeight: Double,
        dstWidth: Double,
        dstHeight: Double,
        scale: coil3.size.Scale,
    ): Double = TODO("K/N coil3 stub")
}
