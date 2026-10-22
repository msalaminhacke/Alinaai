package com.example.routine

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.util.Log
import com.example.bridge.AndroidDeviceBridge
import com.example.memory.JarvisMemoryManager

class JarvisAutomationManager(
    private val context: Context,
    private val deviceBridge: AndroidDeviceBridge,
    private val memoryManager: JarvisMemoryManager
) {

    fun runRoutine(routineName: String): String {
        val r = routineName.trim().lowercase()
        return when {
            r.contains("morning") || r.contains("সকাল") -> {
                deviceBridge.toggleFlashlight(false)
                "শুভ সকাল আলামিন স্যার! আজকের দিনটি সুন্দর কাটুক।"
            }
            r.contains("sleep") || r.contains("bed") || r.contains("রাত") -> {
                toggleBluetooth(false)
                deviceBridge.toggleFlashlight(false)
                "শুভ রাত্রি আলামিন স্যার! আপনার ফোনে স্লিপ মোড সক্রিয় করা হয়েছে।"
            }
            r.contains("focus") || r.contains("study") || r.contains("পড়া") -> {
                "ফোকাস মোড সক্রিয় করা হয়েছে। কোনো ডিস্টার্বেন্স ছাড়াই আপনার পড়াশোনা বা কাজ চালিয়ে যান।"
            }
            else -> {
                "রুটিন $routineName প্রক্রিয়াধীন রয়েছে।"
            }
        }
    }

    fun searchContactNumber(name: String): String? {
        return try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    return it.getString(numIdx)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error searching contact", e)
            null
        }
    }

    fun makePhoneCall(nameOrNumber: String): Boolean {
        val number = if (nameOrNumber.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            nameOrNumber.trim()
        } else {
            searchContactNumber(nameOrNumber) ?: nameOrNumber
        }
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number))).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error making call, trying dialer fallback", e)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Dialer fallback failed", ex)
                false
            }
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "Alina Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm", e)
            false
        }
    }

    fun setTimer(seconds: Int, label: String = "Alina Timer"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting timer", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleWifi(enable: Boolean): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiManager?.setWifiEnabled(enable) ?: false
            } else {
                val intent = Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling wifi", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleBluetooth(enable: Boolean): Boolean {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
            if (adapter != null) {
                if (enable) {
                    @Suppress("DEPRECATION")
                    adapter.enable()
                } else {
                    @Suppress("DEPRECATION")
                    adapter.disable()
                }
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling bluetooth", e)
            false
        }
    }

    companion object {
        private const val TAG = "JarvisAutomationManager"
    }
}