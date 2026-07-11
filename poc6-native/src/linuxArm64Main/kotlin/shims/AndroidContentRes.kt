package android.content.res

// Cales compile-only pour Kotlin/Native Linux : surface `android.content.res`
// atteinte via `Context.resources.assets` dans le code transpile de
// SkipFoundation (lecture des assets embarques).

/// Ressources de l'application. Seul l'acces au gestionnaire d'assets est lu.
// Defauts benins (runtime) : pas de vraies ressources embarquees cote desktop, mais rien ne leve, pour
// laisser le rendu du ContentView transpile avancer.
open class Resources {
    val assets: AssetManager get() = AssetManager()
    val displayMetrics: DisplayMetrics get() = DisplayMetrics().apply { widthPixels = 1080; heightPixels = 1920; density = 2f; densityDpi = 320; scaledDensity = 2f }
    val configuration: Configuration get() = Configuration()
    fun getIdentifier(name: String, defType: String?, defPackage: String?): Int = 0
    fun getFont(id: Int): android.graphics.Typeface = android.graphics.Typeface()
    fun getString(id: Int): String = ""
    companion object {
        fun getSystem(): Resources = Resources()
    }
}

/// Gestionnaire d'assets Android. `list` enumere un dossier, `open` ouvre un
/// fichier. Le type de retour de `open` reste celui d'Android
/// (`java.io.InputStream`), fourni par une autre couche de cales.
open class AssetManager {
    // Aucun asset embarque cote desktop : liste vide, ouverture => flux vide.
    fun list(path: String): Array<String>? = arrayOf()
    fun open(fileName: String): java.io.InputStream = EmptyInputStream()
}

private class EmptyInputStream : java.io.InputStream() {
    override fun read(): Int = -1
}
