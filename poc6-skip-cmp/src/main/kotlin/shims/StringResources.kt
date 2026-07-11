// POC 6 Jalon 2: Android resource string lookup (android.R.string.*), absent on CMP desktop.
package androidx.compose.ui.res

import androidx.compose.runtime.Composable

@Composable fun stringResource(id: Int): String = ""
@Composable fun stringResource(id: Int, vararg formatArgs: Any?): String = ""
