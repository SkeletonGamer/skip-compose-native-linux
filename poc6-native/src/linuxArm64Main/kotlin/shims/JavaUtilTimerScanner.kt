// POC 6 Jalon 4 : java.util.Timer / TimerTask / Scanner (cale compile-only K/N).
package java.util

abstract class TimerTask {
    abstract fun run()
    open fun cancel(): Boolean = TODO("K/N TimerTask stub")
}

class Timer {
    constructor()
    constructor(isDaemon: Boolean)
    constructor(name: String)
    fun schedule(task: TimerTask, delay: Long): Unit = TODO("K/N Timer stub")
    fun schedule(task: TimerTask, delay: Long, period: Long): Unit = TODO("K/N Timer stub")
    fun scheduleAtFixedRate(task: TimerTask, delay: Long, period: Long): Unit = TODO("K/N Timer stub")
    fun cancel(): Unit = TODO("K/N Timer stub")
    fun purge(): Int = TODO("K/N Timer stub")
}

class Scanner(source: String) {
    fun hasNext(): Boolean = TODO("K/N Scanner stub")
    fun next(): String = TODO("K/N Scanner stub")
    fun hasNextInt(): Boolean = TODO("K/N Scanner stub")
    fun nextInt(): Int = TODO("K/N Scanner stub")
    fun nextLine(): String = TODO("K/N Scanner stub")
    fun useDelimiter(pattern: String): Scanner = TODO("K/N Scanner stub")
    fun close(): Unit = TODO("K/N Scanner stub")
}
