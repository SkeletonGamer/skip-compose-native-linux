package android.util

// Cale compile-only pour Kotlin/Native Linux : complement de `android.util`
// (la cale `Log`/`Xml` est dans AndroidUtil.kt et n'est pas redefinie ici).

/// Exception runtime specifique au framework Android. Referencee par le code
/// transpile de SkipUI. On la mappe simplement sur `RuntimeException`.
class AndroidRuntimeException(message: String) : RuntimeException(message)
