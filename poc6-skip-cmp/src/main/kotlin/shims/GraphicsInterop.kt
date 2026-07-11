// POC 6 Jalon 3: compose <-> android.graphics interop, Android-only. Stubs (unused on the desktop render path).
package androidx.compose.ui.graphics

fun Path.asAndroidPath(): android.graphics.Path = android.graphics.Path()

fun android.graphics.Path.asComposePath(): Path = Path()

fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = ImageBitmap(1, 1)
