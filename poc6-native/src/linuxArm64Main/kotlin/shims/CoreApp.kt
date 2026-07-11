// POC 6 K/N Linux : androidx.core.app (pas d'artefact K/N Linux).
// Cales compile-only : ActivityCompat + NotificationCompat.Builder tels que référencés par SkipUI.
package androidx.core.app

object ActivityCompat {
    fun shouldShowRequestPermissionRationale(activity: android.app.Activity, permission: String): Boolean = TODO("K/N stub")
}

object NotificationCompat {
    // Chaîne de construction utilisée par UserNotifications.kt.
    class Builder(context: android.content.Context, channelId: String) {
        fun setContentTitle(title: CharSequence?): Builder = TODO("K/N stub")
        fun setContentText(text: CharSequence?): Builder = TODO("K/N stub")
        fun setAutoCancel(autoCancel: Boolean): Builder = TODO("K/N stub")
        fun setContentIntent(intent: android.app.PendingIntent?): Builder = TODO("K/N stub")
        fun setSmallIcon(icon: androidx.core.graphics.drawable.IconCompat): Builder = TODO("K/N stub")
        fun build(): android.app.Notification = TODO("K/N stub")
    }
}
