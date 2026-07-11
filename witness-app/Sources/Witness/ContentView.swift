import SwiftUI

/// Witness screen: a persistent counter, a label and buttons.
/// The counter uses @AppStorage (persistence) to probe the "cliff": in Skip this maps to
/// SkipUI's AppStorage backed by SkipFoundation's UserDefaults (Android SharedPreferences).
struct ContentView: View {
    @AppStorage("count") var count = 0

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Count: \(count)")
                HStack(spacing: 12) {
                    Button("-") {
                        count -= 1
                    }
                    Button("+") {
                        count += 1
                    }
                }
                if count > 0 {
                    Text("Positive")
                }
                NavigationLink("Details") {
                    Text("Detail for \(count)")
                }
                List {
                    Text("Alpha")
                    Text("Bravo")
                    Text("Charlie")
                }
            }
            .padding()
            .navigationTitle("Witness")
        }
    }
}
