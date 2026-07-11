// POC 6 K/N Linux : androidx.core.view (pas d'artefact K/N Linux).
// Cales compile-only ; seuls les membres referencés par SkipUI sont exposés.
// Les corps lèvent TODO : le but est de COMPILER, pas d'exécuter côté K/N.
package androidx.core.view

// Referencé : WindowInsetsCompat.Type.statusBars(), WindowInsetsCompat.isVisible(Int).
class WindowInsetsCompat {
    fun isVisible(typeMask: Int): Boolean = TODO("K/N stub")

    object Type {
        fun statusBars(): Int = TODO("K/N stub")
    }
}

// Referencé : systemBarsBehavior (var), isAppearanceLight*Bars (var), hide/show(Int),
// et la constante BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE.
class WindowInsetsControllerCompat {
    var systemBarsBehavior: Int
        get() = TODO("K/N stub")
        set(value) = TODO("K/N stub")

    var isAppearanceLightStatusBars: Boolean
        get() = TODO("K/N stub")
        set(value) = TODO("K/N stub")

    var isAppearanceLightNavigationBars: Boolean
        get() = TODO("K/N stub")
        set(value) = TODO("K/N stub")

    fun hide(types: Int): Unit = TODO("K/N stub")
    fun show(types: Int): Unit = TODO("K/N stub")

    companion object {
        val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE: Int
            get() = TODO("K/N stub")
    }
}

// SAM passé à ViewCompat.setOnApplyWindowInsetsListener : (View, WindowInsetsCompat) -> WindowInsetsCompat.
fun interface OnApplyWindowInsetsListener {
    fun onApplyWindowInsets(v: android.view.View, insets: WindowInsetsCompat): WindowInsetsCompat
}

object ViewCompat {
    fun setOnApplyWindowInsetsListener(view: android.view.View, listener: OnApplyWindowInsetsListener?): Unit = TODO("K/N stub")
    fun onApplyWindowInsets(view: android.view.View, insets: WindowInsetsCompat): WindowInsetsCompat = TODO("K/N stub")
}

object WindowCompat {
    fun getInsetsController(window: android.view.Window, view: android.view.View): WindowInsetsControllerCompat = TODO("K/N stub")
}
