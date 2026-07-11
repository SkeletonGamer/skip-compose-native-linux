// POC 6 Jalon 4: the java.lang.Character statics SkipLib's String.kt uses (code-point helpers).
// BMP-only here: Skip's callers pass ASCII/BMP text in the witness. Astral planes would need the
// surrogate-pair math (documented as a limitation, not implemented for a probe).
package java.lang

object Character {
    fun codePointAt(seq: CharSequence, index: Int): Int = seq[index].code
    fun charCount(codePoint: Int): Int = if (codePoint >= 0x10000) 2 else 1
}

// SkipLib's Dictionary.kt imports java.lang.UnsupportedOperationException; map it to Kotlin's.
typealias UnsupportedOperationException = kotlin.UnsupportedOperationException

