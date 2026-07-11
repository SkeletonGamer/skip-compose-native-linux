// POC 6 Jalon 4 : androidx.navigation3.runtime, absent des artefacts K/N (navigation3 est Android/JVM).
// Cales compile-only pour la navigation de SkipUI (Navigation.kt, TabView.kt, View.kt).
// Corps volontairement non implementes : le but est de COMPILER, pas de naviguer.
package androidx.navigation3.runtime

import androidx.compose.runtime.Composable

/// Marqueur des cles de navigation. Les cles internes de SkipUI l'implementent.
interface NavKey

/// Pile de navigation. Se comporte comme une liste mutable (size, removeLastOrNull, indexation).
interface NavBackStack<T : Any> : MutableList<T>

/// Entree decoree d'une pile de navigation, portant sa cle et son contenu Composable.
class NavEntry<T : Any>(key: T, content: @Composable (T) -> Unit)

/// Decorateur d'entree (etat sauvegarde, etc.). Opaque cote SkipUI.
class NavEntryDecorator<T : Any>

/// Constructeur d'entrees facon DSL. `entry` est un MEMBRE (comme le vrai navigation3), pour etre
/// resolu via le recepteur implicite dans `entryProvider { entry<Cle> { } }` sans import explicite.
class EntryProviderBuilder {
    fun <K : Any> entry(content: @Composable (K) -> Unit) {
        TODO("K/N navigation3 stub")
    }
}

/// Construit la fonction de resolution d'entrees a partir du DSL. Retour concret `(NavKey) -> NavEntry<NavKey>`
/// (toutes les cles de la pile sont des NavKey), pour eviter une inference de `T` impossible sur le `val`.
fun entryProvider(builder: EntryProviderBuilder.() -> Unit): (NavKey) -> NavEntry<NavKey> {
    TODO("K/N navigation3 stub")
}

/// Cree/retient une pile de navigation initialisee avec les cles fournies.
@Composable
fun rememberNavBackStack(vararg elements: NavKey): NavBackStack<NavKey> {
    TODO("K/N navigation3 stub")
}

/// Retient un decorateur d'entree adosse a un SaveableStateHolder.
@Composable
fun <T : Any> rememberSaveableStateHolderNavEntryDecorator(): NavEntryDecorator<T> {
    TODO("K/N navigation3 stub")
}

/// Retient la liste des entrees decorees pour une pile donnee.
@Composable
fun <T : Any> rememberDecoratedNavEntries(
    backStack: NavBackStack<T>,
    entryDecorators: List<NavEntryDecorator<T>>,
    entryProvider: (T) -> NavEntry<T>,
): List<NavEntry<T>> {
    TODO("K/N navigation3 stub")
}
