// POC 6 Jalon 3: SkipUI targets an older navigation3 whose rememberNavBackStack took just the elements.
// 1.2.0-alpha05 added a required SavedStateConfiguration first param. This overload bridges the old call
// shape (compiles; the DEFAULT config would throw at runtime, but a non-navigation witness never hits it).
package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.savedstate.serialization.SavedStateConfiguration

@Composable
fun rememberNavBackStack(vararg elements: NavKey): NavBackStack<NavKey> =
    rememberNavBackStack(SavedStateConfiguration.DEFAULT, *elements)
