package com.example.wakeword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isWakeWordEnabled = MutableStateFlow(false)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private var isListening = false

    fun startListening() {
        _isWakeWordEnabled.value = true
        mainHandler.post {
            startRecognizer()
        }
    }

    fun stopListening() {
        _isWakeWordEnabled.value = false
        isListening = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer", e)
            }
        }
    }

    fun toggleWakeWord() {
        if (_isWakeWordEnabled.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startRecognizer() {
        if (!_isWakeWordEnabled.value) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "SpeechRecognizer not available on device")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        // Automatically restart listening if still enabled
                        if (_isWakeWordEnabled.value) {
                            mainHandler.postDelayed({ startRecognizer() }, 800)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (checkMatches(matches)) {
                            onWakeWordDetected()
                        }
                        if (_isWakeWordEnabled.value) {
                            mainHandler.postDelayed({ startRecognizer() }, 500)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (checkMatches(matches)) {
                            onWakeWordDetected()
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognizer", e)
        }
    }

    companion object {
        @Volatile var lastHeard: String = ""
    }

    private fun checkMatches(matches: List<String>?): Boolean {
        if (matches == null) return false
        lastHeard = matches.joinToString(" ").lowercase()
        for (m in matches) {
            val lower = m.lowercase()
            if (lower.contains("alina") || lower.contains("hey alina") ||
                lower.contains("jarvis") || lower.contains("hey jarvis") ||
                lower.contains("আলিনা") || lower.contains("ব্লিভার") || lower.contains("blever")
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "WakeWordManager"
    }
}