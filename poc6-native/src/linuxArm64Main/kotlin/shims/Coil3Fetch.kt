package coil3.fetch

// Cale compile-only pour Kotlin/Native Linux : surface de coil3.fetch referencee par
// AsyncImage.kt (AssetURLFetcher implemente Fetcher / Fetcher.Factory). Compile-only.

// Resultat brut d'une operation de fetch (source ou drawable). Type de base.
interface FetchResult

// Recupere la source de donnees d'une requete. AssetURLFetcher l'implemente.
interface Fetcher {
    suspend fun fetch(): FetchResult

    // Fabrique associant un type de donnees (ici skip.foundation.URL) a un Fetcher.
    interface Factory<T : Any> {
        fun create(data: T, options: coil3.request.Options, imageLoader: coil3.ImageLoader): Fetcher?
    }
}

// Resultat de fetch adosse a une ImageSource. Construit par AssetURLFetcher.fetch() et
// consomme par PdfDecoder (`.source`).
class SourceFetchResult(
    val source: coil3.decode.ImageSource,
    val mimeType: String?,
    val dataSource: coil3.decode.DataSource,
): FetchResult
