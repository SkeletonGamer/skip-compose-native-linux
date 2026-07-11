// POC 6 Jalon 4 : interop android.graphics <-> compose.ui.graphics (Android-only, absent de la pile K/N).
// Cales compile-only pour SkipUI (Path/Bitmap).
package androidx.compose.ui.graphics

fun Path.asAndroidPath(): android.graphics.Path = TODO("K/N graphics interop stub")
fun android.graphics.Path.asComposePath(): Path = TODO("K/N graphics interop stub")
fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = TODO("K/N graphics interop stub")
