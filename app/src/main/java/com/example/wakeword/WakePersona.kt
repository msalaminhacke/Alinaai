package com.example.wakeword

import com.example.data.model.AssistantCharacter

/** Picks the persona named in a wake phrase ("hey alina", "hello jarvis"). Returns null if none is named. */
object WakePersona {
    fun fromPhrase(heard: String): AssistantCharacter? {
        val h = heard.lowercase()
        return when {
            h.contains("alina") || h.contains("আলিনা") -> AssistantCharacter.ALINA
            h.contains("blever") || h.contains("ব্লিভার") -> AssistantCharacter.BLEVER
            h.contains("jarvis") || h.contains("জার্ভিস") -> AssistantCharacter.JARVIS
            else -> null
        }
    }
}
