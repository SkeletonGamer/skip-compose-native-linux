// POC 6 Jalon 2/3: androidx.core.view insets helpers (aar-only). Loose stubs.
package androidx.core.view

class WindowInsetsCompat {
    fun getInsets(typeMask: Int): androidx.core.graphics.Insets = androidx.core.graphics.Insets()
    fun isVisible(typeMask: Int): Boolean = true
    object Type {
        fun systemBars(): Int = 0
        fun statusBars(): Int = 1
        fun navigationBars(): Int = 2
        fun ime(): Int = 3
    }
}

class WindowInsetsControllerCompat {
    var systemBarsBehavior: Int = 0
    var isAppearanceLightStatusBars: Boolean = false
    var isAppearanceLightNavigationBars: Boolean = false
    fun hide(types: Int) {}
    fun show(types: Int) {}
    fun isVisible(type: Int): Boolean = true
    companion object {
        const val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2
    }
}

fun interface OnApplyWindowInsetsListener {
    fun onApplyWindowInsets(v: android.view.View, insets: WindowInsetsCompat): WindowInsetsCompat
}

object ViewCompat {
    fun setOnApplyWindowInsetsListener(view: android.view.View, listener: OnApplyWindowInsetsListener?) {}
    fun onApplyWindowInsets(view: android.view.View, insets: WindowInsetsCompat): WindowInsetsCompat = insets
}

object WindowCompat {
    fun getInsetsController(window: android.view.Window, view: android.view.View): WindowInsetsControllerCompat =
        WindowInsetsControllerCompat()
    fun setDecorFitsSystemWindows(window: android.view.Window, decorFits: Boolean) {}
}
