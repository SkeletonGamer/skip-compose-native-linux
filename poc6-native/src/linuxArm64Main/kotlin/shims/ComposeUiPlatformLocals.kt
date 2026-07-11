// POC 6 Jalon 4 : les CompositionLocals Android de compose.ui (LocalContext/Configuration/View) que
// SkipUI lit. Absents de la pile compose K/N (Android-only). Cales compile-only.
package androidx.compose.ui.platform

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

// Defauts benins (runtime) : le rendu lit ces locals sans qu'un provider Android soit installe.
val LocalContext: ProvidableCompositionLocal<android.content.Context> =
    staticCompositionLocalOf { android.content.DesktopContext }
val LocalConfiguration: ProvidableCompositionLocal<android.content.res.Configuration> =
    staticCompositionLocalOf { android.content.res.Configuration() }
val LocalView: ProvidableCompositionLocal<android.view.View> =
    staticCompositionLocalOf { android.view.View() }
