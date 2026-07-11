package coil3.compose

// Cale compile-only pour Kotlin/Native Linux : surface de coil3.compose referencee par
// AsyncImage.kt / Image.kt / View.kt. Les types Compose (Painter, Modifier, ContentScale,
// DrawScope, Size...) proviennent de la pile compose deja presente : on les refere
// qualifies, sans les recreer. Compile-only, aucun corps reel.

// Painter asynchrone de coil : etend le Painter de Compose et expose un StateFlow d'etat.
// Utilise comme Painter (Image(painter = ...)) et via `.state.collectAsState()`.
class AsyncImagePainter: androidx.compose.ui.graphics.painter.Painter() {
    val state: kotlinx.coroutines.flow.StateFlow<State>
        get() = TODO("K/N coil3 stub")

    override val intrinsicSize: androidx.compose.ui.geometry.Size
        get() = TODO("K/N coil3 stub")

    override fun androidx.compose.ui.graphics.drawscope.DrawScope.onDraw() {
        TODO("K/N coil3 stub")
    }

    // Etat courant du chargement. Compare via `== State.Empty` et teste via `is`.
    sealed interface State {
        // Coil ne sait pas encore si l'image est en cache : singleton compare par egalite.
        object Empty: State
        class Loading: State
        class Success: State
        // Porte le resultat d'erreur (`.result.throwable`).
        class Error(val result: coil3.request.ErrorResult): State
    }
}

// Scope des lambdas de SubcomposeAsyncImage. Seul `painter` est reference (this.painter).
interface SubcomposeAsyncImageScope {
    val painter: androidx.compose.ui.graphics.painter.Painter
}

// Variante subcompose : rend des contenus distincts selon l'etat (loading/success/error).
@androidx.compose.runtime.Composable
fun SubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    loading: (@androidx.compose.runtime.Composable SubcomposeAsyncImageScope.(AsyncImagePainter.State.Loading) -> Unit)? = null,
    success: (@androidx.compose.runtime.Composable SubcomposeAsyncImageScope.(AsyncImagePainter.State.Success) -> Unit)? = null,
    error: (@androidx.compose.runtime.Composable SubcomposeAsyncImageScope.(AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    TODO("K/N coil3 stub")
}

// Cree/memorise un AsyncImagePainter pour un modele donne.
@androidx.compose.runtime.Composable
fun rememberAsyncImagePainter(
    model: Any?,
    contentScale: androidx.compose.ui.layout.ContentScale,
): AsyncImagePainter = TODO("K/N coil3 stub")

// Resolveur de taille adosse aux contraintes de layout. Renvoie un Modifier
// (utilise via `context.modifier.then(...)`).
@androidx.compose.runtime.Composable
fun rememberConstraintsSizeResolver(): androidx.compose.ui.Modifier = TODO("K/N coil3 stub")
