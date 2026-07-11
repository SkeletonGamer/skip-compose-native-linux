// POC 6 Jalon 4 : android.database.ContentObserver (AccessibilityEnvironment en herite). Cale K/N.
package android.database

open class ContentObserver(handler: android.os.Handler?) {
    open fun onChange(selfChange: Boolean) { }
    open fun onChange(selfChange: Boolean, uri: android.net.Uri?) { }
    open fun deliverSelfNotifications(): Boolean = false
}
