// POC 6 Jalon 4 : `.javaClass` (kotlin.jvm) est JVM-only. SkipUI l'utilise dans HitTesting. Extension
// dans le package skip.ui pour etre en portee sans editer le fichier transpile. Reflexion = stub en K/N.
package skip.ui

val <T : Any> T.javaClass: java.lang.Class<T> get() = TODO("K/N javaClass stub")
