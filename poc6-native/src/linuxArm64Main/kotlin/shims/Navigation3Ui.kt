// POC 6 Jalon 4 : androidx.navigation3.ui, absent des artefacts K/N.
// Cales compile-only pour NavDisplay et ses specs de transition (Navigation.kt, TabView.kt, View.kt).
package androidx.navigation3.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator

/// Affichage de navigation adosse a une pile (`backStack`) et a un `entryProvider`.
@Composable
fun <T : Any> NavDisplay(
    backStack: NavBackStack<T>,
    modifier: Modifier,
    onBack: () -> Unit,
    transitionSpec: () -> ContentTransform,
    popTransitionSpec: () -> ContentTransform,
    predictivePopTransitionSpec: (Int) -> ContentTransform,
    entryDecorators: List<NavEntryDecorator<T>>,
    entryProvider: (T) -> NavEntry<T>,
) {
    TODO("K/N navigation3 stub")
}

/// Affichage de navigation adosse a une liste d'entrees deja decorees.
@Composable
fun <T : Any> NavDisplay(
    entries: List<NavEntry<T>>,
    modifier: Modifier,
    onBack: () -> Unit,
    transitionSpec: () -> ContentTransform,
    popTransitionSpec: () -> ContentTransform,
    predictivePopTransitionSpec: (Int) -> ContentTransform,
) {
    TODO("K/N navigation3 stub")
}

/// Spec de transition par defaut (avancee). Le `()` supplementaire cote appelant l'evalue.
fun <T : Any> defaultTransitionSpec(): () -> ContentTransform = TODO("K/N navigation3 stub")

/// Spec de transition par defaut au retour (pop).
fun <T : Any> defaultPopTransitionSpec(): () -> ContentTransform = TODO("K/N navigation3 stub")

/// Spec de transition par defaut du retour predictif, parametree par le bord (`edge`) du geste.
fun <T : Any> defaultPredictivePopTransitionSpec(): (Int) -> ContentTransform = TODO("K/N navigation3 stub")
