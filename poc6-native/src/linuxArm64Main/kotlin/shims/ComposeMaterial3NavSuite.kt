// POC 6 Jalon 4 : androidx.compose.material3.adaptive.navigationsuite (non compilé dans la pile K/N).
// Cale compile-only pour le TabView de SkipUI.
package androidx.compose.material3.adaptive.navigationsuite

import androidx.compose.runtime.Composable

class NavigationSuiteType {
    companion object {
        val NavigationBar = NavigationSuiteType()
        val NavigationRail = NavigationSuiteType()
        val NavigationDrawer = NavigationSuiteType()
        val None = NavigationSuiteType()
    }
}

class NavigationSuiteScaffoldState {
    val currentValue: Any? get() = TODO("K/N navigationsuite stub")
}

@Composable
fun rememberNavigationSuiteScaffoldState(): NavigationSuiteScaffoldState =
    TODO("K/N navigationsuite stub")

object NavigationSuiteScaffoldDefaults {
    fun calculateFromAdaptiveInfo(
        adaptiveInfo: androidx.compose.material3.adaptive.WindowAdaptiveInfo,
    ): NavigationSuiteType = TODO("K/N navigationsuite stub")
}

@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    navigationSuiteType: NavigationSuiteType = NavigationSuiteType.NavigationBar,
    state: NavigationSuiteScaffoldState = NavigationSuiteScaffoldState(),
    primaryActionContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
): Unit = TODO("K/N navigationsuite stub")
