import SwiftUI

/// Minimal witness screen, used by POC 6 (the transpiled SwiftUI rendered by the real SkipUI on Compose
/// Multiplatform, and on Kotlin/Native Linux with no JVM). Intentionally the floor of SwiftUI: a counter,
/// a label and a button, no navigation or persistence, so the render exercises UI primitives only.
///
/// POC 1's richer screen (`@AppStorage`, `NavigationStack`, `List`) stays in `ContentView.swift`; both are
/// transpiled by `skip export`, and each POC references the one it needs.
struct MinimalContentView: View {
    @State var count = 0

    var body: some View {
        VStack(spacing: 16) {
            Text("Count: \(count)")
            Button("Increment") {
                count += 1
            }
        }
        .padding()
    }
}
