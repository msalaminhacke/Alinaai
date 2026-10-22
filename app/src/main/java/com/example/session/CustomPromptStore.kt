package com.example.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AssistantCharacter

/**
 * Stores user-written system prompts per character.
 * A missing or blank entry means the built-in default prompt is used.
 */
object CustomPromptStore {
    private const val PREFS_NAME = "custom_prompt_prefs"
    private var prefs: SharedPreferences? = null
    private val cache = mutableMapOf<String, String>()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        cache.clear()
        AssistantCharacter.values().forEach { c ->
            p.getString(c.id, null)?.takeIf { it.isNotBlank() }?.let { cache[c.id] = it }
        }
    }

    @Synchronized
    fun get(character: AssistantCharacter): String? = cache[character.id]

    @Synchronized
    fun set(character: AssistantCharacter, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            reset(character)
            return
        }
        cache[character.id] = trimmed
        prefs?.edit()?.putString(character.id, trimmed)?.apply()
    }

    @Synchronized
    fun reset(character: AssistantCharacter) {
        cache.remove(character.id)
        prefs?.edit()?.remove(character.id)?.apply()
    }
}
