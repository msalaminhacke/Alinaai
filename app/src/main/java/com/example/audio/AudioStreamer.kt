package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.sqrt

class AudioStreamer {

    companion object {
        const val SAMPLE_RATE_IN = 16000
        const val SAMPLE_RATE_OUT = 24000
        private const val CHUNK_SIZE = 1024 // PCM 16-bit mono chunks
        private const val TAG = "AudioStreamer"
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private val audioQueue = LinkedBlockingQueue<ByteArray>()

    @Volatile
    var isRecording = false
        private set

    @Volatile
    var isPlaying = false
        private set

    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        onPcmChunk: (ByteArray) -> Unit,
        onAmplitude: (Float) -> Unit
    ): Boolean {
        try {
            stopRecording()

            val minBufSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSize = (minBufSize * 2).coerceAtLeast(CHUNK_SIZE * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(CHUNK_SIZE * 2)
                while (isActive && isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        onPcmChunk(chunk)

                        // Calculate RMS amplitude
                        val amp = calculateRms(chunk)
                        onAmplitude(amp)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            return false
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audioRecord", e)
        }
        audioRecord = null
    }

    fun initPlayback(scope: CoroutineScope, onSpeakerAmplitude: (Float) -> Unit) {
        try {
            stopPlayback()

            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE_OUT,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSize = (minBuf * 2).coerceAtLeast(4096)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE_OUT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()
            isPlaying = true
            audioQueue.clear()

            playbackJob = scope.launch(Dispatchers.IO) {
                while (isActive && isPlaying) {
                    try {
                        val chunk = audioQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                        if (chunk != null && chunk.isNotEmpty()) {
                            audioTrack?.write(chunk, 0, chunk.size)
                            val amp = calculateRms(chunk)
                            onSpeakerAmplitude(amp)
                        } else {
                            onSpeakerAmplitude(0f)
                        }
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
        }
    }

    fun enqueueAudioChunk(pcmData: ByteArray) {
        if (isPlaying) {
            audioQueue.offer(pcmData)
        }
    }

    /**
     * Instantly stops playback and flushes buffers when the user interrupts the model.
     */
    fun stopPlaybackAndFlush() {
        audioQueue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Error flushing AudioTrack", e)
        }
    }

    fun stopPlayback() {
        isPlaying = false
        audioQueue.clear()
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }

    fun releaseAll() {
        stopRecording()
        stopPlayback()
    }

    private fun calculateRms(pcmBytes: ByteArray): Float {
        if (pcmBytes.isEmpty()) return 0f
        var sumSquares = 0.0
        val sampleCount = pcmBytes.size / 2
        for (i in 0 until sampleCount) {
            val b1 = pcmBytes[i * 2].toInt() and 0xFF
            val b2 = pcmBytes[i * 2 + 1].toInt()
            val sample = (b2 shl 8) or b1
            sumSquares += (sample.toLong() * sample)
        }
        val rms = sqrt(sumSquares / sampleCount)
        // Normalize roughly between 0f and 1f (32767 is max 16-bit PCM amplitude)
        return (rms / 8000.0).toFloat().coerceIn(0f, 1f)
    }
}
