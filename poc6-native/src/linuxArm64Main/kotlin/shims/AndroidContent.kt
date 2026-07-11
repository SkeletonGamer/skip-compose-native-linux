package android.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri

// Cales compile-only pour Kotlin/Native Linux : surface `android.content` lue
// par le code transpile de SkipFoundation (Context, preferences, intents).

/// Contexte applicatif Android. Seuls les accesseurs reellement appeles sont
/// declares (preferences, package manager, ressources, repertoires, resolveur
/// de contenu). Le code lit `cacheDir` a la fois en propriete et en methode.
interface Context {
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences
    fun getPackageManager(): PackageManager
    fun getPackageName(): String
    fun getApplicationInfo(): ApplicationInfo
    fun getCacheDir(): java.io.File
    fun getFilesDir(): java.io.File
    fun getContentResolver(): ContentResolver
    fun getSystemService(name: String): Any?
    fun startActivity(intent: Intent)
    fun getApplicationContext(): Context
    val cacheDir: java.io.File
    val resources: Resources
    val applicationContext: Context
    val contentResolver: ContentResolver
    val packageName: String
    val mainExecutor: java.util.concurrent.Executor

    companion object {
        /// Mode d'ouverture prive des preferences partagees.
        const val MODE_PRIVATE: Int = 0
        // Noms des services systeme lus par SkipUI (valeurs reelles Android).
        const val CLIPBOARD_SERVICE: String = "clipboard"
        const val NOTIFICATION_SERVICE: String = "notification"
        const val VIBRATOR_SERVICE: String = "vibrator"
        const val VIBRATOR_MANAGER_SERVICE: String = "vibrator_manager"
        const val WINDOW_SERVICE: String = "window"
        const val UI_MODE_SERVICE: String = "uimode"
        const val ACCESSIBILITY_SERVICE: String = "accessibility"
    }
}

/// Resolveur de contenu Android. Le code ouvre un flux depuis une `Uri`
/// "content://". Le type de retour reste celui d'Android (`java.io.InputStream`,
/// nullable), fourni par une autre couche de cales.
open class ContentResolver {
    fun openInputStream(uri: Uri): java.io.InputStream? = TODO("K/N android stub")
    fun registerContentObserver(uri: Uri, notifyForDescendants: Boolean, observer: android.database.ContentObserver) { TODO("K/N android stub") }
    fun unregisterContentObserver(observer: android.database.ContentObserver) { TODO("K/N android stub") }
}

/// Preferences partagees Android (backing store de `UserDefaults`).
interface SharedPreferences {
    fun getAll(): Map<String, *>
    fun getInt(key: String, defValue: Int): Int
    fun edit(): Editor
    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener)

    /// Editeur transactionnel des preferences.
    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun apply()
        fun commit(): Boolean
    }

    /// Callback de changement (converti depuis une lambda SAM par le code).
    fun interface OnSharedPreferenceChangeListener {
        fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?)
    }
}

/// Intent Android (action + extras). SkipUI en construit pour ouvrir des ecrans systeme.
open class Intent {
    constructor()
    constructor(action: String?)
    constructor(action: String?, uri: Uri?)
    constructor(context: Context, cls: Any?)
    var action: String? = null
    val dataString: String? get() = TODO("K/N android stub")
    fun getComponent(): ComponentName? = TODO("K/N android stub")
    fun putExtra(name: String, value: String?): Intent = this
    fun putExtra(name: String, value: Int): Intent = this
    fun putExtra(name: String, value: Long): Intent = this
    fun putExtra(name: String, value: Boolean): Intent = this
    fun setData(data: Uri): Intent = this
    fun setFlags(flags: Int): Intent = this
    fun addFlags(flags: Int): Intent = this
    var type: String? = null
    fun setType(type: String?): Intent = this
    companion object {
        const val FLAG_ACTIVITY_NEW_TASK: Int = 0x10000000
        const val ACTION_VIEW: String = "android.intent.action.VIEW"
        const val ACTION_SEND: String = "android.intent.action.SEND"
        const val ACTION_DIAL: String = "android.intent.action.DIAL"
        const val ACTION_SENDTO: String = "android.intent.action.SENDTO"
        const val EXTRA_TEXT: String = "android.intent.extra.TEXT"
        const val EXTRA_SUBJECT: String = "android.intent.extra.SUBJECT"
        fun createChooser(target: Intent, title: CharSequence?): Intent = target
    }
}

/// Reference a un composant (paquet + classe). Seul le nom de classe est lu.
open class ComponentName {
    fun getClassName(): String = TODO("K/N android stub")
}

/// ContextWrapper delegue au contexte de base (PresentationRoot deroule les wrappers via baseContext).
open class ContextWrapper(base: Context) : Context by base {
    val baseContext: Context = base
}
