package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "JarvisAccessibilityService connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track current focused / interactive package if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "JarvisAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "JarvisAccessibility"
        var instance: JarvisAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun goBack(): Boolean {
            val s = instance ?: return false
            return s.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun goHome(): Boolean {
            val s = instance ?: return false
            return s.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun openRecents(): Boolean {
            val s = instance ?: return false
            return s.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun takeScreenshot(): Boolean {
            val s = instance ?: return false
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                s.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else false
        }

        fun readScreenContent(): String {
            val root = instance?.rootInActiveWindow ?: return "Screen content unavailable (Accessibility service inactive or screen secure)"
            val builder = StringBuilder()
            extractNodeText(root, builder, 0)
            return if (builder.isEmpty()) "Screen appears empty or content is not readable." else builder.toString()
        }

        private fun extractNodeText(node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int) {
            if (node == null || depth > 8) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val className = node.className?.toString()?.substringAfterLast('.') ?: ""
            val isClickable = node.isClickable

            if (!text.isNullOrEmpty() || !desc.isNullOrEmpty()) {
                val label = text ?: desc
                out.append("• [$className")
                if (isClickable) out.append(" (clickable)")
                out.append("]: \"").append(label).append("\"\n")
            }

            for (i in 0 until node.childCount) {
                extractNodeText(node.getChild(i), out, depth + 1)
            }
        }

        fun findAndClick(targetText: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val query = targetText.lowercase().trim()

            // 1. Search by text
            val matchedByText = root.findAccessibilityNodeInfosByText(query)
            if (!matchedByText.isNullOrEmpty()) {
                for (node in matchedByText) {
                    if (clickNodeOrParent(node)) return true
                }
            }

            // 2. Search recursively by content description or partial text
            return searchAndClickNode(root, query)
        }

        private fun searchAndClickNode(node: AccessibilityNodeInfo?, query: String): Boolean {
            if (node == null) return false
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            if (text.contains(query) || desc.contains(query)) {
                if (clickNodeOrParent(node)) return true
            }

            for (i in 0 until node.childCount) {
                if (searchAndClickNode(node.getChild(i), query)) return true
            }
            return false
        }

        private fun clickNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
            var curr = node
            while (curr != null) {
                if (curr.isClickable) {
                    return curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                curr = curr.parent
            }
            return false
        }

        fun typeText(textToType: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            // Find focused node
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
                }
                return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }

            // If no focus, find first EditText
            val editTexts = mutableListOf<AccessibilityNodeInfo>()
            findNodesByClass(root, "EditText", editTexts)
            if (editTexts.isNotEmpty()) {
                val target = editTexts.first()
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
                }
                return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
            return false
        }

        private fun findNodesByClass(node: AccessibilityNodeInfo?, simpleClass: String, out: MutableList<AccessibilityNodeInfo>) {
            if (node == null) return
            val cls = node.className?.toString() ?: ""
            if (cls.contains(simpleClass, ignoreCase = true)) {
                out.add(node)
            }
            for (i in 0 until node.childCount) {
                findNodesByClass(node.getChild(i), simpleClass, out)
            }
        }

        fun scroll(direction: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val action = if (direction.equals("up", ignoreCase = true) || direction.equals("backward", ignoreCase = true)) {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }

            val scrollables = mutableListOf<AccessibilityNodeInfo>()
            findScrollableNodes(root, scrollables)
            for (node in scrollables) {
                if (node.performAction(action)) return true
            }
            return false
        }

        private fun findScrollableNodes(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
            if (node == null) return
            if (node.isScrollable) {
                out.add(node)
            }
            for (i in 0 until node.childCount) {
                findScrollableNodes(node.getChild(i), out)
            }
        }
    }
}
