// POC 2 witness app: 100% Kotlin/Compose, no Skip, no SwiftUI.
// A persistent counter (java.util.prefs) + navigation (CMP navigation-compose) to a detail screen.
package app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.prefs.Preferences

fun main() = application {
    // Fixed size/position so the Xvfb (Linux real-screen) test harness can drive clicks deterministically.
    val state = rememberWindowState(
        size = DpSize(520.dp, 460.dp),
        position = WindowPosition(0.dp, 0.dp),
    )
    Window(onCloseRequest = ::exitApplication, state = state, title = "Compose-first (POC 2)") {
        App()
    }
}

/** Desktop-native persistence: survives process restarts. */
object CounterStore {
    private val prefs = Preferences.userRoot().node("dev/skeletongamer/composefirst")
    var count: Int
        get() = prefs.getInt("count", 0)
        set(value) = prefs.putInt("count", value)
}

@Composable
fun App() {
    val navController = rememberNavController()
    MaterialTheme {
        Surface {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(onOpenDetail = { navController.navigate("detail") })
                }
                composable("detail") {
                    DetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onOpenDetail: () -> Unit) {
    var count by remember { mutableStateOf(CounterStore.count) }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Count: $count")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { count -= 1; CounterStore.count = count }) { Text("-") }
            Button(onClick = { count += 1; CounterStore.count = count }) { Text("+") }
        }
        if (count > 0) {
            Text("Positive")
        }
        Button(onClick = onOpenDetail) { Text("Details") }
    }
}

@Composable
fun DetailScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = onBack) { Text("< Back") }
        Text("Detail for ${CounterStore.count}")
    }
}
