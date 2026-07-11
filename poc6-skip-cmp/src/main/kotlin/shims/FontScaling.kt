// POC 6 Jalon 3: androidx.compose.ui.unit.fontscaling (Android non-linear font scaling). Linear stub.
package androidx.compose.ui.unit.fontscaling

interface FontScaleConverter {
    fun convertSpToDp(sp: Float): Float
}

object FontScaleConverterFactory {
    fun isNonLinearFontScalingActive(fontScale: Float): Boolean = false
    fun forScale(fontScale: Float): FontScaleConverter? = object : FontScaleConverter {
        override fun convertSpToDp(sp: Float): Float = sp * fontScale
    }
}
