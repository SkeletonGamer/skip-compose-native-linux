// POC 6 : cale compile-only de java.util.logging pour K/N Linux. SkipFoundation (Logger.kt / SkipLogger)
// se rabat sur java.util.logging.Logger.getLogger(nom).log(niveau, message) quand android.util.Log
// n'est pas disponible. On reproduit la surface Logger + Level reellement referencee.
package java.util.logging

// Niveaux de journalisation. Les valeurs entieres reprennent celles de la JVM (intValue).
class Level internal constructor(val name: String, private val value: Int) {
    // intValue() : poids numerique du niveau (comparaison de seuils).
    fun intValue(): Int = value

    companion object {
        val OFF = Level("OFF", Int.MAX_VALUE)
        val SEVERE = Level("SEVERE", 1000)
        val WARNING = Level("WARNING", 900)
        val INFO = Level("INFO", 800)
        val CONFIG = Level("CONFIG", 700)
        val FINE = Level("FINE", 500)
        val FINER = Level("FINER", 400)
        val FINEST = Level("FINEST", 300)
        val ALL = Level("ALL", Int.MIN_VALUE)
    }
}

class Logger private constructor(val name: String) {
    // log(niveau, message) : point d'entree utilise par SkipLogger.
    fun log(level: Level, message: String): Unit = TODO("K/N stub")

    fun info(message: String): Unit = TODO("K/N stub")
    fun warning(message: String): Unit = TODO("K/N stub")
    fun severe(message: String): Unit = TODO("K/N stub")
    fun fine(message: String): Unit = TODO("K/N stub")

    // isLoggable(niveau) : indique si un message a ce niveau serait emis.
    fun isLoggable(level: Level): Boolean = TODO("K/N stub")

    // setLevel(niveau) : fixe le seuil du logger.
    fun setLevel(level: Level): Unit = TODO("K/N stub")

    companion object {
        // getLogger(nom) : recupere (ou cree) le logger nomme.
        fun getLogger(name: String): Logger = TODO("K/N stub")
    }
}
