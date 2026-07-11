package android.util

// Cales compile-only pour Kotlin/Native Linux : surface `android.util` lue par
// le code transpile de SkipFoundation (journalisation et parsing XML).

/// Journalisation Android. Toutes les methodes renvoient un Int (nombre
/// d'octets ecrits sous Android). Le code appelle les variantes (tag, message).
object Log {
    // Journalise sur stdout (utile pour voir les logs Skip pendant le run) et renvoie 0.
    private fun log(level: String, tag: String, msg: String): Int { println("[$level/$tag] $msg"); return 0 }
    fun v(tag: String, msg: String): Int = log("V", tag, msg)
    fun v(tag: String, msg: String, tr: Throwable?): Int = log("V", tag, msg)
    fun d(tag: String, msg: String): Int = log("D", tag, msg)
    fun d(tag: String, msg: String, tr: Throwable?): Int = log("D", tag, msg)
    fun i(tag: String, msg: String): Int = log("I", tag, msg)
    fun i(tag: String, msg: String, tr: Throwable?): Int = log("I", tag, msg)
    fun w(tag: String, msg: String): Int = log("W", tag, msg)
    fun w(tag: String, msg: String, tr: Throwable?): Int = log("W", tag, msg)
    fun e(tag: String, msg: String): Int = log("E", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable?): Int = log("E", tag, msg)
    fun wtf(tag: String, msg: String): Int = log("WTF", tag, msg)
    fun wtf(tag: String, msg: String, tr: Throwable?): Int = log("WTF", tag, msg)
}

/// Fabrique de parsers XML pull. Le type de retour reste celui d'Android
/// (`org.xmlpull.v1.XmlPullParser`), fourni par une autre couche de cales.
object Xml {
    fun newPullParser(): org.xmlpull.v1.XmlPullParser = TODO("K/N android stub")
}
