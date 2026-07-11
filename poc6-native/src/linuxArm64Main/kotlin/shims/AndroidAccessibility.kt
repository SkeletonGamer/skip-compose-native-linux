// POC 6 Jalon 4 : surface accessibilite + database observer (AccessibilityEnvironment de SkipUI).
// Un fichier = un package : ce fichier ne porte QUE android.view.accessibility.
package android.view.accessibility

class AccessibilityManager {
    val isEnabled: Boolean get() = TODO("K/N accessibility stub")
    val isTouchExplorationEnabled: Boolean get() = TODO("K/N accessibility stub")
    fun getEnabledAccessibilityServiceList(feedbackTypeFlags: Int): List<android.accessibilityservice.AccessibilityServiceInfo> = TODO("K/N accessibility stub")
    fun addAccessibilityStateChangeListener(listener: AccessibilityStateChangeListener): Boolean = TODO("K/N accessibility stub")
    fun addTouchExplorationStateChangeListener(listener: TouchExplorationStateChangeListener): Boolean = TODO("K/N accessibility stub")
    fun addAccessibilityServicesStateChangeListener(listener: AccessibilityServicesStateChangeListener) { TODO("K/N accessibility stub") }

    fun interface AccessibilityStateChangeListener {
        fun onAccessibilityStateChanged(enabled: Boolean)
    }
    fun interface TouchExplorationStateChangeListener {
        fun onTouchExplorationStateChanged(enabled: Boolean)
    }
    fun interface AccessibilityServicesStateChangeListener {
        fun onAccessibilityServicesStateChanged(manager: AccessibilityManager)
    }
}
