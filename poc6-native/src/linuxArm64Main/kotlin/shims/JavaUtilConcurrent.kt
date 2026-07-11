// POC 6 : cale compile-only de java.util.concurrent pour K/N Linux. SkipFoundation s'en sert dans
// OperationQueue (Executors.newSingleThreadExecutor().submit(block)) et via TimeUnit.MILLISECONDS
// (URLSessionTask, NSLock). On reproduit la surface reellement referencee, plus les fabriques et
// ExecutorService associees.
// Note : java.util.concurrent.Semaphore et java.util.concurrent.locks.* (references par NSLock /
// OSAllocatedUnfairLock) relevent d'autres packages/fichiers et ne sont pas traites ici.
package java.util.concurrent

import java.lang.Runnable

// Unites de temps. TimeUnit.MILLISECONDS est la seule reellement referencee, l'enum complet suit la JVM.
enum class TimeUnit {
    NANOSECONDS,
    MICROSECONDS,
    MILLISECONDS,
    SECONDS,
    MINUTES,
    HOURS,
    DAYS
}

// Resultat d'une soumission de tache (renvoye par ExecutorService.submit).
interface Future<out V> {
    fun get(): V
    fun get(timeout: Long, unit: TimeUnit): V
    fun cancel(mayInterruptIfRunning: Boolean): Boolean
    fun isCancelled(): Boolean
    fun isDone(): Boolean
}

interface Executor {
    // execute(commande) : lance la tache (Runnable est aliase sur () -> Unit dans JavaLang.kt).
    fun execute(command: Runnable)
}

interface ExecutorService : Executor {
    // submit(tache) : soumet une tache et rend un Future ; utilise par OperationQueue.
    fun submit(task: Runnable): Future<*>

    // shutdown() : arret ordonne de l'executeur.
    fun shutdown()
}

object Executors {
    fun newSingleThreadExecutor(): ExecutorService = TODO("K/N stub")
    fun newFixedThreadPool(nThreads: Int): ExecutorService = TODO("K/N stub")
    fun newCachedThreadPool(): ExecutorService = TODO("K/N stub")
}

// Levee quand une operation bloquante depasse son delai.
class TimeoutException(message: String? = null) : Exception(message)

// Table concurrente. Deleguee a une LinkedHashMap : pas de garantie thread-safe reelle (sonde K/N),
// juste la surface Map attendue par le code transpile.
class ConcurrentHashMap<K, V>(
    private val backing: MutableMap<K, V> = LinkedHashMap()
) : MutableMap<K, V> by backing
