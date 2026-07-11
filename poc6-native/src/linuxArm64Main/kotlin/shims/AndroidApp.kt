// POC 6 Jalon 4 : surface android.app referencee par SkipUI (UIApplication, ShareLink, notifications).
// Activity implemente Context (WorkManager.getInstance(activity), etc.). Cales compile-only K/N.
package android.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources

open class Activity : Context {
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = TODO("K/N app stub")
    override fun getPackageManager(): PackageManager = TODO("K/N app stub")
    override fun getPackageName(): String = TODO("K/N app stub")
    override fun getApplicationInfo(): ApplicationInfo = TODO("K/N app stub")
    override fun getCacheDir(): java.io.File = TODO("K/N app stub")
    override fun getFilesDir(): java.io.File = TODO("K/N app stub")
    override fun getContentResolver(): android.content.ContentResolver = TODO("K/N app stub")
    override fun getSystemService(name: String): Any? = TODO("K/N app stub")
    override fun startActivity(intent: Intent) { TODO("K/N app stub") }
    override fun getApplicationContext(): Context = this
    override val cacheDir: java.io.File get() = TODO("K/N app stub")
    override val resources: Resources get() = TODO("K/N app stub")
    override val applicationContext: Context get() = this
    override val contentResolver: android.content.ContentResolver get() = TODO("K/N app stub")
    override val packageName: String get() = TODO("K/N app stub")
    override val mainExecutor: java.util.concurrent.Executor get() = TODO("K/N app stub")

    open val window: android.view.Window get() = TODO("K/N app stub")
    var intent: Intent? = null
    val isDestroyed: Boolean get() = TODO("K/N app stub")
    fun runOnUiThread(action: () -> Unit) { TODO("K/N app stub") }
    fun finish() { TODO("K/N app stub") }
}

open class Application : Activity() {
    fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) { TODO("K/N app stub") }
    fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) { TODO("K/N app stub") }

    interface ActivityLifecycleCallbacks {
        fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) { }
        fun onActivityStarted(activity: Activity) { }
        fun onActivityResumed(activity: Activity) { }
        fun onActivityPaused(activity: Activity) { }
        fun onActivityStopped(activity: Activity) { }
        fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) { }
        fun onActivityDestroyed(activity: Activity) { }
    }
}

class Service

class PendingIntent {
    companion object {
        const val FLAG_IMMUTABLE: Int = 0x04000000
        const val FLAG_UPDATE_CURRENT: Int = 0x08000000
        fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent = TODO("K/N app stub")
    }
}

class Notification
class NotificationChannel(id: String, name: CharSequence, importance: Int)

class NotificationManager {
    fun notify(id: Int, notification: Notification) { TODO("K/N app stub") }
    fun createNotificationChannel(channel: NotificationChannel) { TODO("K/N app stub") }
    companion object {
        const val IMPORTANCE_DEFAULT: Int = 3
        const val IMPORTANCE_HIGH: Int = 4
    }
}

class UiModeManager {
    fun getNightMode(): Int = TODO("K/N app stub")
    val currentModeType: Int get() = TODO("K/N app stub")
    fun getContrast(): Float = TODO("K/N app stub")
    fun addContrastChangeListener(executor: java.util.concurrent.Executor, listener: ContrastChangeListener) { TODO("K/N app stub") }

    fun interface ContrastChangeListener {
        fun onContrastChanged(contrast: Float)
    }

    companion object {
        const val MODE_NIGHT_YES: Int = 2
        const val MODE_NIGHT_NO: Int = 1
    }
}
