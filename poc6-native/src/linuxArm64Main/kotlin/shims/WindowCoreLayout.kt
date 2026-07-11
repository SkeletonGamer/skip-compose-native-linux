// POC 6 K/N Linux : androidx.window.core.layout (pas d'artefact K/N Linux).
// Classes de taille de fenêtre référencées par SkipUI (Adaptive/size classes).
package androidx.window.core.layout

class WindowWidthSizeClass private constructor() {
    companion object {
        val COMPACT: WindowWidthSizeClass
            get() = TODO("K/N stub")
        val MEDIUM: WindowWidthSizeClass
            get() = TODO("K/N stub")
        val EXPANDED: WindowWidthSizeClass
            get() = TODO("K/N stub")
    }
}

class WindowHeightSizeClass private constructor() {
    companion object {
        val COMPACT: WindowHeightSizeClass
            get() = TODO("K/N stub")
        val MEDIUM: WindowHeightSizeClass
            get() = TODO("K/N stub")
        val EXPANDED: WindowHeightSizeClass
            get() = TODO("K/N stub")
    }
}

// Conteneur retourné par WindowAdaptiveInfo.windowSizeClass ; SkipUI lit ses deux axes.
class WindowSizeClass {
    val windowWidthSizeClass: WindowWidthSizeClass
        get() = TODO("K/N stub")
    val windowHeightSizeClass: WindowHeightSizeClass
        get() = TODO("K/N stub")
}
