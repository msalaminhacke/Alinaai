package com.example.memory

import android.content.Context
import android.content.SharedPreferences

class JarvisMemoryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_memory_prefs", Context.MODE_PRIVATE)

    fun saveFact(key: String, value: String) {
        val cleanKey = key.trim().lowercase()
        prefs.edit().putString(cleanKey, value.trim()).apply()
    }

    fun getFact(key: String): String? {
        val cleanKey = key.trim().lowercase()
        return prefs.getString(cleanKey, null)
    }

    fun deleteFact(key: String) {
        val cleanKey = key.trim().lowercase()
        prefs.edit().remove(cleanKey).apply()
    }

    fun getAllFactsSummary(): String {
        val all = prefs.all
        if (all.isEmpty()) return "No saved memories or facts yet."
        return all.entries.joinToString("
") { (k, v) -> "- : " }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}