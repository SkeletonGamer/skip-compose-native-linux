package android.net

// Cale compile-only pour Kotlin/Native Linux : surface `android.net` lue par le
// code transpile de SkipFoundation (conversion d'URL).

/// URI Android. Le code appelle `Uri.parse(...)` puis `toString()` (herite).
open class Uri {
    companion object {
        fun parse(uriString: String): Uri = TODO("K/N android stub")
    }
}
