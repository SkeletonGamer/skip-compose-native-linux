// POC 6 Jalon 4: java.security.SecureRandom. Not cryptographically strong here (it is a K/N probe,
// not production), just enough surface for SkipLib's SystemRandomNumberGenerator to resolve.
package java.security

import kotlin.random.Random as KotlinRandom

class SecureRandom {
    fun nextLong(): Long = KotlinRandom.Default.nextLong()
    fun nextInt(): Int = KotlinRandom.Default.nextInt()
    fun nextDouble(): Double = KotlinRandom.Default.nextDouble()
    fun nextFloat(): Float = KotlinRandom.Default.nextFloat()
}
