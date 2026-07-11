// POC 6 Jalon 4: the two JVM-only concurrency primitives SkipLib's Concurrency.kt relies on, provided
// natively for K/N Linux so the transpiled Task engine compiles unchanged.
//
//  - `synchronized(lock) { ... }`: a JVM intrinsic, absent on K/N. Backed by an atomicfu reentrant lock.
//    Coarse (one global lock) but correct for the probe; Skip only guards small TaskState critical sections.
//    The EXACTLY_ONCE contract mirrors kotlin.synchronized so definite-assignment across the block holds.
//  - `ThreadLocal<T>` + `.asContextElement(value)`: on the JVM Skip stores the current TaskState in a
//    ThreadLocal and propagates it across coroutine dispatches via kotlinx-coroutines' JVM-only
//    `ThreadLocal.asContextElement`. Here `asContextElement` binds the value eagerly and returns a no-op
//    context element. LIMITATION: this does not restore per-continuation like the JVM path, so nested
//    concurrent Tasks would share the last-bound state. Fine for the witness (no concurrent Tasks);
//    a production port would use a coroutines ThreadContextElement.
package skip.lib

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.locks.ReentrantLock

@PublishedApi
internal val skipGlobalLock = ReentrantLock()

@OptIn(ExperimentalContracts::class)
inline fun <R> synchronized(lock: Any?, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    skipGlobalLock.lock()
    try {
        return block()
    } finally {
        skipGlobalLock.unlock()
    }
}

private object SkipTaskLocalKey : CoroutineContext.Key<SkipTaskLocalElement>
private class SkipTaskLocalElement : AbstractCoroutineContextElement(SkipTaskLocalKey)

class ThreadLocal<T> {
    private var storage: T? = null

    @Suppress("UNCHECKED_CAST")
    fun get(): T = storage as T
    // Parametre nullable pour coller au type-plateforme JVM (java.lang.ThreadLocal.set accepte null).
    fun set(value: T?) { storage = value }
    fun remove() { storage = null }

    fun asContextElement(value: T): CoroutineContext.Element {
        storage = value
        return SkipTaskLocalElement()
    }
}
