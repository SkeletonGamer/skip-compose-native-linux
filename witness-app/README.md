# Witness app

> French version: [`README.fr.md`](./README.fr.md).

The SwiftUI witness app that `skip export` transpiles to Kotlin. Its transpiled output is the input to
POC 1 (Skip → Compose Multiplatform) and POC 6 (de-Android-ifying SkipUI for CMP Desktop and Kotlin/Native
Linux, no JVM). See the top-level [`README.md`](../README.md) for the POC story.

It holds two screens, both transpiled by `skip export`:

- `ContentView`: the richer screen (`@AppStorage`, `NavigationStack`, `NavigationLink`, `List`), used by
  POC 1's analysis of the Skip → Compose gap.
- `MinimalContentView`: a minimal counter and button, rendered by POC 6 (offscreen on CMP Desktop, and in
  a GLFW window on Kotlin/Native Linux with no JVM).

The transpiled Gradle project (`./export` at the repo root) is git-ignored; regenerate it with
`scripts/setup.sh`, which runs `skip export` here and applies `scripts/export.patch`.

This is a [Skip](https://skip.dev) dual-platform project: a stand-alone Swift Package Manager module and an
Xcode project that translates to a Kotlin/Gradle Android app via the skipstone plugin. To build or run the
witness itself (iOS simulator / Android emulator), open `Project.xcworkspace` in Xcode; see Skip's docs.
