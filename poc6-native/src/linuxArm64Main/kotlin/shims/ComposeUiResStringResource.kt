// POC 6 Jalon 4 : androidx.compose.ui.res.stringResource (variante Android par id Int). K/N utilise le
// systeme de ressources JetBrains ; cette surcharge Android-only manque. Cale compile-only.
package androidx.compose.ui.res

import androidx.compose.runtime.Composable

@Composable
fun stringResource(id: Int): String = TODO("K/N stringResource stub")

@Composable
fun stringResource(id: Int, vararg formatArgs: Any?): String = TODO("K/N stringResource stub")
