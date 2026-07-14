/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2026 SkeletonGamer, licensed under the Apache License, Version 2.0.
// Changed from the original: the AppKit mouse-wheel ScrollConfig (which read appkitEventOrNull) is
// replaced by the Linux scroll formula, taken from JetBrains' own LinuxGnomeConfig (foundation
// desktopMain, DesktopScrollable.desktop.kt), which they derived experimentally from Ubuntu Nautilus.
// AWT reports how many lines a wheel notch scrolls (MouseWheelEvent.scrollAmount); GLFW does not, so
// SCROLL_LINES_PER_NOTCH stands in for it (3 is the X11/GTK default).

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt

private const val SCROLL_LINES_PER_NOTCH = 3f

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig =
    LinuxScrollConfig

private object LinuxScrollConfig : ScrollConfig {
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        var delta = Offset.Zero
        for (change in event.changes) delta += change.scrollDelta
        return Offset(
            x = delta.x * sqrt(bounds.width.toFloat()),
            y = delta.y * sqrt(bounds.height.toFloat()),
        ) * -SCROLL_LINES_PER_NOTCH
    }
}
