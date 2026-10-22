package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import java.util.Locale

class JarvisNotificationListenerService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    override fun onCreate() {
        super.onCreate()
        initTts()
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun initTts() {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                try {
                    tts?.language = Locale("bn", "BD")
                } catch (_: Exception) {}
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { NotificationDigest.record(applicationContext, it) }
        if (!isAnnouncerEnabled) return
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return

        // Skip self notifications
        if (pkg == packageName) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        // Throttle repeated announcements
        val now = System.currentTimeMillis()
        val key = "::"
        if (key == lastAnnouncedMessage && now - lastAnnouncedTime < 8000) {
            return
        }
        lastAnnouncedMessage = key
        lastAnnouncedTime = now

        // Check if from communication apps or important apps
        val isChatApp = pkg in listOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.facebook.orca",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.android.mms"
        ) || pkg.contains("message") || pkg.contains("chat")

        val announcement = if (isChatApp) {
            val appLabel = when {
                pkg.contains("whatsapp") -> "হোয়াটসঅ্যাপ"
                pkg.contains("facebook") || pkg.contains("orca") -> "মেসেঞ্জার"
                pkg.contains("telegram") -> "টেলিগ্রাম"
                else -> "মেসেজ"
            }
            if (text.isNotEmpty()) {
                "আলামিন স্যার,  এ  এর নতুন মেসেজ এসেছে: "
            } else {
                "আলামিন স্যার,  এ  থেকে নোটিফিকেশন এসেছে!"
            }
        } else {
            "নোটিফিকেশন: , "
        }

        announce(announcement)
    }

    private fun announce(text: String) {
        if (!isTtsReady || tts == null) {
            initTts()
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "notif_" + System.currentTimeMillis())
    }

    companion object {
        private const val TAG = "JarvisNotifListener"
        var isAnnouncerEnabled: Boolean = true
        var lastAnnouncedMessage: String? = null
        var lastAnnouncedTime: Long = 0L

        fun isPermissionGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            val myComponent = ComponentName(context, JarvisNotificationListenerService::class.java).flattenToString()
            return !TextUtils.isEmpty(flat) && flat.contains(myComponent)
        }

        fun openPermissionSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening notification listener settings", e)
            }
        }
    }
}