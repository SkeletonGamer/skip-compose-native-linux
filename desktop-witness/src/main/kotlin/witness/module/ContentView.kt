package witness.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

import skip.ui.*
import skip.foundation.*
import skip.model.*

/// Witness screen: a persistent counter, a label and buttons.
/// The counter uses @AppStorage (persistence) to probe the "cliff": in Skip this maps to
/// SkipUI's AppStorage backed by SkipFoundation's UserDefaults (Android SharedPreferences).
internal class ContentView: View {
    internal var count: Int
        get() = _count.wrappedValue
        set(newValue) {
            _count.wrappedValue = newValue
        }
    internal var _count: skip.ui.AppStorage<Int>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("Count: ")
                                str.appendInterpolation(count)
                                LocalizedStringKey(stringInterpolation = str)
                            }()).Compose(composectx)
                            HStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Button(LocalizedStringKey(stringLiteral = "-")) { -> count -= 1 }.Compose(composectx)
                                    Button(LocalizedStringKey(stringLiteral = "+")) { -> count += 1 }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            if (count > 0) {
                                Text(LocalizedStringKey(stringLiteral = "Positive")).Compose(composectx)
                            }
                            NavigationLink(LocalizedStringKey(stringLiteral = "Details")) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("Detail for ")
                                        str.appendInterpolation(count)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }()).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            List { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Alpha")).Compose(composectx)
                                    Text(LocalizedStringKey(stringLiteral = "Bravo")).Compose(composectx)
                                    Text(LocalizedStringKey(stringLiteral = "Charlie")).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .navigationTitle(LocalizedStringKey(stringLiteral = "Witness")).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedcount by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.AppStorage<Int>, Any>) { mutableStateOf(_count) }
        _count = rememberedcount

        return super.Evaluate(context, options)
    }

    constructor(count: Int = 0) {
        this._count = skip.ui.AppStorage(wrappedValue = count, "count")
    }
}
