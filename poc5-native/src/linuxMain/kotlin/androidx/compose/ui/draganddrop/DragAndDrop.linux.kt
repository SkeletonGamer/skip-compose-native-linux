/*
 * Copyright 2024 The Android Open Source Project
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
// Changed from the original: the macOS/native source set declares these with private constructors and
// `positionInRoot` as TODO("Not yet implemented"), which means no DragAndDropEvent can exist at all and
// drag and drop simply does not work on any Kotlin/Native target. Here they carry real data, so a drop
// coming from the window system can be delivered to Compose.
//
// Scope: file drops INTO the app (what GLFW's drop callback gives us: a list of paths). Dragging OUT of the
// app, and rich MIME payloads, would need XDND (X11) or wl_data_device (Wayland) directly.

package androidx.compose.ui.draganddrop

import androidx.compose.ui.geometry.Offset

/**
 * An event sent by the platform during a drag and drop operation.
 *
 * Unlike the macOS actual (which cannot be constructed at all), this one carries what the window system
 * gave us: where the pointer is, and the files being dropped.
 */
actual class DragAndDropEvent internal constructor(
    internal val position: Offset,
    /** Absolute paths of the dropped files. Empty while merely hovering. */
    val files: List<String> = emptyList(),
)

/** Position of this event relative to the root Compose view. */
internal actual val DragAndDropEvent.positionInRoot: Offset
    get() = position

/**
 * Transferable data. Only file paths are carried for now; a richer implementation would model MIME types,
 * which is what X11 selections and wl_data_device actually exchange.
 */
actual class DragAndDropTransferData(
    val files: List<String> = emptyList(),
)
