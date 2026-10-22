package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import java.util.Locale

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Incoming call state: , number: ")

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                isCallRinging = true
                lastCallerNumber = incomingNumber ?: "Unknown"
                val resolvedName = if (!incomingNumber.isNullOrBlank()) {
                    resolveContactName(context, incomingNumber)
                } else {
                    "অপরিচিত কলার"
                }
                lastCallerName = resolvedName

                if (isAnnouncerEnabled) {
                    if (tts == null) {
                        initTts(context.applicationContext) {
                            announceCall(resolvedName, incomingNumber)
                        }
                    } else {
                        announceCall(resolvedName, incomingNumber)
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK,
            TelephonyManager.EXTRA_STATE_IDLE -> {
                isCallRinging = false
            }
        }
    }

    private fun resolveContactName(context: Context, number: String): String {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    return it.getString(nameIdx) ?: number
                }
            }
            number
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact name", e)
            number
        }
    }

    private fun announceCall(name: String, number: String?) {
        val speech = if (name != number && name.isNotBlank()) {
            "আলামিন স্যার,  আপনার ফোনে কল দিচ্ছেন!"
        } else {
            "আলামিন স্যার, একটি নতুন ইনকামিং কল আসছে!  নম্বর থেকে ফোন আসছে!"
        }
        tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "call_" + System.currentTimeMillis())
    }

    companion object {
        private const val TAG = "IncomingCallReceiver"
        var isAnnouncerEnabled: Boolean = true
        var isCallRinging: Boolean = false
        var lastCallerName: String? = null
        var lastCallerNumber: String? = null
        private var tts: TextToSpeech? = null
        private var isTtsReady: Boolean = false

        fun initTts(context: Context, onReady: (() -> Unit)? = null) {
            if (tts != null && isTtsReady) {
                onReady?.invoke()
                return
            }
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    try {
                        tts?.language = Locale("bn", "BD")
                    } catch (_: Exception) {}
                    onReady?.invoke()
                }
            }
        }

        fun answerCall(context: Context): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    telecom?.acceptRingingCall()
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error answering call", e)
                false
            }
        }

        fun endCall(context: Context): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    telecom?.endCall()
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
                false
            }
        }
    }
}