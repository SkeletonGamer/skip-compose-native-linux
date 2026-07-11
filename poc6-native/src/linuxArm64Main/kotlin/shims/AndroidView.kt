package android.view

// Cales compile-only pour Kotlin/Native Linux : surface `android.view` lue par
// le code transpile de SkipUI. `View` n'est utilise que comme type de variable
// nullable ; `WindowManager.LayoutParams` fournit un drapeau de fenetre lu comme
// valeur.

/// Vue Android. Aucun membre n'est appele directement (seul le type sert a
/// declarer une variable `android.view.View?`).
open class View {
    open val parent: View? get() = TODO("K/N View stub")
    open val context: android.content.Context get() = TODO("K/N View stub")
    fun findViewById(id: Int): View? = TODO("K/N View stub")
}

/// Gestionnaire de fenetres. Seul le drapeau `FLAG_KEEP_SCREEN_ON` de
/// `LayoutParams` est lu (pour empecher la mise en veille de l'ecran).
open class WindowManager {
    object LayoutParams {
        /// Maintient l'ecran allume tant que la fenetre est visible.
        const val FLAG_KEEP_SCREEN_ON: Int = 0x00000080
    }
}
