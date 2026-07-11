// POC 6 Jalon 4: kotlin.text.String.format is JVM-only. SkipLib's String.kt converts a Swift format to
// a Java-style one and calls `.format(...)`. Provide a printf-subset formatter for K/N in skip.lib scope.
// Covers the specifiers Skip emits (d, i, u, x, X, o, f, e, g, s, @, c, %) with width/precision/flags.
package skip.lib

fun String.format(vararg args: Any?): String {
    val out = StringBuilder()
    var i = 0
    var argIndex = 0
    while (i < length) {
        val c = this[i]
        if (c != '%') { out.append(c); i++; continue }
        // Parse: %[argpos$][flags][width][.precision]conv
        val spec = StringBuilder("%")
        i++
        if (i < length && this[i] == '%') { out.append('%'); i++; continue }
        // optional positional "n$"
        var explicitIndex = -1
        val posStart = i
        while (i < length && this[i].isDigit()) i++
        if (i < length && this[i] == '$') {
            explicitIndex = substring(posStart, i).toInt() - 1
            i++
        } else {
            i = posStart
        }
        // flags
        val flags = StringBuilder()
        while (i < length && this[i] in "-+ 0#,") { flags.append(this[i]); i++ }
        // width
        var width = -1
        val wStart = i
        while (i < length && this[i].isDigit()) i++
        if (i > wStart) width = substring(wStart, i).toInt()
        // precision
        var precision = -1
        if (i < length && this[i] == '.') {
            i++
            val pStart = i
            while (i < length && this[i].isDigit()) i++
            precision = if (i > pStart) substring(pStart, i).toInt() else 0
        }
        if (i >= length) { out.append(spec); break }
        val conv = this[i]; i++
        val arg = if (explicitIndex >= 0) args.getOrNull(explicitIndex) else args.getOrNull(argIndex++)
        out.append(formatOne(conv, flags.toString(), width, precision, arg))
    }
    return out.toString()
}

private fun formatOne(conv: Char, flags: String, width: Int, precision: Int, arg: Any?): String {
    val body: String = when (conv) {
        'd', 'i', 'u' -> ((arg as? Number)?.toLong() ?: 0L).toString()
        'x' -> ((arg as? Number)?.toLong() ?: 0L).toString(16)
        'X' -> ((arg as? Number)?.toLong() ?: 0L).toString(16).uppercase()
        'o' -> ((arg as? Number)?.toLong() ?: 0L).toString(8)
        'f', 'F' -> {
            val d = (arg as? Number)?.toDouble() ?: 0.0
            val p = if (precision >= 0) precision else 6
            formatFixed(d, p)
        }
        'e', 'E', 'g', 'G' -> ((arg as? Number)?.toDouble() ?: 0.0).toString()
        'c' -> when (arg) { is Char -> arg.toString(); is Number -> arg.toInt().toChar().toString(); else -> "" }
        's', '@' -> arg?.toString() ?: "null"
        else -> arg?.toString() ?: ""
    }
    var result = body
    if (precision >= 0 && (conv == 's' || conv == '@') && result.length > precision) {
        result = result.substring(0, precision)
    }
    if (width > result.length) {
        val pad = if (flags.contains('0') && !flags.contains('-')) '0' else ' '
        val padding = pad.toString().repeat(width - result.length)
        result = if (flags.contains('-')) result + padding else padding + result
    }
    return result
}

private fun formatFixed(value: Double, precision: Int): String {
    if (precision == 0) return kotlin.math.round(value).toLong().toString()
    val factor = generateSequence(1.0) { it * 10 }.elementAt(precision)
    val rounded = kotlin.math.round(value * factor) / factor
    val s = rounded.toString()
    val dot = s.indexOf('.')
    if (dot < 0) return s + "." + "0".repeat(precision)
    val decimals = s.length - dot - 1
    return if (decimals >= precision) s.substring(0, dot + 1 + precision) else s + "0".repeat(precision - decimals)
}
