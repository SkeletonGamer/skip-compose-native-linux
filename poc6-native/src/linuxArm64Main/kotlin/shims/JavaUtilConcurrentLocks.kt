// POC 6 Jalon 4: java.util.concurrent.Semaphore + .locks surface for K/N, backed by an atomicfu lock.
// Used by SkipFoundation's NSLock / OSAllocatedUnfairLock. Coarse but functional for the probe.
package java.util.concurrent

class Semaphore(permits: Int) {
    private val lock = kotlinx.atomicfu.locks.ReentrantLock()
    fun acquire() { lock.lock() }
    fun acquireUninterruptibly() { lock.lock() }
    fun release() { lock.unlock() }
    fun tryAcquire(): Boolean = lock.tryLock()
    fun tryAcquire(timeout: Long, unit: TimeUnit): Boolean = lock.tryLock()
    fun availablePermits(): Int = 0
}
