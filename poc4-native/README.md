# poc4-native: a minimal `ui-glfw`, interactive Compose UI on Kotlin/Native Linux (no JVM)

> English canonical. French: [`README.fr.md`](./README.fr.md). Full journal:
> [`../FINDINGS-POC4.md`](../FINDINGS-POC4.md).

Proves the full Compose vertical runs natively on Linux with **no JVM**: a real `@Composable`
composition (compiler plugin + Recomposer) → a custom `Applier`/node tree → **skiko** draw (rects +
fontconfig text) → a **GLFW** window → **mouse input → recompose**. A hand-written *minimal* `ui-glfw`
(not JetBrains' `compose.ui`; no material3/foundation).

## Host / target

Kotlin/Native has **no linux-aarch64 host** compiler → **cross-compile on macOS arm64 → `linuxArm64`**,
then **run the ELF in a Linux arm64 container** (reuses the `poc3-native` image).

## Build & run

```bash
# reuse the poc3 image for the runtime container + native-lib extraction:
(cd ../poc3-native && docker build -f docker/Dockerfile -t poc3-native .)

# native libs the cross-link needs (GLFW/GL/EGL + fontconfig/freetype), extracted into native/glfw:
#   (poc4 reuses poc3's extraction; run ../poc3-native/docker/extract-native.sh, then add fontconfig/freetype)

gradle linkDebugExecutableLinuxArm64        # cross-compile on macOS
# run under Xvfb with: DISPLAY, XDG_RUNTIME_DIR, LIBGL_ALWAYS_SOFTWARE=1, GALLIUM_DRIVER=llvmpipe
# drive a click with xdotool; capture via GL readPixels (see FINDINGS-POC4 for the exact run).
```

Native link set: `-lglfw -lGL -lEGL -lfontconfig -lfreetype --allow-shlib-undefined`.

## Result

Interactive UI (`docs/poc4-ui-*-arm64.png`): a header + a button whose counter goes 0→1 on a real GLFW
click. Release binary **24 MB** self-contained (no JVM) vs POC 2's 137 MB JVM image.

## Next (Path A)

Real material3/foundation widgets need building compose's ui modules for Linux K/N + a real `ui-glfw`
backend from source (`compose.ui`'s `nativeMain` is platform-agnostic; only `ui-uikit` is iOS). Weeks.
