package android.content

import android.net.Uri

// Cales compile-only pour Kotlin/Native Linux : complement de `android.content`
// (le `Context`, `ContentResolver`, `Intent`... sont dans AndroidContent.kt et
// ne sont pas redefinis ici). On couvre le presse-papiers (`UIPasteboard.kt`),
// l'acces aux services systeme (`Context.getSystemService`) et les noms de
// service lus comme valeurs.

/// Gestionnaire du presse-papiers. Obtenu via `context.getSystemService(...)`
/// puis interroge/modifie. Le clip courant est nullable comme sur Android.
open class ClipboardManager {
    fun hasPrimaryClip(): Boolean = TODO("K/N android stub")
    fun getPrimaryClip(): ClipData? = TODO("K/N android stub")
    fun setPrimaryClip(clip: ClipData): Unit = TODO("K/N android stub")
    fun clearPrimaryClip(): Unit = TODO("K/N android stub")
    fun addPrimaryClipChangedListener(listener: OnPrimaryClipChangedListener): Unit = TODO("K/N android stub")

    /// Callback declenche a chaque changement du clip principal.
    fun interface OnPrimaryClipChangedListener {
        fun onPrimaryClipChanged(): Unit
    }
}

/// Donnees du presse-papiers : une ou plusieurs entrees (`Item`), chacune
/// portant du texte ou une `Uri`. Construite via les fabriques `newPlainText`
/// et `newRawUri`, puis enrichie par `addItem`.
open class ClipData {
    fun getItemCount(): Int = TODO("K/N android stub")
    fun getItemAt(index: Int): Item = TODO("K/N android stub")
    fun addItem(item: Item): Unit = TODO("K/N android stub")

    /// Une entree du clip. Porte du texte ou une `Uri`. `coerceToText`
    /// convertit le contenu en texte affichable (a l'aide du `Context`).
    open class Item {
        constructor(text: CharSequence)
        constructor(uri: Uri)

        fun getText(): CharSequence? = TODO("K/N android stub")
        fun getUri(): Uri? = TODO("K/N android stub")
        fun coerceToText(context: Context): CharSequence = TODO("K/N android stub")
    }

    companion object {
        fun newPlainText(label: CharSequence, text: CharSequence): ClipData = TODO("K/N android stub")
        fun newRawUri(label: CharSequence, uri: Uri): ClipData = TODO("K/N android stub")
    }
}

// Acces aux services systeme. `Context` etant une interface (AndroidContent.kt),
// on ne peut pas lui ajouter de membre : `getSystemService` est fournie en
// fonction d'extension, et les noms de service en proprietes d'extension sur le
// companion (`Context.CLIPBOARD_SERVICE`, etc.). Valeurs = chaines Android
// reelles, lues telles quelles par le code.

/// Recupere un service systeme par nom. Le code caste le retour (`as?`).
fun Context.getSystemService(name: String): Any? = TODO("K/N android stub")

val Context.Companion.CLIPBOARD_SERVICE: String get() = "clipboard"
val Context.Companion.NOTIFICATION_SERVICE: String get() = "notification"
val Context.Companion.VIBRATOR_MANAGER_SERVICE: String get() = "vibrator_manager"
val Context.Companion.ACCESSIBILITY_SERVICE: String get() = "accessibility"
val Context.Companion.UI_MODE_SERVICE: String get() = "uimode"
