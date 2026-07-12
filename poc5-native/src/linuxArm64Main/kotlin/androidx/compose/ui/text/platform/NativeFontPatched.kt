/*
 * Copyright 2023 The Android Open Source Project and JetBrains s.r.o.
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
// Changed from the original: patched copy of the Compose ui-text NativeFont.native.kt; only skFontStyle
// differs (to match the published skiko FontStyle signature), as the comment below explains.

// POC 5 version-shim: patched copy of ui-text's NativeFont.native.kt. jb-main HEAD targets an
// unreleased skiko whose FontStyle takes (FontWeight, FontWidth, FontSlant); public skiko 0.150.x takes
// (Int, Int, FontSlant). Only `skFontStyle` differs from the original (which is excluded in Gradle).
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.ExperimentalTextApi
import kotlin.native.Platform as NativePlatform
import org.jetbrains.skia.FontStyle as SkFontStyle
import org.jetbrains.skia.Typeface as SkTypeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import kotlin.experimental.ExperimentalNativeApi
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontWidth

@OptIn(ExperimentalTextApi::class)
internal actual fun loadTypeface(font: Font): SkTypeface {
    if (font !is PlatformFont) {
        throw IllegalArgumentException("Unsupported font type: $font")
    }
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    return when (font) {
        is LoadedFont -> FontMgr.default.makeFromData(Data.makeFromBytes(font.getData()))
            ?: error("loadTypeface makeFromData failed")
        is SystemFont -> FontMgr.default.legacyMakeTypeface(font.identity, font.skFontStyle)
            ?: error("loadTypeface legacyMakeTypeface failed")
        else -> throw IllegalArgumentException("Unsupported font type: $font")
    }.cloneWithVariationSettings(font.variationSettings)
}

// Patched for public skiko 0.150.x: FontStyle(weight: Int, width: Int, slant: FontSlant).
private val Font.skFontStyle: SkFontStyle
    get() = SkFontStyle(
        weight = weight.weight,
        width = FontWidth.NORMAL,
        slant = if (style == FontStyle.Italic) FontSlant.ITALIC else FontSlant.UPRIGHT
    )

@OptIn(ExperimentalNativeApi::class)
internal actual fun currentPlatform(): Platform = when (NativePlatform.osFamily) {
    OsFamily.MACOSX -> Platform.MacOS
    OsFamily.IOS -> Platform.IOS
    OsFamily.LINUX -> Platform.Linux
    OsFamily.WINDOWS -> Platform.Windows
    OsFamily.TVOS -> Platform.TvOS
    OsFamily.WATCHOS -> Platform.WatchOS
    else -> Platform.Unknown
}
