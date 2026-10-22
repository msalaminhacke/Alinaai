package com.example.bridge
import android.os.PowerManager
import com.example.memory.JarvisMemoryManager
import com.example.receiver.IncomingCallReceiver
import com.example.routine.JarvisAutomationManager
import com.example.security.SentryModeManager
import com.example.service.JarvisFloatingBubbleService
import com.example.service.JarvisNotificationListenerService


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.webkit.JavascriptInterface
import com.example.data.model.BatteryInfo

class AndroidDeviceBridge(private val context: Context) {

    val memoryManager by lazy { JarvisMemoryManager(context) }
    val automationManager by lazy { JarvisAutomationManager(context, this, memoryManager) }
    var sentryManager: SentryModeManager? = null
    var onVisionRequest: ((String) -> Unit)? = null
    var onScreenExplainRequest: (() -> Unit)? = null

    @JavascriptInterface
    fun makePhoneCall(nameOrNumber: String, isBengali: Boolean = false): String {
        val success = automationManager.makePhoneCall(nameOrNumber)
        return if (success) {
            if (isBengali) "$nameOrNumber কে ফোন কল করা হচ্ছে 📞" else "Calling $nameOrNumber 📞"
        } else {
            if (isBengali) "ফোন কল শুরু করতে ব্যর্থ হয়েছে।" else "Failed to initiate call."
        }
    }

