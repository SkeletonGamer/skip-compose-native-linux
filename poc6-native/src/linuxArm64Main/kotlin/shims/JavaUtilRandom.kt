// POC 6 Jalon 4: java.util.Random backed by kotlin.random on K/N Linux.
package java.util

import kotlin.random.Random as KotlinRandom

open class Random {
    private val impl: KotlinRandom

    constructor() { impl = KotlinRandom.Default }
    constructor(seed: Long) { impl = KotlinRandom(seed) }

    open fun nextLong(): Long = impl.nextLong()
    open fun nextInt(): Int = impl.nextInt()
    open fun nextDouble(): Double = impl.nextDouble()
    open fun nextFloat(): Float = impl.nextFloat()
    open fun nextBoolean(): Boolean = impl.nextBoolean()
}
