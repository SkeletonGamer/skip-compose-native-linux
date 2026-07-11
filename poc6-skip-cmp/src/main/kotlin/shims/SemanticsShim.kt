// POC 6 Jalon 2: testTagsAsResourceId is an Android accessibility semantics property.
package androidx.compose.ui.semantics

private var testTagsAsResourceIdBacking = false
var SemanticsPropertyReceiver.testTagsAsResourceId: Boolean
    get() = testTagsAsResourceIdBacking
    set(value) { testTagsAsResourceIdBacking = value }
