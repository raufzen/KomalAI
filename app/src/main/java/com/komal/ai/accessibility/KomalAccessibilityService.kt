package com.komal.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class KomalAccessibilityService : AccessibilityService() {

    companion object {
        var instance: KomalAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("KomalAccessibility", "Komal Accessibility Service Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be used to track foreground package or window state changes
    }

    override fun onInterrupt() {
        Log.w("KomalAccessibility", "Komal Accessibility Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.d("KomalAccessibility", "Komal Accessibility Service Destroyed.")
    }

    /**
     * Extracts a compact hierarchical summary of text and clickable elements on screen
     */
    fun dumpScreenHierarchy(): String {
        val root = rootInActiveWindow ?: return "Screen is empty or unavailable."
        val builder = StringBuilder()
        traverseNode(root, builder, 0)
        return builder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, builder: StringBuilder, depth: Int) {
        if (depth > 12) return // avoid deep recursion

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        if (!text.isNullOrEmpty() || !desc.isNullOrEmpty() || isClickable || isEditable) {
            val indent = "  ".repeat(depth)
            builder.append(indent)
            if (isClickable) builder.append("[Button] ")
            if (isEditable) builder.append("[Input] ")
            if (!text.isNullOrEmpty()) builder.append("text=\"$text\" ")
            if (!desc.isNullOrEmpty()) builder.append("desc=\"$desc\" ")
            if (!viewId.isNullOrEmpty()) builder.append("id=\"$viewId\" ")
            builder.append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, builder, depth + 1)
        }
    }

    /**
     * Click element matching text or content description
     */
    fun clickByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in matchedNodes) {
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    /**
     * Types text into the currently focused or active input field
     */
    fun typeTextIntoActiveField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false

        // First attempt: find currently focused node
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // Second attempt: find any editable node on screen
        val editableNode = findFirstEditableNode(root)
        if (editableNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Tap at exact screen coordinates (x, y) via GestureDescription
     */
    fun tapCoordinates(x: Float, y: Float, onComplete: (() -> Unit)? = null): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onComplete?.invoke()
            }
        }, null)
    }

    /**
     * Scroll up or down
     */
    fun scrollScreen(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (direction.equals("down", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

        // Try to perform scroll on scrollable node
        val scrollable = findFirstScrollableNode(root)
        return scrollable?.performAction(action) ?: false
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Global Navigation Actions: Home, Back, Recents, Notifications
     */
    fun performNavigation(action: String): Boolean {
        return when (action.lowercase()) {
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            else -> false
        }
    }
}
