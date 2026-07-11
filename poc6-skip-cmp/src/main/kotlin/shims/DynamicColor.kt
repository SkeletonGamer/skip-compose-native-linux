// POC 6 Jalon 2: Material You dynamic color is Android-only. Fall back to static schemes.
package androidx.compose.material3

fun dynamicLightColorScheme(context: android.content.Context): ColorScheme = lightColorScheme()
fun dynamicDarkColorScheme(context: android.content.Context): ColorScheme = darkColorScheme()
