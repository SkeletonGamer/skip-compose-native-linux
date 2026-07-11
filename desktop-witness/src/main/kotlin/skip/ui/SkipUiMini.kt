// Minimal desktop adapter for the slice of the SkipUI API that a trivial witness app uses.
// This is NOT the real SkipUI (a 3561-line Android-Compose interface). It reproduces just the
// symbols the Skip-transpiled ContentView.kt references, implemented on JetBrains Compose desktop,
// to measure how small that slice is for a floor-level app.
package skip.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// --- Rendering engine contract expected by the transpiler output ---

/** Marker for the list returned by [View.Evaluate]; unused by the render path here. */
interface Renderable

/** Per-compose context carrying the saver used by the transpiler's rememberSaveable calls. */
class ComposeContext {
    // The transpiler casts this to Saver<State<T>, Any>; a no-op saver is enough on desktop.
    val stateSaver: Saver<Any, Any> = Saver(save = { 0 }, restore = { null })
}

/** Singleton result the transpiler returns from each ComposeBuilder body. */
class ComposeResult {
    companion object {
        val ok = ComposeResult()
    }
}

/** Base SwiftUI View. Compose() runs Evaluate (state setup) then renders body(). */
interface View {
    fun body(): View = EmptyView

    @Composable
    fun Evaluate(context: ComposeContext, options: Int): List<Renderable> = emptyList()

    @Composable
    fun Compose(context: ComposeContext): ComposeResult {
        Evaluate(context, 0)
        return body().Compose(context)
    }
}

internal object EmptyView : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult = ComposeResult.ok
}

/** Wraps a @Composable content lambda; body() of every transpiled view returns one of these. */
class ComposeBuilder(private val content: @Composable (ComposeContext) -> ComposeResult) : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult = content(context)
}

/** @State<T> backed by a Compose MutableState so writes recompose. */
class State<T>(value: T) {
    private val backing = mutableStateOf(value)
    var wrappedValue: T
        get() = backing.value
        set(v) {
            backing.value = v
        }
}

// @AppStorage<T>: SwiftUI's persistent property wrapper. In real Skip this is a 283-line
// SkipUI class backed by SkipFoundation's UserDefaults (611 lines, Android SharedPreferences).
// Here it is re-backed on the desktop-native store java.util.prefs.Preferences: crossing the
// "cliff" means porting a persistence subsystem, not adding one layout function.
class AppStorage<T>(wrappedValue: T, private val key: String) {
    private val prefs = java.util.prefs.Preferences.userRoot().node("dev/skeletongamer/witness")
    private val backing = mutableStateOf(readPersisted(wrappedValue))

    var wrappedValue: T
        get() = backing.value
        set(v) {
            backing.value = v
            writePersisted(v)
        }

    @Suppress("UNCHECKED_CAST")
    private fun readPersisted(default: T): T = when (default) {
        is Int -> prefs.getInt(key, default) as T
        is Long -> prefs.getLong(key, default) as T
        is Boolean -> prefs.getBoolean(key, default) as T
        is Double -> prefs.getDouble(key, default) as T
        is String -> prefs.get(key, default) as T
        else -> default
    }

    private fun writePersisted(v: T) {
        when (v) {
            is Int -> prefs.putInt(key, v)
            is Long -> prefs.putLong(key, v)
            is Boolean -> prefs.putBoolean(key, v)
            is Double -> prefs.putDouble(key, v)
            is String -> prefs.put(key, v)
        }
    }
}

// --- Localized string keys (String and interpolated forms) ---

class LocalizedStringKey {
    val text: String

    constructor(stringLiteral: String) {
        this.text = stringLiteral
    }

    constructor(stringInterpolation: StringInterpolation) {
        this.text = stringInterpolation.builder.toString()
    }

    class StringInterpolation(
        @Suppress("UNUSED_PARAMETER") literalCapacity: Int,
        @Suppress("UNUSED_PARAMETER") interpolationCount: Int,
    ) {
        val builder = StringBuilder()
        fun appendLiteral(literal: String) {
            builder.append(literal)
        }

        fun appendInterpolation(value: Any?) {
            builder.append(value)
        }
    }
}

// --- Views used by the witness: VStack, Text, Button + modifiers ---

fun VStack(spacing: Double = 0.0, content: () -> View): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content().Compose(context)
        }
        return ComposeResult.ok
    }
}

fun HStack(spacing: Double = 0.0, content: () -> View): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content().Compose(context)
        }
        return ComposeResult.ok
    }
}

// SwiftUI NavigationStack / NavigationLink / .navigationTitle.
// Real SkipUI Navigation is 2326 lines built on androidx.navigation3: an Android-only library with
// NO Compose Multiplatform artifact (unlike List's deps, which all exist in CMP). The adapter route
// sidesteps navigation3 entirely: here is a hand-rolled back stack on plain Compose. It gives real
// push/pop, but none of navigation3's transitions, deep links, predictive-back or state restoration.
private class Navigator {
    val stack = mutableStateListOf<View>()
}

private val LocalNavigator = compositionLocalOf<Navigator?> { null }

fun NavigationStack(content: () -> View): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        val nav = remember { Navigator() }
        CompositionLocalProvider(LocalNavigator provides nav) {
            Column {
                if (nav.stack.isNotEmpty()) {
                    M3Button(onClick = { nav.stack.removeAt(nav.stack.lastIndex) }) { M3Text("< Back") }
                    nav.stack.last().Compose(context)
                } else {
                    content().Compose(context)
                }
            }
        }
        return ComposeResult.ok
    }
}

fun NavigationLink(key: LocalizedStringKey, destination: () -> View): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        val nav = LocalNavigator.current
        M3Button(onClick = { nav?.stack?.add(destination()) }) { M3Text(key.text) }
        return ComposeResult.ok
    }
}

fun View.navigationTitle(key: LocalizedStringKey): View {
    val inner = this
    return object : View {
        @Composable
        override fun Compose(context: ComposeContext): ComposeResult {
            Column {
                M3Text(key.text)
                inner.Compose(context)
            }
            return ComposeResult.ok
        }
    }
}

// SwiftUI List → a LazyColumn. Real SkipUI List is 1334 lines wiring a LazyItemFactory protocol,
// EnvironmentValues, ForEach diffing, rows, separators, selection and swipe actions. This toy keeps
// only "render the content in a bounded lazy scroll": enough for static rows, none of the rest.
fun List(content: () -> View): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
            item { content().Compose(context) }
        }
        return ComposeResult.ok
    }
}

fun Text(key: LocalizedStringKey): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        M3Text(key.text)
        return ComposeResult.ok
    }
}

fun Button(key: LocalizedStringKey, action: () -> Unit): View = object : View {
    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        M3Button(onClick = action) { M3Text(key.text) }
        return ComposeResult.ok
    }
}

/** SwiftUI .padding() modifier: wraps the receiver with default padding. */
fun View.padding(): View {
    val inner = this
    return object : View {
        @Composable
        override fun Compose(context: ComposeContext): ComposeResult {
            androidx.compose.foundation.layout.Box(Modifier.padding(16.dp)) {
                inner.Compose(context)
            }
            return ComposeResult.ok
        }
    }
}
