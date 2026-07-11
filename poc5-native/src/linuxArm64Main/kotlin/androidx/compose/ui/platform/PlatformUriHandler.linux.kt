// POC 5: actual Linux d'ouverture d'URI. Stub no-op (le mediator réel appellera xdg-open).
package androidx.compose.ui.platform

private class LinuxUriHandler : UriHandler {
    override fun openUri(uri: String) {
        // TODO mediator : déléguer à xdg-open. Sans effet dans le POC.
    }
}

internal actual fun createPlatformUriHandler(): UriHandler = LinuxUriHandler()
