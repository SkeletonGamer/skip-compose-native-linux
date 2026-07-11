// POC 6 Jalon 4: minimal fakes of the JVM-only symbols SkipLib references, so K/N Linux can resolve
// them. These are the *tractable* value-type shims; the hard cores (Skip's concurrency engine,
// String.format, BigInteger/BigDecimal) are NOT here and are the real reimplementation work.
package androidx.annotation

// SkipLib tags companion objects with @androidx.annotation.Keep (proguard hint on Android). No-op on K/N.
@Retention(AnnotationRetention.BINARY)
annotation class Keep
