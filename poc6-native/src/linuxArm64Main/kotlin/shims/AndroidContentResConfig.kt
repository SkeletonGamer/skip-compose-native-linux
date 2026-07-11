// POC 6 Jalon 4 : android.content.res.Configuration + DisplayMetrics (LocalConfiguration, EnvironmentValues).
package android.content.res

class Configuration {
    constructor()
    constructor(other: Configuration)
    var uiMode: Int = 0
    var orientation: Int = 0
    var screenWidthDp: Int = 0
    var screenHeightDp: Int = 0
    var densityDpi: Int = 0
    var fontScale: Float = 1.0f
    var fontWeightAdjustment: Int = 0
    val locales: LocaleList get() = LocaleList(java.util.Locale.getDefault())
    fun setLocale(locale: java.util.Locale) { }
    companion object {
        const val UI_MODE_NIGHT_MASK: Int = 0x30
        const val UI_MODE_NIGHT_YES: Int = 0x20
        const val UI_MODE_NIGHT_NO: Int = 0x10
        const val ORIENTATION_PORTRAIT: Int = 1
        const val ORIENTATION_LANDSCAPE: Int = 2
        const val FONT_WEIGHT_ADJUSTMENT_UNDEFINED: Int = -2147483648
    }
}

class LocaleList(private vararg val list: java.util.Locale) {
    operator fun get(index: Int): java.util.Locale = list.getOrElse(index) { java.util.Locale.getDefault() }
    fun size(): Int = if (list.isEmpty()) 1 else list.size
    fun isEmpty(): Boolean = false
}

class DisplayMetrics {
    var widthPixels: Int = 0
    var heightPixels: Int = 0
    var density: Float = 1.0f
    var densityDpi: Int = 0
    var scaledDensity: Float = 1.0f
}
