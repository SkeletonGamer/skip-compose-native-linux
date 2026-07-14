// POC 5 Jalon 3: actual Linux du locale du DatePicker. macOS aliase NSLocale ; ici un porteur
// opaque (le code partagé ne lit aucun membre). defaultLocale renvoie en-US (le mediator réel
// lira LANG/LC_TIME via l'environnement).
package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

actual class CalendarLocale internal constructor(internal val languageTag: String)

@Composable
@ReadOnlyComposable
internal actual fun defaultLocale(): CalendarLocale = CalendarLocale("en-US")
