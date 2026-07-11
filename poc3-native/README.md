# poc3-native: Compose/Skia on Kotlin/Native Linux desktop (no JVM)

> English canonical. French copy: [`README.fr.md`](./README.fr.md).

POC 3 probe: how far can a **JVM-free** Compose UI go on Linux via **Kotlin/Native**? See
[`../FINDINGS-POC3.md`](../FINDINGS-POC3.md) for the full journal and verdict.

## What this builds

A single `linuxArm64` Kotlin/Native executable (no JVM at runtime) that:

- renders a **Skia rectangle offscreen** (raster), Jalon 1;
- exercises **`compose.runtime`** snapshot state, Jalon 2;
- renders a **Skia rectangle into a real GLFW window** via an OpenGL context, Jalon 3 (windowing brick).

All against official K/N Linux klibs: `org.jetbrains.skiko:skiko:0.150.0` and
`org.jetbrains.compose.runtime:runtime:1.9.0`. (`compose.ui`/`foundation`/`material3` are **not**
published for K/N Linux: that gap is the remaining multi-week work.)

## Host / target reality

The Kotlin/Native compiler has **no `linux-aarch64` host**. So you **compile on macOS arm64,
cross-compiling to `linuxArm64`**, then **run the ELF in a Linux arm64 container**.

## Build & run

```bash
# 1. Build the runtime container image (JDK + gradle + native deps).
docker build -f docker/Dockerfile -t poc3-native .

# 2. Extract the arm64 GLFW/GL/EGL headers + libs the cross-link needs (git-ignored).
docker/extract-native.sh

# 3. Cross-compile on the macOS host → linuxArm64 ELF.
gradle linkDebugExecutableLinuxArm64          # or linkReleaseExecutableLinuxArm64

# 4a. Run the offscreen + runtime paths (no display needed):
docker run --rm \
  -v "$PWD/build/bin/linuxArm64/debugExecutable:/b:ro" -v "$PWD/docker/out:/out" \
  poc3-native /b/poc3-native.kexe

# 4b. Run the windowed GL path under Xvfb (see docker/run notes / FINDINGS for the env vars:
#     DISPLAY, XDG_RUNTIME_DIR, LIBGL_ALWAYS_SOFTWARE=1, GALLIUM_DRIVER=llvmpipe).
```

Outputs land in `docker/out/`: `skia-knative.png` (offscreen), `poc3-window-gl.png` (read back from the
GL window surface). Saved copies: `../docs/poc3-skia-knative-arm64.png`,
`../docs/poc3-window-gl-arm64.png`.

## Weight (Jalon 4)

21.5 MB self-contained release binary (Skia included) vs the JVM Compose Desktop 137 MB image
(87 MB JRE + 49 MB jars). RAM is Skia-dominated, so only modestly lower. Details in FINDINGS.
