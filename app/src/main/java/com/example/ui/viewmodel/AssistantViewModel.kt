package com.example.ui.viewmodel
import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import com.example.audio.SciFiSoundEffects
import com.example.receiver.IncomingCallReceiver
import com.example.security.SentryModeManager
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisBackgroundService
import com.example.service.JarvisFloatingBubbleService
import com.example.service.JarvisNotificationListenerService
import com.example.vision.JarvisVisionManager
import com.example.wakeword.WakeWordManager


import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioStreamer
import com.example.bridge.AndroidDeviceBridge
import com.example.data.model.AssistantState
import com.example.data.model.AvailableVoices
import com.example.data.model.BatteryInfo
import com.example.data.model.DeviceActionResult
import com.example.session.GeminiLiveSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    val sentryModeManager by lazy {
        SentryModeManager(getApplication<Application>().applicationContext, deviceBridge, audioStreamer)
    }
    val visionManager by lazy {
        JarvisVisionManager(getApplication<Application>().applicationContext, audioStreamer)
    }
    val wakeWordManager by lazy {
        WakeWordManager(getApplication<Application>().applicationContext) {
            com.example.wakeword.WakePersona.fromPhrase(com.example.wakeword.WakeWordManager.lastHeard)?.let { setCharacter(it) }
            startAssistant(true)
        }
    }

    private val _showVisionScanner = MutableStateFlow(false)
    val showVisionScanner: StateFlow<Boolean> = _showVisionScanner.asStateFlow()

    private val _isVisionAnalyzing = MutableStateFlow(false)
    val isVisionAnalyzing: StateFlow<Boolean> = _isVisionAnalyzing.asStateFlow()

    private val _visionResult = MutableStateFlow("")
    val visionResult: StateFlow<String> = _visionResult.asStateFlow()

    private val _visionQuery = MutableStateFlow("")
    val visionQuery: StateFlow<String> = _visionQuery.asStateFlow()

    private val _isBubbleActive = MutableStateFlow(false)
    val isBubbleActive: StateFlow<Boolean> = _isBubbleActive.asStateFlow()

    private val _isNotificationAnnouncerEnabled = MutableStateFlow(true)
    val isNotificationAnnouncerEnabled: StateFlow<Boolean> = _isNotificationAnnouncerEnabled.asStateFlow()

    val isSentryArmed: StateFlow<Boolean> by lazy { sentryModeManager.isArmed }
    val isSentryAlarmTriggered: StateFlow<Boolean> by lazy { sentryModeManager.isAlarmTriggered }
    val sentryCountdown: StateFlow<Int> by lazy { sentryModeManager.armCountdown }
    val isWakeWordActive: StateFlow<Boolean> by lazy { wakeWordManager.isWakeWordEnabled }


    init {
        deviceBridge.sentryManager = sentryModeManager
        deviceBridge.onVisionRequest = { query ->
            openCameraVision(query)
        }
        deviceBridge.onScreenExplainRequest = {
            explainCurrentScreen()
        }
    }


    val deviceBridge = AndroidDeviceBridge(application.applicationContext)
    private val audioStreamer = AudioStreamer()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _speakerAmplitude = MutableStateFlow(0f)
    val speakerAmplitude: StateFlow<Float> = _speakerAmplitude.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _pendingAction = MutableStateFlow<com.example.session.PendingDeviceAction?>(null)
    val pendingAction: StateFlow<com.example.session.PendingDeviceAction?> = _pendingAction.asStateFlow()

    private val _apiKey = MutableStateFlow(
        application.getSharedPreferences("dev_profile_prefs", android.content.Context.MODE_PRIVATE)
            .getString("gemini_api_key", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
    )
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _showDeveloperProfile = MutableStateFlow(false)
    val showDeveloperProfile: StateFlow<Boolean> = _showDeveloperProfile.asStateFlow()

    private val _selectedCharacter = MutableStateFlow(com.example.data.model.AssistantCharacter.ALINA)
    val selectedCharacter: StateFlow<com.example.data.model.AssistantCharacter> = _selectedCharacter.asStateFlow()

    private val _selectedVoice = MutableStateFlow(AvailableVoices.first().id)
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-2.0-flash-exp")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("bn")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _recentActions = MutableStateFlow<List<DeviceActionResult>>(emptyList())
    val recentActions: StateFlow<List<DeviceActionResult>> = _recentActions.asStateFlow()

    private val _batteryInfo = MutableStateFlow(deviceBridge.getBatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _sassyQuote = MutableStateFlow("আমাকে সক্রিয় করতে ট্যাপ করো, বন্ধু! 😊")
    val sassyQuote: StateFlow<String> = _sassyQuote.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val prefs = application.getSharedPreferences("dev_profile_prefs", android.content.Context.MODE_PRIVATE)

    init {
        com.example.session.CustomPromptStore.init(application)
        com.example.reminder.ReminderManager.init(application)
    }
    private val _developerAvatarUri = MutableStateFlow<String?>(prefs.getString("dev_avatar_uri", null))
    val developerAvatarUri: StateFlow<String?> = _developerAvatarUri.asStateFlow()

    fun verifyDeveloperPassword(input: String): Boolean {
        return input.trim() == "@alaminnote8#"
    }

    fun saveCustomDeveloperAvatar(uri: android.net.Uri): Boolean {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val file = java.io.File(context.filesDir, "custom_developer_avatar.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val path = file.absolutePath
            prefs.edit().putString("dev_avatar_uri", path).apply()
            _developerAvatarUri.value = path
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveCustomDeveloperAvatarPath(path: String) {
        prefs.edit().putString("dev_avatar_uri", path).apply()
        _developerAvatarUri.value = path
    }

    fun resetDeveloperAvatarToDefault() {
        try {
            val context = getApplication<Application>().applicationContext
            val file = java.io.File(context.filesDir, "custom_developer_avatar.jpg")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.edit().remove("dev_avatar_uri").apply()
        _developerAvatarUri.value = null
    }

    private var durationJob: Job? = null
    private var quoteTickerJob: Job? = null

    private val alinaQuotesBn = listOf(
        "কী খবর বন্ধু, কেমন কাটছে তোমার সময়? বলো শুনছি... 😊",
        "আমি সবসময় তোমার পাশে আছি বন্ধু, যেকোনো কিছু বলো! ✨",
        "আজকের দিনটা তোমার অনেক সুন্দর আর সফল কাটুক! 🌟",
        "বলো 'ফ্ল্যাশলাইট জ্বালাও' বা 'ইউটিউব খোলো'... আমি করে দিচ্ছি! ⚡",
        "যেকোনো প্রশ্ন বা মনের কথা নির্দ্বিধায় আমার সাথে শেয়ার করো! 💬",
        "তোমার মতো একজন চমৎকার বন্ধু পেয়ে আমি সত্যিই আনন্দিত! 🌸",
        "আমি মনোযোগ দিয়ে শুনছি বন্ধু... তোমার কথাগুলো বলো! 🤝",
        "কাজের ফাঁকে একটু কথা বলে মন ভালো করে নিই চলো! 🌈"
    )

    private val bleverQuotesBn = listOf(
        "কী খবর দোস্ত! একদম চাঙ্গা আছো তো? বলো কী করতে হবে! 🚀",
        "আমি ব্লিভার, তোমার স্মার্ট বন্ধু! ফোন চালানো থেকে যেকোনো কাজ, আমি রেডি! 🔥",
        "বলো দোস্ত, 'হোমে যাও', 'ইউটিউবে গান চালাও' নাকি 'টর্চ জ্বালাও'? ⚡",
        "আরে বন্ধু, কোনো টেনশন নিও না! একসাথে থাকলে সব সহজ! 🦾",
        "আমার কাজ তোমাকে সেরা সাপোর্ট দেওয়া, বলো কী মিশন আজ? 🎯",
        "সুপার স্মার্ট Jarvis স্টাইলে সব কাজ নিমেষেই শেষ করব! 🤖",
        "আমি শুনছি দোস্ত, বলো কী হেল্প লাগবে? 🎧",
        "ফাটাফাটি একটা দিন শুরু করতে প্রস্তুত তো? চলো! 💥"
    )

    private val alinaQuotesEn = listOf(
        "Hey friend! How's your day going? I'm right here for you 😊",
        "Feel free to ask me anything or tell me what's on your mind! ✨",
        "Wishing you a productive and happy day ahead, friend! 🌟",
        "Say 'turn on flashlight' or 'open YouTube'... I've got you covered! ⚡",
        "Always here to listen, support, and help whenever you need 🤝",
        "I'm listening closely, my friend. What would you like to explore? 💬"
    )

    private val bleverQuotesEn = listOf(
        "What's up buddy! Ready to get things done today? 🚀",
        "Blever in the house! Your Jarvis-powered tech buddy at your service! 🔥",
        "Need to launch apps, send WhatsApp messages, or play songs? Just say it! ⚡",
        "Zero worries, my friend. We tackle everything together! 🦾",
        "Listening sharp and clear, bro! What's the plan? 🎧"
    )

    private val jarvisQuotesBn = listOf(
        "সব সিস্টেম অনলাইন স্যার। আদেশ করুন। ⚙️",
        "জার্ভিস রিপোর্ট করছে: আপনার সহকারী প্রস্তুত। 🤖",
        "বলুন স্যার, আজ কোন কাজটা আগে করব? 🎯",
        "আমি শুনছি স্যার, নির্দেশ দিন। 🎧"
    )

    private val jarvisQuotesEn = listOf(
        "All systems online. Awaiting your command. ⚙️",
        "Jarvis at your service. What shall we do first? 🎯"
    )

    private fun getRandomQuote(): String {
        val isBn = _selectedLanguage.value == "bn"
        return when (_selectedCharacter.value) {
            com.example.data.model.AssistantCharacter.JARVIS -> if (isBn) jarvisQuotesBn.random() else jarvisQuotesEn.random()
            com.example.data.model.AssistantCharacter.BLEVER -> if (isBn) bleverQuotesBn.random() else bleverQuotesEn.random()
            else -> if (isBn) alinaQuotesBn.random() else alinaQuotesEn.random()
        }
    }

    private val liveSession = GeminiLiveSession(
        deviceBridge = deviceBridge,
        coroutineScope = viewModelScope,
        onStateChange = { newState ->
            _assistantState.value = newState
            if (newState == AssistantState.LISTENING) {
                _sassyQuote.value = getRandomQuote()
            }
        },
        onAudioReceived = { audioBytes ->
            audioStreamer.enqueueAudioChunk(audioBytes)
        },
        onInterrupted = {
            // User interrupted: immediately dump audio buffer
            audioStreamer.stopPlaybackAndFlush()
            _speakerAmplitude.value = 0f
            _sassyQuote.value = if (_selectedLanguage.value == "bn") {
                "হ্যাঁ বন্ধু, আমি শুনছি, বলো! 👂"
            } else {
                "Yes friend, I'm listening, go ahead! 👂"
            }
        },
        onDeviceAction = { action ->
            addDeviceAction(action)
            updateHardwareStates()
        },
        onError = { err ->
            _errorMessage.value = err
        }
    )

    init {
        startQuoteTicker()
        updateHardwareStates()
    }

    private fun startQuoteTicker() {
        quoteTickerJob?.cancel()
        quoteTickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(8000)
                if (_assistantState.value == AssistantState.LISTENING) {
                    _sassyQuote.value = getRandomQuote()
                }
            }
        }
    }

    fun toggleSession(hasRecordAudioPermission: Boolean) {
        if (_assistantState.value == AssistantState.IDLE || _assistantState.value == AssistantState.ERROR) {
            startAssistant(hasRecordAudioPermission)
        } else {
            stopAssistant()
        }
    }

    fun startAssistant(hasRecordAudioPermission: Boolean) {
        if (!hasRecordAudioPermission) {
            _errorMessage.value = if (_selectedLanguage.value == "bn") {
                "ভয়েস কথোপকথনের জন্য মাইক্রোফোন পারমিশন প্রয়োজন।"
            } else {
                "Microphone permission required for voice-to-voice conversation."
            }
            return
        }

        _errorMessage.value = null

        // 1. Initialize playback track for 24kHz audio output
        audioStreamer.initPlayback(viewModelScope) { amp ->
            _speakerAmplitude.value = amp
        }

        // 2. Start WebSocket Live Session
        liveSession.startSession(
            apiKey = _apiKey.value,
            voiceName = _selectedVoice.value,
            modelName = _selectedModel.value,
            language = _selectedLanguage.value
        )

        // 3. Start Recording Mic (16kHz PCM)
        val started = audioStreamer.startRecording(
            scope = viewModelScope,
            onPcmChunk = { chunk ->
                if (!_isMuted.value) {
                    liveSession.sendAudioChunk(chunk)
                }
            },
            onAmplitude = { amp ->
                if (!_isMuted.value) {
                    _micAmplitude.value = amp
                } else {
                    _micAmplitude.value = 0f
                }
            }
        )

        if (!started) {
            _errorMessage.value = if (_selectedLanguage.value == "bn") "মাইক্রোফোন রেকর্ডিং শুরু করতে ব্যর্থ হয়েছে" else "Failed to start microphone recording"
            _assistantState.value = AssistantState.ERROR
            return
        }

        startTimer()
    }

    fun stopAssistant() {
        liveSession.clearPendingConfirmation()
        liveSession.stopSession()
        audioStreamer.releaseAll()
        _assistantState.value = AssistantState.IDLE
        _micAmplitude.value = 0f
        _speakerAmplitude.value = 0f
        stopTimer()
        _sassyQuote.value = if (_selectedLanguage.value == "bn") {
            "কন্ঠকে একটু বিশ্রাম দিচ্ছি... কথা বলতে মাইকে ট্যাপ করো বন্ধু! 🌸"
        } else {
            "Resting my voice... Tap the mic whenever you want to talk, friend! 🌸"
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            _micAmplitude.value = 0f
        }
    }

    fun getCustomPrompt(character: com.example.data.model.AssistantCharacter): String =
        com.example.session.CustomPromptStore.get(character) ?: ""

    fun setCustomPrompt(character: com.example.data.model.AssistantCharacter, text: String) {
        com.example.session.CustomPromptStore.set(character, text)
    }

    fun resetCustomPrompt(character: com.example.data.model.AssistantCharacter) {
        com.example.session.CustomPromptStore.reset(character)
    }

    init {
        liveSession.onPendingAction = { _pendingAction.value = it }
    }

    fun cancelPendingAction() {
        _pendingAction.value = null
        liveSession.clearPendingConfirmation()
    }

    fun setApiKey(newKey: String) {
        _apiKey.value = newKey.trim()
        prefs.edit().putString("gemini_api_key", _apiKey.value).apply()
    }

    fun setCharacter(character: com.example.data.model.AssistantCharacter) {
        _selectedCharacter.value = character
        liveSession.setCharacter(character)
        _selectedVoice.value = character.defaultVoice
        deviceBridge.vibrateDevice(70, _selectedLanguage.value == "bn")
        _sassyQuote.value = getRandomQuote()
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        deviceBridge.vibrateDevice(40, false)
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        deviceBridge.vibrateDevice(40, false)
    }

    fun setShowDeveloperProfile(show: Boolean) {
        _showDeveloperProfile.value = show
        if (show) deviceBridge.vibrateDevice(50, false)
    }

    fun openDeveloperUrl(url: String) {
        deviceBridge.openWebsite(url, _selectedLanguage.value == "bn")
    }

    fun copyDeveloperText(text: String, label: String = "Developer Info"): Boolean {
        return deviceBridge.copyToClipboard(label, text)
    }

    fun isAccessibilityEnabled(): Boolean {
        return com.example.service.JarvisAccessibilityService.isRunning()
    }

    fun openAccessibilitySettings() {
        deviceBridge.openAccessibilitySettings()
    }

    fun setVoice(voiceId: String) {
        _selectedVoice.value = voiceId
    }

    fun setModel(modelId: String) {
        _selectedModel.value = modelId
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
        liveSession.currentLanguage = lang
        _sassyQuote.value = getRandomQuote()
        deviceBridge.vibrateDevice(50, lang == "bn")
    }

    fun toggleLanguage() {
        val next = if (_selectedLanguage.value == "bn") "en" else "bn"
        setLanguage(next)
    }

    fun triggerDeviceActionDirectly(toolName: String) {
        val isBn = _selectedLanguage.value == "bn"
        viewModelScope.launch(Dispatchers.IO) {
            when (toolName) {
                "toggleFlashlight" -> {
                    val next = !_isFlashlightOn.value
                    val msg = deviceBridge.toggleFlashlight(next, isBn)
                    addDeviceAction(DeviceActionResult(UUID.randomUUID().toString(), if (isBn) "টর্চ" else "Flashlight", msg, true))
                    _isFlashlightOn.value = deviceBridge.isFlashlightActive()
                }
                "getBattery" -> {
                    val msg = deviceBridge.getBatteryStatus(isBn)
                    addDeviceAction(DeviceActionResult(UUID.randomUUID().toString(), if (isBn) "ব্যাটারি" else "Battery", msg, true))
                    _batteryInfo.value = deviceBridge.getBatteryInfo()
                }
                "vibrate" -> {
                    val msg = deviceBridge.vibrateDevice(400, isBn)
                    addDeviceAction(DeviceActionResult(UUID.randomUUID().toString(), if (isBn) "কম্পন" else "Vibrate", msg, true))
                }
                "sassyJoke" -> {
                    val joke = getRandomQuote()
                    _sassyQuote.value = joke
                    deviceBridge.vibrateDevice(100, isBn)
                }
            }
        }
    }

    private fun addDeviceAction(action: DeviceActionResult) {
        val current = _recentActions.value.toMutableList()
        current.add(0, action)
        if (current.size > 5) {
            current.removeLast()
        }
        _recentActions.value = current
    }

    private fun updateHardwareStates() {
        _batteryInfo.value = deviceBridge.getBatteryInfo()
        _isFlashlightOn.value = deviceBridge.isFlashlightActive()
    }

    private fun startTimer() {
        durationJob?.cancel()
        val startTime = SystemClock.elapsedRealtime()
        durationJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                _sessionDurationSeconds.value = (SystemClock.elapsedRealtime() - startTime) / 1000
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        durationJob?.cancel()
        durationJob = null
        _sessionDurationSeconds.value = 0L
    }

    
    fun toggleSentryMode() {
        sentryModeManager.toggleSentry()
    }

    fun disarmSentry() {
        sentryModeManager.disarm()
    }

    fun toggleWakeWord() {
        wakeWordManager.toggleWakeWord()
    }

    fun toggleFloatingBubble() {
        val next = !_isBubbleActive.value
        _isBubbleActive.value = next
        deviceBridge.toggleFloatingBubble(next, _selectedLanguage.value == "bn")
    }

    fun toggleNotificationAnnouncer() {
        val next = !_isNotificationAnnouncerEnabled.value
        _isNotificationAnnouncerEnabled.value = next
        deviceBridge.toggleNotificationAnnouncer(next, _selectedLanguage.value == "bn")
    }

    fun openCameraVision(query: String = "") {
        _visionQuery.value = query
        _visionResult.value = ""
        _showVisionScanner.value = true
        SciFiSoundEffects.playRadarScan()
    }

    fun closeCameraVision() {
        _showVisionScanner.value = false
    }

    fun analyzeCapturedBitmap(bitmap: Bitmap, query: String = "") {
        _isVisionAnalyzing.value = true
        _visionQuery.value = query
        viewModelScope.launch {
            val key = _apiKey.value
            val res = visionManager.analyzeBitmap(bitmap, query, key)
            _visionResult.value = res
            _isVisionAnalyzing.value = false
            addDeviceAction(
                DeviceActionResult(
                    UUID.randomUUID().toString(),
                    "ভিশন স্ক্যান",
                    res.take(60) + "...",
                    true
                )
            )
        }
    }

    fun explainCurrentScreen(query: String = "") {
        val text = JarvisAccessibilityService.readScreenContent()
        if (text.isBlank()) {
            audioStreamer.speakText("অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় নেই অথবা স্ক্রিনে কোনো টেক্সট পাওয়া যায়নি।")
            return
        }
        viewModelScope.launch {
            val key = _apiKey.value
            val res = visionManager.explainScreen(text, query, key)
            addDeviceAction(
                DeviceActionResult(
                    UUID.randomUUID().toString(),
                    "স্ক্রিন ব্যাখ্যা",
                    res.take(60) + "...",
                    true
                )
            )
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        return JarvisAccessibilityService.isConnected
    }

    fun openAccessibilitySettings() {
        deviceBridge.openAccessibilitySettings()
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return deviceBridge.isIgnoringBatteryOptimizations()
    }

    fun openBatteryOptimizationSettings() {
        deviceBridge.openBatteryOptimizationSettings()
    }

    fun openAppDetailsSettings() {
        deviceBridge.openAppDetailsSettings()
    }

    fun openMiuiAutostartSettings() {
        deviceBridge.openMiuiAutostartSettings()
    }

    fun copyDeveloperText(text: String, label: String = "DevInfo") {
        deviceBridge.copyToClipboard(label, text)
    }

    fun openDeveloperUrl(url: String) {
        deviceBridge.openWebsite(url, _selectedLanguage.value == "bn")
    }

    fun testSpeakerAudio() {
        SciFiSoundEffects.playSystemInitialized()
        audioStreamer.speakText("অডিও স্পিকার এবং সাই-ফাই সাউন্ড ইফেক্ট সম্পূর্ণ কার্যকর আছে!")
    }

    override fun onCleared() {
        super.onCleared()
        stopAssistant()
        quoteTickerJob?.cancel()
    }
}
