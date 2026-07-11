// POC 6 Jalon 4 : android.accessibilityservice.AccessibilityServiceInfo (constantes de feedback). Cale K/N.
package android.accessibilityservice

class AccessibilityServiceInfo {
    val feedbackType: Int get() = TODO("K/N stub")
    val settingsActivityName: String? get() = TODO("K/N stub")
    companion object {
        const val FEEDBACK_GENERIC: Int = 0x0010
        const val FEEDBACK_ALL_MASK: Int = -1
        const val FEEDBACK_SPOKEN: Int = 0x0001
        const val FEEDBACK_HAPTIC: Int = 0x0002
    }
}
