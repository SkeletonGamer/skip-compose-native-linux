package coil3

// Cale compile-only pour Kotlin/Native Linux : surface racine de coil3 reellement
// referencee par SkipUI (AsyncImage.kt, Image.kt). coil3 ne publie pas de variante
// linuxArm64 ; ces declarations existent uniquement pour COMPILER. Aucun corps reel.

// Contexte plateforme de coil (equivalent desktop d'android.Context). Retourne par
// coil3.request.Options.context.
class PlatformContext

// Chargeur d'images de coil. Utilise seulement comme type de parametre dans les
// fabriques Fetcher.Factory / Decoder.Factory.
class ImageLoader

// Image decodee par coil. Referencee indirectement comme type de retour de asImage().
class Image

// Conversion vers une coil3.Image (dans coil : extension sur Drawable/Bitmap). Ici jamais
// appelee ; declaree pour resoudre l'import `coil3.asImage` d'AsyncImage.kt.
fun asImage(): Image = TODO("K/N coil3 stub")
