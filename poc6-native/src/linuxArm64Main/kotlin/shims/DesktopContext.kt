// POC 6 Jalon 5 (runtime) : un Context desktop fonctionnel a defauts benins, pour le bootstrap de
// SkipFoundation (ProcessInfo.androidContext) cote K/N Linux. Rien ne leve : le rendu du ContentView
// transpile peut avancer (ressources vides, prefs en memoire, repertoire temporaire).
package android.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import java.io.File

object DesktopContext : Context {
    private val prefs = DesktopSharedPreferences()
    private val res = Resources()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
    override fun getPackageManager(): PackageManager = PackageManager()
    override fun getPackageName(): String = "app.skip.desktop"
    override fun getApplicationInfo(): ApplicationInfo = ApplicationInfo()
    override fun getCacheDir(): File = File("/tmp")
    override fun getFilesDir(): File = File("/tmp")
    override fun getContentResolver(): ContentResolver = ContentResolver()
    override fun getSystemService(name: String): Any? = null
    override fun startActivity(intent: Intent) { }
    override fun getApplicationContext(): Context = this
    override val applicationContext: Context get() = this
    override val contentResolver: ContentResolver get() = ContentResolver()
    override val packageName: String get() = "app.skip.desktop"
    override val mainExecutor: java.util.concurrent.Executor get() = object : java.util.concurrent.Executor {
        override fun execute(command: java.lang.Runnable) { command() }
    }
    override val cacheDir: File get() = File("/tmp")
    override val resources: Resources get() = res
}

// Preferences en memoire (aucune persistance) : suffit pour que UserDefaults ne leve pas au bootstrap.
class DesktopSharedPreferences : SharedPreferences {
    private val map = HashMap<String, Any?>()
    override fun getAll(): Map<String, *> = map
    override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun edit(): SharedPreferences.Editor = DesktopEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) { }

    private class DesktopEditor(private val map: HashMap<String, Any?>) : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor { map[key] = value; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { map[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { map[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { map[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { map[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { map.remove(key); return this }
        override fun clear(): SharedPreferences.Editor { map.clear(); return this }
        override fun apply() { }
        override fun commit(): Boolean = true
    }
}
