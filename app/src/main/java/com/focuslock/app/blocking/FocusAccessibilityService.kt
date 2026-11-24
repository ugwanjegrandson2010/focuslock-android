package com.focuslock.app.blocking

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focuslock.app.FocusLockApp
import com.focuslock.app.domain.MarkerMatcher

class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: AccessibilityServiceInfo().also { serviceInfo = it }
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val current = event ?: return
        val app = application as? FocusLockApp ?: return
        val manager = app.container.sessionManager
        if (!manager.isActiveNow()) return

        val pkg = current.packageName?.toString() ?: return
        if (pkg == packageName) return

        when (current.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (manager.isFullAppBlocked(pkg)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    manager.onBlockDetected(pkg, "APP")
                    OverlayService.start(this, manager.currentSessionName())
                    return
                }
                checkFeatureSurfaces(pkg, manager)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!manager.isFullAppBlocked(pkg)) checkFeatureSurfaces(pkg, manager)
            }
        }
    }

    private fun checkFeatureSurfaces(pkg: String, manager: com.focuslock.app.domain.SessionManager) {
        if (!manager.hasFeatureTargets() || !manager.isFeatureTarget(pkg)) return
        val root = rootInActiveWindow ?: return
        val matched = containsMarker(root, manager.featureMarkers(), 0)
        root.recycle()
        if (matched) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)
            manager.onBlockDetected(pkg, "FEATURE")
            OverlayService.start(this, manager.currentSessionName())
        }
    }

    private fun containsMarker(node: AccessibilityNodeInfo?, markers: List<String>, depth: Int): Boolean {
        if (node == null || depth > 8) return false
        val texts = listOf(node.text?.toString(), node.contentDescription?.toString(), node.viewIdResourceName)
        if (MarkerMatcher.matches(texts, markers)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsMarker(child, markers, depth + 1)) return true
            child.recycle()
        }
        return false
    }

    companion object {
        @Volatile
        var instance: FocusAccessibilityService? = null
            private set
    }
}
