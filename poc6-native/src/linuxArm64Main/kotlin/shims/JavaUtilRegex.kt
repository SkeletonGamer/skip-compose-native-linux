// POC 6 Jalon 4: the one java.util.regex static SkipLib references (Regex.kt): quoteReplacement,
// which escapes \ and $ in a replacement string. kotlin.text.Regex is otherwise used directly.
package java.util.regex

object Matcher {
    fun quoteReplacement(s: String): String {
        if (s.indexOf('\\') < 0 && s.indexOf('$') < 0) return s
        val sb = StringBuilder(s.length * 2)
        for (c in s) {
            if (c == '\\' || c == '$') sb.append('\\')
            sb.append(c)
        }
        return sb.toString()
    }
}
