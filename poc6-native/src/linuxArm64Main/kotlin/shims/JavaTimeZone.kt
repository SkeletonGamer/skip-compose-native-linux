// POC 6 Jalon 4: java.time.zone surface (compile-only K/N stub). Used by SkipFoundation's TimeZone.
package java.time.zone

import java.time.Instant
import java.time.ZoneOffset

class ZoneRules {
    fun nextTransition(instant: Instant): ZoneOffsetTransition? = TODO("K/N java.time.zone stub")
    fun getOffset(instant: Instant): ZoneOffset = TODO("K/N java.time.zone stub")
    fun isDaylightSavings(instant: Instant): Boolean = TODO("K/N java.time.zone stub")
}

class ZoneOffsetTransition {
    fun getInstant(): Instant = TODO("K/N java.time.zone stub")
    fun getOffsetBefore(): ZoneOffset = TODO("K/N java.time.zone stub")
    fun getOffsetAfter(): ZoneOffset = TODO("K/N java.time.zone stub")
}
