package android.os

// Cales compile-only pour Kotlin/Native Linux : reproduisent la surface
// `android.os` lue par le code transpile de SkipFoundation. Aucune logique,
// uniquement de quoi faire compiler la cible K/N sans JVM Android.

/// Constantes de build Android. Le code transpile les lit comme des valeurs
/// (elles sont stockees dans un dictionnaire de proprietes systeme), donc on
/// fournit des valeurs triviales du bon type ("" pour les String, 0 pour Int,
/// 0L pour Long).
object Build {
    const val BOARD: String = ""
    const val BOOTLOADER: String = ""
    const val BRAND: String = ""
    const val DEVICE: String = ""
    const val DISPLAY: String = ""
    const val FINGERPRINT: String = ""
    const val HARDWARE: String = ""
    const val HOST: String = ""
    const val ID: String = ""
    const val MANUFACTURER: String = ""
    const val MODEL: String = ""
    const val ODM_SKU: String = ""
    const val PRODUCT: String = ""
    const val SKU: String = ""
    const val SOC_MANUFACTURER: String = ""
    const val SOC_MODEL: String = ""
    const val TAGS: String = ""
    const val TYPE: String = ""
    const val USER: String = ""

    /// Millisecondes depuis l'epoch UNIX ; le code appelle `.toString()` dessus.
    const val TIME: Long = 0L

    // Listes d'ABI (les usages transpiles sont commentes, mais on garde la
    // surface pour rester coherent avec `Build`).
    val SUPPORTED_32_BIT_ABIS: Array<String> = arrayOf()
    val SUPPORTED_64_BIT_ABIS: Array<String> = arrayOf()
    val SUPPORTED_ABIS: Array<String> = arrayOf()

    object VERSION {
        const val BASE_OS: String = ""
        const val CODENAME: String = ""
        const val INCREMENTAL: String = ""
        const val PREVIEW_SDK_INT: Int = 0
        const val RELEASE: String = ""
        const val SDK_INT: Int = 0
        const val SECURITY_PATCH: String = ""
    }

    object VERSION_CODES {
        /// Paliers d'API compares a `Build.VERSION.SDK_INT` par SkipUI.
        const val P: Int = 28
        const val Q: Int = 29
        const val R: Int = 30
        const val S: Int = 31
        const val S_V2: Int = 32
        const val TIRAMISU: Int = 33
        const val UPSIDE_DOWN_CAKE: Int = 34
    }
}

/// Identite du processus courant.
object Process {
    fun myPid(): Int = TODO("K/N android stub")
}

/// File de messages d'un thread. Le code compare `myLooper()` au
/// `getMainLooper()` pour savoir s'il est sur le thread principal.
open class Looper {
    companion object {
        fun myLooper(): Looper? = TODO("K/N android stub")
        fun getMainLooper(): Looper = TODO("K/N android stub")
    }
}

/// Conteneur cle/valeur Android (distinct de `skip.foundation.Bundle`).
/// Seul `getString` est lu, via `ApplicationInfo.metaData`.
open class Bundle {
    fun getString(key: String): String? = TODO("K/N android stub")
}
