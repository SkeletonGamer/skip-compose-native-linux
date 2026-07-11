// POC 6 Jalon 4: java.util.concurrent.locks.ReentrantLock for K/N, backed by an atomicfu reentrant lock.
package java.util.concurrent.locks

interface Lock {
    fun lock()
    fun unlock()
    fun tryLock(): Boolean
    fun tryLock(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean
}

class ReentrantLock : Lock {
    private val impl = kotlinx.atomicfu.locks.ReentrantLock()
    override fun lock() { impl.lock() }
    override fun unlock() { impl.unlock() }
    override fun tryLock(): Boolean = impl.tryLock()
    override fun tryLock(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean = impl.tryLock()
}
