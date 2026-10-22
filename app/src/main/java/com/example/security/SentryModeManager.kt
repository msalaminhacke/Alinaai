package com.example.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.audio.AudioStreamer
import com.example.audio.SciFiSoundEffects
import com.example.bridge.AndroidDeviceBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class SentryModeManager(
    private val context: Context,
    private val deviceBridge: AndroidDeviceBridge,
    private val audioStreamer: AudioStreamer
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isArmed = MutableStateFlow(false)
    val isArmed: StateFlow<Boolean> = _isArmed.asStateFlow()

    private val _isAlarmTriggered = MutableStateFlow(false)
    val isAlarmTriggered: StateFlow<Boolean> = _isAlarmTriggered.asStateFlow()

    private val _armCountdown = MutableStateFlow(0)
    val armCountdown: StateFlow<Int> = _armCountdown.asStateFlow()

    private var baselineMagnitude: Float = 9.8f
    private var isCalibrated: Boolean = false
    private var alarmJob: Job? = null

    fun arm(countdownSeconds: Int = 5, onArmed: ((Boolean) -> Unit)? = null) {
        if (_isArmed.value) return
        scope.launch {
            for (i in countdownSeconds downTo 1) {
                _armCountdown.value = i
                SciFiSoundEffects.playRadarScan()
                delay(1000)
            }
            _armCountdown.value = 0
            isCalibrated = false
            accelerometer?.let {
                sensorManager?.registerListener(
                    this@SentryModeManager,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            _isArmed.value = true
            SciFiSoundEffects.playSystemInitialized()
            audioStreamer.speakText("আলামিন স্যার, সেন্ট্রি মোড সম্পূর্ণ সক্রিয় হয়েছে। কোনো মুভমেন্ট হলে অ্যালার্ম বাজবে।")
            onArmed?.invoke(true)
        }
    }

    fun disarm() {
        _isArmed.value = false
        _isAlarmTriggered.value = false
        _armCountdown.value = 0
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        stopAlarm()
        SciFiSoundEffects.playCommandSuccess()
        audioStreamer.speakText("সেন্ট্রি মোড বন্ধ করা হয়েছে।")
    }

    fun toggleSentry() {
        if (_isArmed.value) {
            disarm()
        } else {
            arm()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!_isArmed.value || _isAlarmTriggered.value || event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        if (!isCalibrated) {
            baselineMagnitude = magnitude
            isCalibrated = true
            return
        }

        val delta = Math.abs(magnitude - baselineMagnitude)
        if (delta > MOVEMENT_THRESHOLD) {
            Log.w(TAG, "Movement detected! Delta: ")
            triggerAlarm()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun triggerAlarm() {
        if (_isAlarmTriggered.value) return
        _isAlarmTriggered.value = true

        alarmJob?.cancel()
        alarmJob = scope.launch(Dispatchers.Default) {
            var toggle = false
            while (isActive && _isAlarmTriggered.value) {
                SciFiSoundEffects.playPoliceNuclearSiren(3000)
                vibrateStrong()
                toggle = !toggle
                deviceBridge.toggleFlashlight(toggle)
                delay(3200)
            }
        }
    }

    fun stopAlarm() {
        _isAlarmTriggered.value = false
        alarmJob?.cancel()
        alarmJob = null
        deviceBridge.toggleFlashlight(false)
    }

    private fun vibrateStrong() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 400, 200, 400, 200, 600)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(1000)
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "SentryModeManager"
        private const val MOVEMENT_THRESHOLD = 2.4f
        private const val CALIBRATION_DELAY_MS = 1000L
    }
}