// POC 6 Jalon 2: androidx.core.graphics.drawable.IconCompat (aar-only).
package androidx.core.graphics.drawable

class IconCompat {
    companion object {
        fun createWithBitmap(bitmap: android.graphics.Bitmap): IconCompat = IconCompat()
        fun createWithResource(context: android.content.Context, resId: Int): IconCompat = IconCompat()
    }
}
