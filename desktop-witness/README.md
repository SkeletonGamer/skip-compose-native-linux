# desktop-witness: Skip-transpiled SwiftUI on Compose Multiplatform desktop

> English canonical. French copy: [`README.fr.md`](./README.fr.md).

End-to-end proof for the PoC: the **verbatim Skip-transpiled** `ContentView.kt` (a SwiftUI counter:
`VStack { Text; Button }` + `@State`) rendered and **interactive** on **Compose Multiplatform
desktop** (JVM, Skia), on Linux/macOS/Windows.

## What is real vs adapted

- `src/main/kotlin/witness/module/ContentView.kt`, **the real Skip output**, copied verbatim from
  `skip export` (`docs/transpiled-ContentView.kt`). Not edited.
- `src/main/kotlin/skip/ui/SkipUiMini.kt`, a **minimal desktop adapter** (~200 lines) reproducing
  only the slice of the SkipUI API the witness uses (`View`, `ComposeBuilder`, `ComposeContext`,
  `ComposeResult`, `Renderable`, `State`, `LocalizedStringKey`, `VStack`, `Text`, `Button`,
  `.padding()`), implemented on JetBrains Compose. **This is not the real SkipUI** (a 3561-line
  Android-Compose interface); it measures how small that slice is for a floor-level app.
- `skip/{lib,foundation,model}/Stub.kt`: empty packages so the transpiler's star-imports resolve.
- `witness/Main.kt`: windowed Compose Desktop app. `witness/RenderPng.kt`: offscreen render for
  headless verification.

## Run

```bash
gradle run        # opens the window
gradle renderPng  # writes ../docs/desktop-witness*.png (no display needed)
```

Result: `docs/desktop-witness.png` (Count: 0) and `docs/desktop-witness-after-click.png` (Count: 1
after a simulated click): the `@State` counter updates and Compose recomposes.

### Run on Linux (Docker)

```bash
docker/render-linux.sh   # builds the Linux image, renders headless, writes ../docs/linux-witness*.png
```

Headless Skia software render inside a Linux container. Note: skiko needs `libGL.so.1` even in
software mode (the image installs `libgl1`); see `docker/Dockerfile` and `../FINDINGS.md` step 6.

## Meaning for the PoC

The Skip-transpiled UI code itself **is portable** to CMP desktop. The cost is the **SkipUI runtime**:
either port the real (large, Android-coupled) SkipUI, or (as here) provide the small slice a given
app needs. See `../FINDINGS.md` for the full analysis.
