// POC 6 Jalon 2: androidx.window size classes (aar-only, no JVM variant).
package androidx.window.core.layout

class WindowWidthSizeClass private constructor(val name: String) {
    companion object {
        val COMPACT = WindowWidthSizeClass("COMPACT")
        val MEDIUM = WindowWidthSizeClass("MEDIUM")
        val EXPANDED = WindowWidthSizeClass("EXPANDED")
    }
}

class WindowHeightSizeClass private constructor(val name: String) {
    companion object {
        val COMPACT = WindowHeightSizeClass("COMPACT")
        val MEDIUM = WindowHeightSizeClass("MEDIUM")
        val EXPANDED = WindowHeightSizeClass("EXPANDED")
    }
}

class WindowSizeClass {
    val windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT
    val windowHeightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.COMPACT
}
