package android.provider

import android.content.ContentResolver
import android.net.Uri

// Cales compile-only pour Kotlin/Native Linux : surface `android.provider`
// lue par le code transpile de SkipUI (`UIApplication.kt` pour les intents de
// reglages, `AccessibilityEnvironment.kt` pour les reglages d'accessibilite).
// Les constantes d'action/extra sont lues comme valeurs : on fournit leurs
// vraies chaines Android. Les getters de reglages renvoient une valeur triviale.

/// Point d'acces aux reglages systeme Android. Le code lit des constantes
/// d'action (pour construire des `Intent`/URL) et interroge les tables `Secure`
/// et `Global`.
object Settings {
    const val ACTION_APP_NOTIFICATION_SETTINGS: String = "android.settings.APP_NOTIFICATION_SETTINGS"
    const val ACTION_APPLICATION_DETAILS_SETTINGS: String = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val EXTRA_APP_PACKAGE: String = "android.provider.extra.APP_PACKAGE"

    /// Reglages securises (par utilisateur, en lecture seule pour les apps).
    object Secure {
        const val ACCESSIBILITY_DISPLAY_INVERSION_ENABLED: String = "accessibility_display_inversion_enabled"

        fun getInt(cr: ContentResolver, name: String, def: Int): Int = TODO("K/N android stub")
        fun getUriFor(name: String): Uri = TODO("K/N android stub")
    }

    /// Reglages globaux (partages par tous les utilisateurs de l'appareil).
    object Global {
        const val ANIMATOR_DURATION_SCALE: String = "animator_duration_scale"

        fun getInt(cr: ContentResolver, name: String, def: Int): Int = TODO("K/N android stub")
        fun getFloat(cr: ContentResolver, name: String, def: Float): Float = TODO("K/N android stub")
        fun getUriFor(name: String): Uri = TODO("K/N android stub")
    }
}
