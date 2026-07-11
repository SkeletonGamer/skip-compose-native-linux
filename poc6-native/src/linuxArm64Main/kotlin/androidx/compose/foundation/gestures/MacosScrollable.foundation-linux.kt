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

// Modifications: Copyright 2026 SkeletonGamer, licensed under Apache-2.0.
// Renamed from the original androidx MacosScrollable.macos.kt and changed for the
// Kotlin/Native Linux target: the AppKit mouse-wheel ScrollConfig (which read
// appkitEventOrNull) is replaced by a Linux stub returning Offset.Zero, to be fed
// by the ui-glfw mediator.

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

// POC 5 Jalon 3: macOS lit l'événement AppKit natif (deltas de molette). Ici le mediator ui-glfw
// fournira les deltas ; ce stub compile et renvoie zéro tant que le mediator n'est pas branché.
internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig =
    LinuxScrollConfig

private object LinuxScrollConfig : ScrollConfig {
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset =
        Offset.Zero // TODO mediator ui-glfw : convertir les deltas de molette GLFW.
}