    @JavascriptInterface
    fun sendSMS(number: String, message: String, isBengali: Boolean = false): String {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            if (isBengali) "$number এ এসএমএস প্রস্তুত করা হয়েছে ✉️" else "Prepared SMS for $number ✉️"
        } catch (e: Exception) {
            if (isBengali) "এসএমএস পাঠানো ব্যর্থ: ${e.localizedMessage}" else "SMS failed: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun sendWhatsAppMessage(number: String, message: String, isBengali: Boolean = false): String {
        return try {
            val cleanNumber = number.replace("+", "").replace("-", "").replace(" ", "")
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            if (isBengali) "হোয়াটসঅ্যাপে মেসেজ পাঠানো হচ্ছে 💬" else "Opening WhatsApp to send message 💬"
        } catch (e: Exception) {
            if (isBengali) "হোয়াটসঅ্যাপ ব্যর্থ: ${e.localizedMessage}" else "WhatsApp failed: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun answerIncomingCall(): String {
        val success = IncomingCallReceiver.answerCall(context)
        return if (success) "ইনকামিং কল রিসিভ করা হয়েছে 📞" else "কল রিসিভ করা যায়নি।"
    }

    @JavascriptInterface
    fun endIncomingCall(): String {
        val success = IncomingCallReceiver.endCall(context)
        return if (success) "ইনকামিং কল কেটে দেওয়া হয়েছে 📴" else "কল কাটা যায়নি।"
    }

    @JavascriptInterface
    fun enableSentryMode(countdown: Int = 5, isBengali: Boolean = false): String {
        sentryManager?.arm(countdown)
        return if (isBengali) "সেন্ট্রি মোড $countdown সেকেন্ড পর সক্রিয় হবে 🛡️" else "Sentry mode arming in $countdown seconds 🛡️"
    }

    @JavascriptInterface
    fun toggleFloatingBubble(enable: Boolean, isBengali: Boolean = false): String {
        return if (enable) {
            JarvisFloatingBubbleService.start(context)
            if (isBengali) "ফ্লোটিং বাবল সক্রিয় করা হয়েছে 🔮" else "Floating bubble active 🔮"
        } else {
            JarvisFloatingBubbleService.stop(context)
            if (isBengali) "ফ্লোটিং বাবল বন্ধ করা হয়েছে" else "Floating bubble stopped"
        }
    }

    @JavascriptInterface
    fun mediaControl(action: String, isBengali: Boolean = false): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val key = when (action.lowercase()) {
            "play", "pause", "toggle" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "volume_up" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                return if (isBengali) "ভলিউম বাড়ানো হয়েছে" else "Volume raised"
            }
            "volume_down" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                return if (isBengali) "ভলিউম কমানো হয়েছে" else "Volume lowered"
            }
            else -> return if (isBengali) "এই মিডিয়া কমান্ড চেনা যায়নি" else "Unknown media action"
        }
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, key))
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, key))
        return if (isBengali) "মিডিয়া কমান্ড পাঠানো হয়েছে: $action" else "Media command sent: $action"
    }

    fun toggleNotificationAnnouncer(enable: Boolean, isBengali: Boolean = false): String {
        JarvisNotificationListenerService.isAnnouncerEnabled = enable
        IncomingCallReceiver.isAnnouncerEnabled = enable
        return if (enable) {
            if (isBengali) "ভয়েস অ্যানাউন্সার চালু করা হয়েছে 🔔" else "Notification announcer enabled 🔔"
        } else {
            if (isBengali) "ভয়েস অ্যানাউন্সার বন্ধ করা হয়েছে" else "Notification announcer disabled"
        }
    }

    @JavascriptInterface
    fun toggleWifi(enable: Boolean, isBengali: Boolean = false): String {
        val res = automationManager.toggleWifi(enable)
        return if (enable) {
            if (isBengali) "ওয়াইফাই চালু করা হয়েছে 📶" else "Wi-Fi enabled 📶"
        } else {
            if (isBengali) "ওয়াইফাই বন্ধ করা হয়েছে" else "Wi-Fi disabled"
        }
    }

    @JavascriptInterface
    fun toggleBluetooth(enable: Boolean, isBengali: Boolean = false): String {
        val res = automationManager.toggleBluetooth(enable)
        return if (enable) {
            if (isBengali) "ব্লুটুথ চালু করা হয়েছে ᛒ" else "Bluetooth enabled ᛒ"
        } else {
            if (isBengali) "ব্লুটুথ বন্ধ করা হয়েছে" else "Bluetooth disabled"
        }
    }

    @JavascriptInterface
    fun setAlarm(hour: Int, minute: Int, label: String = "Alina Alarm", isBengali: Boolean = false): String {
        val ok = automationManager.setAlarm(hour, minute, label)
        return if (ok) {
            if (isBengali) "$hour:$minute এ অ্যালার্ম সেট করা হয়েছে ⏰" else "Alarm set for $hour:$minute ⏰"
        } else {
            if (isBengali) "অ্যালার্ম সেট করা যায়নি" else "Failed to set alarm"
        }
    }

    @JavascriptInterface
    fun setTimer(seconds: Int, label: String = "Alina Timer", isBengali: Boolean = false): String {
        val ok = automationManager.setTimer(seconds, label)
        return if (ok) {
            if (isBengali) "$seconds সেকেন্ডের টাইমার চালু করা হয়েছে ⏳" else "Timer set for $seconds seconds ⏳"
        } else {
            if (isBengali) "টাইমার সেট করা যায়নি" else "Failed to set timer"
        }
    }

    @JavascriptInterface
    fun saveMemory(key: String, value: String, isBengali: Boolean = false): String {
        memoryManager.saveFact(key, value)
        return if (isBengali) "মনে রাখলাম: $key = $value 🧠" else "Remembered: $key = $value 🧠"
    }

    @JavascriptInterface
    fun getMemory(key: String, isBengali: Boolean = false): String {
        val fact = memoryManager.getFact(key)
        return if (fact != null) {
            if (isBengali) "আমার মনে আছে: $key = $fact" else "I remember: $key = $fact"
        } else {
            if (isBengali) "$key সম্পর্কে কোনো তথ্য মনে নেই।" else "No memory found for $key."
        }
    }

    @JavascriptInterface
    fun runAutomationRoutine(routineName: String, isBengali: Boolean = false): String {
        return automationManager.runRoutine(routineName)
    }

    @JavascriptInterface
    fun explainCurrentScreen(isBengali: Boolean = false): String {
        onScreenExplainRequest?.invoke()
        return if (isBengali) "বর্তমান স্ক্রিন বিশ্লেষণ করা হচ্ছে 👁️" else "Analyzing current screen 👁️"
    }

    @JavascriptInterface
    fun analyzeVisualScene(query: String = "", isBengali: Boolean = false): String {
        onVisionRequest?.invoke(query)
        return if (isBengali) "ক্যামেরা স্ক্যানার চালু করা হচ্ছে 📷" else "Opening camera scanner 📷"
    }

    fun openAppDetailsSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openMiuiAutostartSettings() {
        try {
            val intent = Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings()
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else true
    }


    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private var isTorchOn = false

    @JavascriptInterface
    fun toggleFlashlight(enable: Boolean, isBengali: Boolean = false): String {
        return try {
            val cm = cameraManager ?: return if (isBengali) "এই ডিভাইসে ফ্ল্যাশলাইট নেই।" else "Flashlight not supported on this device."
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return if (isBengali) "কোনো ফ্ল্যাশ ক্যামেরা পাওয়া যায়নি।" else "No flash camera found on device."

            cm.setTorchMode(cameraId, enable)
            isTorchOn = enable
            if (enable) {
                if (isBengali) "ফ্ল্যাশলাইট জ্বালিয়ে দিয়েছি ✨" else "Flashlight turned ON ✨"
            } else {
                if (isBengali) "ফ্ল্যাশলাইট বন্ধ করে দিয়েছি 🌑" else "Flashlight turned OFF 🌑"
            }
        } catch (e: CameraAccessException) {
            Log.e("AndroidBridge", "Camera access error toggling flashlight", e)
            if (isBengali) "ফ্ল্যাশলাইট অ্যাক্সেস করা যায়নি: ${e.localizedMessage}" else "Could not access flashlight: ${e.localizedMessage}"
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error toggling flashlight", e)
            if (isBengali) "ফ্ল্যাশলাইট সমস্যা: ${e.localizedMessage}" else "Flashlight error: ${e.localizedMessage}"
        }
    }

    fun isFlashlightActive(): Boolean = isTorchOn

    @JavascriptInterface
    fun getBatteryStatus(isBengali: Boolean = false): String {
        val info = getBatteryInfo()
        val chargingStr = if (info.isCharging) {
            if (isBengali) "চার্জ হচ্ছে ⚡" else "charging ⚡"
        } else {
            if (isBengali) "চার্জ হচ্ছে না" else "discharging"
        }
        return if (isBengali) {
            "ব্যাটারি চার্জ আছে ${info.percentage}%, $chargingStr"
        } else {
            "Battery is at ${info.percentage}%, $chargingStr."
        }
    }

    fun getBatteryInfo(): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val pct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85
        return BatteryInfo(
            percentage = pct,
            isCharging = isCharging,
            statusText = if (isCharging) "Charging ($pct%)" else "Battery $pct%"
        )
    }

    @JavascriptInterface
    fun openWebsite(rawUrl: String, isBengali: Boolean = false): String {
        return try {
            var url = rawUrl.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            if (isBengali) "ব্রাউজারে $url খোলা হয়েছে 🌐" else "Opened $url in browser 🌐"
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error opening URL: $rawUrl", e)
            if (isBengali) "ওয়েবসাইট খোলা সম্ভব হয়নি: ${e.localizedMessage}" else "Failed to open website: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun openApp(appName: String, isBengali: Boolean = false): String {
        val query = appName.lowercase().trim()
        val packageManager = context.packageManager

        // Known common package mappings
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "camera" to null, // handled by camera intent
            "maps" to "com.google.android.apps.maps",
            "settings" to "com.android.settings",
            "chrome" to "com.android.chrome",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "spotify" to "com.spotify.music",
            "music" to "com.google.android.apps.youtube.music"
        )

        try {
            if (query.contains("camera")) {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return if (isBengali) "ক্যামেরা চালু করা হয়েছে 📸" else "Launched Camera app 📸"
            }

            if (query.contains("setting")) {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return if (isBengali) "সেটিংস খোলা হয়েছে ⚙️" else "Opened Settings ⚙️"
            }

            val targetPkg = knownPackages.entries.firstOrNull { query.contains(it.key) }?.value

            if (targetPkg != null) {
                val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val disp = appName.replaceFirstChar { it.uppercase() }
                    return if (isBengali) "$disp অ্যাপ চালু হয়েছে 🚀" else "Opened $disp 🚀"
                }
            }

            // Search installed packages
            val installed = packageManager.getInstalledApplications(0)
            for (app in installed) {
                val label = packageManager.getApplicationLabel(app).toString().lowercase()
                if (label.contains(query) || query.contains(label)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return if (isBengali) "$label অ্যাপটি খোলা হয়েছে 📱" else "Opened $label 📱"
                    }
                }
            }

            // Fallback: search in Play Store or Browser
            val browserSearch = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(appName)}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(browserSearch)
            return if (isBengali) "$appName অনুসন্ধান করা হচ্ছে..." else "Searching for $appName online..."
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error launching app: $appName", e)
            return if (isBengali) "$appName খোলা যায়নি: ${e.localizedMessage}" else "Could not open $appName: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun vibrateDevice(durationMs: Int = 300, isBengali: Boolean = false): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs.toLong().coerceIn(50L, 2000L), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs.toLong().coerceIn(50L, 2000L))
            }
            return if (isBengali) "ডিভাইসটিতে মৃদু কম্পন দেওয়া হয়েছে 📳" else "Device vibrated for ${durationMs}ms 📳"
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Vibrate error", e)
            return if (isBengali) "কম্পন ব্যর্থ হয়েছে: ${e.localizedMessage}" else "Vibration failed: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun adjustVolume(percent: Int, isBengali: Boolean = false): String {
        return try {
            val am = audioManager ?: return if (isBengali) "অডিও ম্যানেজার পাওয়া যায়নি" else "Audio manager unavailable"
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = ((percent.coerceIn(0, 100) / 100f) * maxVol).toInt()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            if (isBengali) "ভলিউম $percent% এ পরিবর্তন করা হয়েছে 🔊" else "Volume adjusted to $percent% 🔊"
        } catch (e: Exception) {
            "Could not adjust volume: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun sendWhatsAppMessage(phoneNumber: String, message: String, isBengali: Boolean = false): String {
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            val encodedMsg = Uri.encode(message)
            val uri = if (cleanPhone.isNotEmpty()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.whatsapp")
            }
            try {
                context.startActivity(intent)
            } catch (ex: Exception) {
                // WhatsApp app not found, open in browser
                val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            if (isBengali) {
                "হোয়াটসঅ্যাপে বার্তাটি সফলভাবে পাঠানো হচ্ছে: \"${message.take(40)}...\" 💬"
            } else {
                "Dispatching WhatsApp message: \"${message.take(40)}...\" 💬"
            }
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error sending WhatsApp message", e)
            if (isBengali) "হোয়াটসঅ্যাপে বার্তা পাঠাতে সমস্যা হয়েছে: ${e.localizedMessage}" else "Failed to send WhatsApp message: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun playYouTubeSong(songName: String, isBengali: Boolean = false): String {
        return try {
            val query = songName.trim()
            val uri = Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.google.android.youtube")
            }
            try {
                context.startActivity(intent)
            } catch (ex: Exception) {
                // Fallback to browser
                val webUri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            if (isBengali) {
                "ইউটিউবে '$query' প্লে করা হচ্ছে 🎵"
            } else {
                "Playing '$query' on YouTube 🎵"
            }
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error playing YouTube song", e)
            if (isBengali) "ইউটিউবে গান চালাতে সমস্যা হয়েছে: ${e.localizedMessage}" else "Failed to play YouTube song: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun goHome(isBengali: Boolean = false): String {
        return try {
            val success = com.example.service.JarvisAccessibilityService.goHome()
            if (!success) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            if (isBengali) "হোম স্ক্রিনে ফিরে যাওয়া হয়েছে 🏠" else "Navigated to Home screen 🏠"
        } catch (e: Exception) {
            if (isBengali) "হোমে যেতে ব্যর্থ: ${e.localizedMessage}" else "Failed to go home: ${e.localizedMessage}"
        }
    }

    @JavascriptInterface
    fun goBack(isBengali: Boolean = false): String {
        val success = com.example.service.JarvisAccessibilityService.goBack()
        return if (success) {
            if (isBengali) "পূর্বের স্ক্রিনে ফিরে যাওয়া হয়েছে 🔙" else "Navigated back 🔙"
        } else {
            if (isBengali) "ব্যাক করতে অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় করতে হবে ⚙️" else "Accessibility service required for back navigation"
        }
    }

    @JavascriptInterface
    fun openRecentApps(isBengali: Boolean = false): String {
        val success = com.example.service.JarvisAccessibilityService.openRecents()
        return if (success) {
            if (isBengali) "রিসেন্ট অ্যাপস খোলা হয়েছে 🗂️" else "Opened Recent Apps 🗂️"
        } else {
            if (isBengali) "রিসেন্ট অ্যাপস খুলতে অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় করতে হবে ⚙️" else "Accessibility service required for recents"
        }
    }

    @JavascriptInterface
    fun readScreenContent(isBengali: Boolean = false): String {
        return com.example.service.JarvisAccessibilityService.readScreenContent()
    }

    @JavascriptInterface
    fun findAndClickElement(targetText: String, isBengali: Boolean = false): String {
        val success = com.example.service.JarvisAccessibilityService.findAndClick(targetText)
        return if (success) {
            if (isBengali) "'$targetText' এ সফলভাবে ক্লিক করা হয়েছে 👆" else "Successfully clicked '$targetText' 👆"
        } else {
            if (isBengali) "স্ক্রিনে '$targetText' খুঁজে পাওয়া যায়নি বা ক্লিক করা সম্ভব হয়নি" else "Could not find or click element '$targetText'"
        }
    }

    @JavascriptInterface
    fun typeTextIntoField(textToType: String, isBengali: Boolean = false): String {
        val success = com.example.service.JarvisAccessibilityService.typeText(textToType)
        return if (success) {
            if (isBengali) "\"$textToType\" সফলভাবে টাইপ করা হয়েছে ✍️" else "Typed \"$textToType\" into field ✍️"
        } else {
            if (isBengali) "কোনো সক্রিয় টেক্সট ফিল্ড পাওয়া যায়নি" else "No active text field found to type into"
        }
    }

    @JavascriptInterface
    fun scrollScreen(direction: String, isBengali: Boolean = false): String {
        val success = com.example.service.JarvisAccessibilityService.scroll(direction)
        return if (success) {
            if (isBengali) "$direction দিকে স্ক্রল করা হয়েছে 📜" else "Scrolled $direction 📜"
        } else {
            if (isBengali) "স্ক্রল করা সম্ভব হয়নি" else "Could not scroll screen"
        }
    }

    @JavascriptInterface
    fun getCurrentDateTime(isBengali: Boolean = false): String {
        val now = java.util.Date()
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault())
        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)
        return if (isBengali) {
            "এখন সময় $timeStr, আজ $dateStr 🕒"
        } else {
            "Current time is $timeStr on $dateStr 🕒"
        }
    }

    @JavascriptInterface
    fun openSettingsPage(page: String, isBengali: Boolean = false): String {
        return try {
            val action = when (page.lowercase().trim()) {
                "wifi" -> android.provider.Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
                "accessibility" -> android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                "sound", "volume" -> android.provider.Settings.ACTION_SOUND_SETTINGS
                "display" -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
                "battery" -> android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
                else -> android.provider.Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
            if (isBengali) "$page সেটিংস পেজ খোলা হয়েছে ⚙️" else "Opened $page settings ⚙️"
        } catch (e: Exception) {
            if (isBengali) "সেটিংস খুলতে ব্যর্থ: ${e.localizedMessage}" else "Failed to open settings: ${e.localizedMessage}"
        }
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error opening accessibility settings", e)
        }
    }

    fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            vibrateDevice(50, false)
            true
        } catch (e: Exception) {
            Log.e("AndroidBridge", "Error copying to clipboard", e)
            false
        }
    }
}
