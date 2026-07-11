// POC 6 render (JVM): the shared export imports skip.lib.synchronized (defined natively for the
// Kotlin/Native build, where kotlin's builtin synchronized is unavailable). On the JVM we provide the
// same symbol delegating to the stdlib builtin. The EXACTLY_ONCE contract lets callers assign captured
// locals inside the block (Skip relies on it), matching the native shim, so the one shared export
// resolves and type-checks here too.
package skip.lib

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> synchronized(lock: Any?, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return kotlin.synchronized(lock ?: Unit, block)
}
