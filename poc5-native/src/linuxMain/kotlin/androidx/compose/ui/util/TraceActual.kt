// Native actual for compose.ui.util tracing expects (Android systrace has no Linux-native equivalent;
// no-op is correct here). This is a tiny piece of what a real Linux K/N compose.ui build must provide.
package androidx.compose.ui.util

actual inline fun <T> trace(sectionName: String, block: () -> T): T = block()

actual fun traceValue(tag: String, value: Long) {}
