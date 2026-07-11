// POC 5 Jalon 3: actual Linux de getCurrentThread pour animation-core (apple utilise NSThread).
// Seul besoin : une identité stable par thread pour le check "même thread" de rememberTransition.
// @ThreadLocal donne à chaque thread sa propre instance, donc l'égalité référentielle tient.
package androidx.compose.animation.core

@kotlin.native.concurrent.ThreadLocal
private val currentThreadToken = Any()

internal actual fun getCurrentThread(): Any = currentThreadToken
