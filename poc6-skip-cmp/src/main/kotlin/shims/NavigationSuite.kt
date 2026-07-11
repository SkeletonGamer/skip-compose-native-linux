// POC 6 Jalon 2/3: material3 adaptive navigation suite (aar-only). Loose stubs.
package androidx.compose.material3.adaptive.navigationsuite

import androidx.compose.runtime.Composable

class NavigationSuiteType private constructor(val name: String) {
    companion object {
        val NavigationBar = NavigationSuiteType("NavigationBar")
        val NavigationRail = NavigationSuiteType("NavigationRail")
    }
}

object NavigationSuiteScaffoldDefaults {
    fun calculateFromAdaptiveInfo(info: androidx.compose.material3.adaptive.WindowAdaptiveInfo): NavigationSuiteType =
        NavigationSuiteType.NavigationBar
}

class NavigationSuiteScaffoldState

@Composable
fun rememberNavigationSuiteScaffoldState(): NavigationSuiteScaffoldState = NavigationSuiteScaffoldState()

@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    navigationSuiteType: NavigationSuiteType = NavigationSuiteType.NavigationBar,
    state: NavigationSuiteScaffoldState = NavigationSuiteScaffoldState(),
    primaryActionContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {}
