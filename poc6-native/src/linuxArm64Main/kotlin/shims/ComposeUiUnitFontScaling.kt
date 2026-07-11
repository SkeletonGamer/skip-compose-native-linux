// POC 6 Jalon 4 : androidx.compose.ui.unit.fontscaling (Android-only, ScaledMetric de SkipUI). Cale K/N.
package androidx.compose.ui.unit.fontscaling

class FontScaleConverter {
    fun convertSpToDp(sp: Float): Float = TODO("K/N fontscaling stub")
    fun convertDpToSp(dp: Float): Float = TODO("K/N fontscaling stub")
}

object FontScaleConverterFactory {
    fun forScale(fontScale: Float): FontScaleConverter? = TODO("K/N fontscaling stub")
    fun isNonLinearFontScalingActive(fontScale: Float): Boolean = TODO("K/N fontscaling stub")
}
