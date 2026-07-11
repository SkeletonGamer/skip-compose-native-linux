package android.graphics.pdf

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor

// Cale compile-only pour Kotlin/Native Linux : surface `android.graphics.pdf`
// lue par le code transpile de SkipUI (`AsyncImage.kt`, decodeur PDF). Le
// descripteur de fichier provient d'un `ParcelFileDescriptor` (cale android.os).

/// Rend les pages d'un document PDF vers des bitmaps. Construit depuis un
/// descripteur de fichier ouvert en lecture seule.
open class PdfRenderer(fileDescriptor: ParcelFileDescriptor) {
    fun openPage(index: Int): Page = TODO("K/N android stub")
    fun close(): Unit = TODO("K/N android stub")

    /// Une page du document. Le code lit ses dimensions, la rend dans un bitmap
    /// puis la ferme. Les deux parametres nullables de `render` (rectangle de
    /// destination et matrice de transformation) sont toujours passes a `null`,
    /// on les type donc `Any?` pour eviter d'introduire d'autres cales.
    open class Page {
        val width: Int
            get() = TODO("K/N android stub")
        val height: Int
            get() = TODO("K/N android stub")

        fun render(destination: Bitmap, destClip: Any?, transform: Any?, renderMode: Int): Unit = TODO("K/N android stub")
        fun close(): Unit = TODO("K/N android stub")

        companion object {
            /// Mode de rendu adapte a l'affichage ecran.
            const val RENDER_MODE_FOR_DISPLAY: Int = 1
        }
    }
}
