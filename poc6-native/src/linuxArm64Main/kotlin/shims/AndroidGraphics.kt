package android.graphics

import android.content.ContentResolver
import android.net.Uri

// Cales compile-only pour Kotlin/Native Linux : surface `android.graphics` lue
// par le code transpile de SkipUI (chemins, bitmaps, typographie, mesure de
// chemin, decodage d'image). Aucune logique : de quoi faire compiler la cible
// K/N sans JVM Android. Les types compose (`androidx...Path`) et java
// (`java.io.*`, `java.nio.ByteBuffer`) sont fournis ailleurs, on ne les recree
// pas ; on ne declare que les membres reellement references.

/// Chemin graphique Android. Obtenu via `androidx...asAndroidPath()` puis
/// complete avec des primitives geometriques. Seules les methodes appelees par
/// SkipUI (`Path.kt`, `Shape.kt`) sont declarees.
open class Path {
    /// Sens d'enroulement d'une primitive fermee. Seul `CW` est lu.
    enum class Direction { CW, CCW }

    fun addRect(left: Float, top: Float, right: Float, bottom: Float, dir: Direction): Unit = TODO("K/N android stub")
    fun addRoundRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, dir: Direction): Unit = TODO("K/N android stub")
    fun addRoundRect(left: Float, top: Float, right: Float, bottom: Float, radii: FloatArray, dir: Direction): Unit = TODO("K/N android stub")
    fun addOval(left: Float, top: Float, right: Float, bottom: Float, dir: Direction): Unit = TODO("K/N android stub")
}

/// Mesure la longueur d'un chemin et en extrait des segments (utilise pour
/// le rognage `trim` des formes). Le second parametre du constructeur indique
/// si le chemin doit etre force en boucle fermee.
open class PathMeasure(path: Path, forceClosed: Boolean) {
    fun getLength(): Float = TODO("K/N android stub")
    fun getSegment(startD: Float, stopD: Float, dst: Path, startWithMoveTo: Boolean): Boolean = TODO("K/N android stub")
}

/// Image bitmap Android. Le code lit ses dimensions, la redimensionne, la
/// compresse et la convertit (via l'extension androidx `asImageBitmap`, fournie
/// ailleurs). Seuls les membres references sont declares.
open class Bitmap {
    /// Format de couleur d'un bitmap. Seul `ARGB_8888` est lu.
    enum class Config { ARGB_8888 }

    /// Format de compression a l'export. `JPEG` et `PNG` sont references.
    enum class CompressFormat { JPEG, PNG, WEBP }

    fun getWidth(): Int = TODO("K/N android stub")
    fun getHeight(): Int = TODO("K/N android stub")
    fun compress(format: CompressFormat, quality: Int, stream: java.io.OutputStream): Boolean = TODO("K/N android stub")

    companion object {
        fun createBitmap(width: Int, height: Int, config: Config): Bitmap = TODO("K/N android stub")
        fun createScaledBitmap(src: Bitmap, dstWidth: Int, dstHeight: Int, filter: Boolean): Bitmap = TODO("K/N android stub")
    }
}

/// Fabrique de bitmaps a partir de flux ou de tableaux d'octets (chemin de
/// repli avant l'API `ImageDecoder`). Retours nullables comme sur Android.
object BitmapFactory {
    fun decodeByteArray(data: ByteArray, offset: Int, length: Int): Bitmap? = TODO("K/N android stub")
    fun decodeStream(stream: java.io.InputStream): Bitmap? = TODO("K/N android stub")
}

/// Decodeur d'image moderne (API 28+). Le code construit une `Source` depuis un
/// `ByteBuffer` ou un `ContentResolver`+`Uri`, puis decode un `Bitmap`.
object ImageDecoder {
    /// Source opaque d'image (fichier, buffer, resolveur de contenu).
    open class Source

    fun createSource(buffer: java.nio.ByteBuffer): Source = TODO("K/N android stub")
    fun createSource(resolver: ContentResolver, uri: Uri): Source = TODO("K/N android stub")
    fun decodeBitmap(source: Source): Bitmap = TODO("K/N android stub")
}

/// Fonte Android. Le code cree une fonte systeme par nom et style ; le resultat
/// est en pratique remplace par `FontFamily.Default` cote CMP. Seuls
/// `create` et la constante de style `NORMAL` sont lus.
open class Typeface {
    companion object {
        /// Style normal (ni gras ni italique).
        const val NORMAL: Int = 0
        fun create(familyName: String, style: Int): Typeface? = TODO("K/N android stub")
    }
}

/// Utilitaires de couleur. Seule la conversion RVB vers TSV (HSV) est appelee ;
/// elle ecrit son resultat dans le tableau `hsv` fourni.
object Color {
    fun RGBToHSV(red: Int, green: Int, blue: Int, hsv: FloatArray): Unit = TODO("K/N android stub")
}
