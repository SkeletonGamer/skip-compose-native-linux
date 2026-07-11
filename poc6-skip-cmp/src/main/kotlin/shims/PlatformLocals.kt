// POC 6 render: the Android compose-locals SkipUI reads at runtime. android.jar types throw "Stub!"
// if instantiated, so we back them with Mockito mocks (RETURNS_MOCKS) so the SkipUI render can read
// Configuration / Context / View and proceed on desktop.
package androidx.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import org.mockito.Mockito

private fun <T> mock(cls: Class<T>): T = Mockito.mock(cls, Mockito.RETURNS_MOCKS)

val LocalContext = staticCompositionLocalOf<android.content.Context> {
    androidx.test.core.app.ApplicationProvider.getApplicationContext()
}
val LocalConfiguration = staticCompositionLocalOf { mock(android.content.res.Configuration::class.java) }
val LocalView = staticCompositionLocalOf { mock(android.view.View::class.java) }
