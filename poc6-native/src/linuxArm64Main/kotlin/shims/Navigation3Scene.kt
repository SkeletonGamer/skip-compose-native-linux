// POC 6 Jalon 4 : androidx.navigation3.scene, absent des artefacts K/N.
// L'import `Scene` est present dans SkipUI (Navigation.kt, View.kt) mais jamais utilise ;
// cette cale sert uniquement a resoudre l'import.
package androidx.navigation3.scene

/// Regroupement d'entrees de navigation. Opaque et inutilise cote SkipUI (import seul).
interface Scene<T : Any>
