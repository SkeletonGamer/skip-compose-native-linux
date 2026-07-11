// POC 5: minimal Linux actual for ui-text's font-resolution interceptor (no-op passthrough).
package androidx.compose.ui.text.font

internal actual fun createPlatformResolveInterceptor(): PlatformResolveInterceptor =
    object : PlatformResolveInterceptor {}
