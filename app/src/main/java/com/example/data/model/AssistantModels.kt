package com.example.data.model

enum class AssistantState {
    IDLE,        // Sleeping / Waiting for tap or start
    CONNECTING,  // Establishing WebSocket Live connection to Gemini
    LISTENING,   // Streaming microphone PCM16 to model
    SPEAKING,    // Model audio response is actively playing
    ERROR        // Connection or permission issue
}

data class VoiceOption(
    val id: String,
    val name: String,
    val description: String,
    val isFemale: Boolean = true
)

val AvailableVoices = listOf(
    VoiceOption("Aoede", "Aoede", "Vibrant, playful & expressive (Recommended)"),
    VoiceOption("Kore", "Kore", "Warm, sultry & confident"),
    VoiceOption("Puck", "Puck", "High-energy, cheeky & bold"),
    VoiceOption("Fenrir", "Fenrir", "Deep, resonant & charismatic")
)

data class DeviceActionResult(
    val id: String,
    val toolName: String,
    val message: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val statusText: String
)

enum class AssistantCharacter(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val gender: String,
    val defaultVoice: String,
    val subtitle: String,
    val descriptionBn: String
) {
    ALINA(
        id = "alina",
        nameEn = "Alina",
        nameBn = "আলিনা",
        gender = "girl",
        defaultVoice = "Aoede",
        subtitle = "Smart, cute girl friend",
        descriptionBn = "স্মার্ট, কিউট ও মিষ্টি মেয়ে বন্ধু"
    ),
    BLEVER(
        id = "blever",
        nameEn = "Blever",
        nameBn = "ব্লিভার",
        gender = "boy",
        defaultVoice = "Fenrir",
        subtitle = "Smart, cool boy friend",
        descriptionBn = "স্মার্ট, কুল ও সাহসী ছেলে বন্ধু"
    ),
    JARVIS(
        id = "jarvis",
        nameEn = "Jarvis",
        nameBn = "জার্ভিস",
        gender = "neutral",
        defaultVoice = "Kore",
        subtitle = "Calm, precise, butler-style AI",
        descriptionBn = "শান্ত, নির্ভুল ও বুদ্ধিদীপ্ত সহকারী"
    )
}

