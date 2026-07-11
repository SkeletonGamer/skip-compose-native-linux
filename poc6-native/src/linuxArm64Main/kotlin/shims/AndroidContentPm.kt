package android.content.pm

import android.content.Intent

// Cales compile-only pour Kotlin/Native Linux : surface `android.content.pm`
// lue par le code transpile de SkipFoundation (metadonnees d'application).

/// Point d'acces aux informations de paquets installes.
open class PackageManager {
    // Defauts benins (runtime) : pas d'infos de paquet reelles cote desktop, rien ne leve.
    fun getPackageInfo(packageName: String, flags: Int): PackageInfo? = PackageInfo()
    fun getApplicationLabel(info: ApplicationInfo): CharSequence? = "app"
    fun getLaunchIntentForPackage(packageName: String): Intent? = null

    companion object {
        const val PERMISSION_GRANTED: Int = 0
        const val PERMISSION_DENIED: Int = -1
        /// Drapeau demandant les metadonnees `<meta-data>` du manifeste.
        const val GET_META_DATA: Int = 0
    }
}

/// Informations sur un paquet (version, application associee).
open class PackageInfo {
    var applicationInfo: ApplicationInfo? = null
    var versionName: String? = null
    var longVersionCode: Long = 0L
    var versionCode: Int = 0
}

/// Informations sur l'application (metadonnees, classe, SDK minimum).
open class ApplicationInfo {
    var metaData: android.os.Bundle? = null
    var className: String? = null
    var minSdkVersion: Int = 0
}
