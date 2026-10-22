package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object SciFiSoundEffects {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSystemInitialized() {
        scope.launch {
            val durationMs = 350
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            val frequencies = intArrayOf(523, 659, 783, 1046) // C - E - G - C arpeggio
            val segmentLen = numSamples / frequencies.size

            for (i in 0 until numSamples) {
                val freqIdx = (i / segmentLen).coerceAtMost(frequencies.size - 1)
                val freq = frequencies[freqIdx]
                val envelope = sin((i % segmentLen).toDouble() / segmentLen * PI)
                val sample = sin(2.0 * PI * i * freq / SAMPLE_RATE) * envelope * 0.4
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playCommandSuccess() {
        scope.launch {
            val durationMs = 180
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 880.0 + progress * 880.0 // 880 -> 1760 Hz
                val envelope = (1.0 - progress) * 0.35
                val sample = sin(2.0 * PI * i * freq / SAMPLE_RATE) * envelope
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playRadarScan() {
        scope.launch {
            val durationMs = 280
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 1200.0 - progress * 600.0 // 1200 -> 600 Hz ping
                val envelope = Math.exp(-progress * 5.0) * 0.4
                val sample = sin(2.0 * PI * i * freq / SAMPLE_RATE) * envelope
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playAlarmWarble() {
        playPoliceNuclearSiren(2500)
    }

    fun playPoliceNuclearSiren(durationMs: Int = 4000) {
        scope.launch {
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Sweeping warble between 600Hz and 1400Hz at 3Hz sweep rate
                val sweep = (sin(2.0 * PI * 3.0 * t) + 1.0) / 2.0
                val freq = 600.0 + sweep * 800.0
                val sample = sin(2.0 * PI * i * freq / SAMPLE_RATE) * 0.65
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(pcm: ShortArray) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = minBufSize.coerceAtLeast(pcm.size * 2)
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcm, 0, pcm.size)
            audioTrack.play()
            audioTrack.setNotificationMarkerPosition(pcm.size)
            audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.stop()
                    track?.release()
                }
                override fun onPeriodicNotification(track: AudioTrack?) {}
            })
        } catch (_: Exception) {}
    }
}