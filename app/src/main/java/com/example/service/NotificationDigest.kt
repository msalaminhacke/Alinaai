package com.example.service

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification

data class CapturedNotification(
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long
)

/**
 * Keeps the last few notifications in memory only (never saved to disk).
 * Their content is sent to the model only when the user asks for a summary.
 */
object NotificationDigest {
    private const val MAX_ITEMS = 60
    private const val MAX_TEXT = 120
    private val items = ArrayDeque<CapturedNotification>()

    @Synchronized
    fun record(context: Context, sbn: StatusBarNotification) {
        if (sbn.packageName == context.packageName) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val label = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            sbn.packageName
        }
        items.addLast(CapturedNotification(label, title, text.take(MAX_TEXT), sbn.postTime))
        while (items.size > MAX_ITEMS) items.removeFirst()
    }

    @Synchronized
    fun summary(minutesBack: Int, isBn: Boolean): String {
        val since = System.currentTimeMillis() - minutesBack.coerceAtLeast(1) * 60_000L
        val recent = items.filter { it.time >= since }
        if (recent.isEmpty()) {
            return if (isBn) "গত $minutesBack মিনিটে কোনো নতুন নোটিফিকেশন নেই।"
            else "No new notifications in the last $minutesBack minutes."
        }
        val byApp = recent.groupBy { it.appLabel }.entries.sortedByDescending { it.value.size }
        val lines = byApp.map { (app, list) ->
            val last = list.last()
            val detail = listOf(last.title, last.text).filter { it.isNotEmpty() }.joinToString(": ")
            if (isBn) "$app: ${list.size}টি। শেষটা: $detail"
            else "$app: ${list.size}. Latest: $detail"
        }
        val header = if (isBn) "মোট ${recent.size}টি নোটিফিকেশন:" else "Total ${recent.size} notifications:"
        return (listOf(header) + lines).joinToString("\n")
    }
}
