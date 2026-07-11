// POC 6 Jalon 4 : androidx.compose.material.pullrefresh (non compilé dans la pile K/N). Cale compile-only.
package androidx.compose.material.pullrefresh

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class PullRefreshState

@Composable
fun rememberPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThreshold: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(80f),
    refreshingOffset: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(56f),
): PullRefreshState = TODO("K/N pullrefresh stub")

fun Modifier.pullRefresh(state: PullRefreshState, enabled: Boolean = true): Modifier =
    TODO("K/N pullrefresh stub")

@Composable
fun PullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    scale: Boolean = false,
): Unit = TODO("K/N pullrefresh stub")

