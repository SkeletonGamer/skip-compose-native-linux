// POC 6 Jalon 4 : androidx.compose.ui.semantics.testTagsAsResourceId (Android-only : expose testTag comme
// resource-id pour l'outillage UI natif Android). Absent de la pile K/N. Cale compile-only.
package androidx.compose.ui.semantics

private val TestTagsAsResourceIdKey =
    SemanticsPropertyKey<Boolean>("TestTagsAsResourceId")

var SemanticsPropertyReceiver.testTagsAsResourceId: Boolean
    get() = throw UnsupportedOperationException("testTagsAsResourceId is write-only")
    set(value) { this[TestTagsAsResourceIdKey] = value }
