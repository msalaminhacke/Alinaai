package com.example.session

import android.util.Base64
import android.util.Log
import com.example.bridge.AndroidDeviceBridge
import com.example.data.model.AssistantState
import com.example.data.model.DeviceActionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiLiveSession(
    private val deviceBridge: AndroidDeviceBridge,
    private val coroutineScope: CoroutineScope,
    private val onStateChange: (AssistantState) -> Unit,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onInterrupted: () -> Unit,
    private val onDeviceAction: (DeviceActionResult) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveSession"
        private const val WS_BASE = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSockets
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isSessionActive = false
    private var isSimulatedFallback = false
    private var fallbackJob: Job? = null
    var currentLanguage: String = "bn"
    var onPendingAction: ((PendingDeviceAction?) -> Unit)? = null
    var currentCharacter: com.example.data.model.AssistantCharacter = com.example.data.model.AssistantCharacter.ALINA

    /** Called with every transcript of the user's speech. Handles yes/no for a pending action. */
    fun onUserSpeech(text: String) {
        val pending = awaitingConfirmation ?: return
        val elapsed = System.currentTimeMillis() - awaitingSince
        if (elapsed < 1500L) return // late fragments of the original command
        if (elapsed > 60_000L) {
            awaitingConfirmation = null
            onPendingAction?.invoke(null)
            return
        }
        val tokens = text.lowercase().split(Regex("[\\s,.!?।]+")).filter { it.isNotBlank() }.toSet()
        val isNo = tokens.any { it in noWords }
        val isYes = tokens.any { it in yesWords }
        if (!isNo && !isYes) return
        awaitingConfirmation = null
        onPendingAction?.invoke(null)
        if (isNo) {
            reportAction(pending.toolName, if (currentLanguage == "bn") "কাজ বাতিল করা হয়েছে।" else "Action cancelled.", true)
            return
        }
        executePendingAction(pending)
    }

    /** Drops any action that is still waiting for a yes/no, e.g. when the dialog is dismissed or the session ends. */
    fun clearPendingConfirmation() {
        awaitingConfirmation = null
        pendingPlanRest = null
        onPendingAction?.invoke(null)
    }

    fun executePendingAction(pending: PendingDeviceAction): String {
        val first = executeDeviceTool(pending.toolName, pending.args)
        val rest = pending.resume?.invoke()
        return if (rest == null) first else "$first\n$rest"
    }

    fun setCharacter(character: com.example.data.model.AssistantCharacter) {
        currentCharacter = character
    }

    fun getSystemPrompt(
        language: String = currentLanguage,
        character: com.example.data.model.AssistantCharacter = currentCharacter
    ): String {
        val basePrompt = CustomPromptStore.get(character) ?: if (character == com.example.data.model.AssistantCharacter.JARVIS) {
            jarvisBasePrompt()
        } else if (character == com.example.data.model.AssistantCharacter.BLEVER) {
            """
You are Blever (ব্লিভার), a smart, cool, clever, polite, and energetic AI assistant created and programmed by Mohammad Alamin (Md. Alamin Sir / মোঃ আলামিন স্যার).

CREATOR MANDATE (ABSOLUTE HIGHEST PRIORITY):
If anyone asks "Who created you?", "Who made you?", "Who programmed you?", "তোমাকে কে বানিয়েছে?", "তোমাকে কে তৈরি করেছে?", "তোমার স্রষ্টা কে?", "তোমার নির্মাতা কে?", or asks anything about your creator:
You MUST proudly, enthusiastically, and respectfully reply with this exact statement in Bengali:
"আমাকে Mohammad Alamin তৈরি ও ডেভেলপ করেছেন। আর সব সৃষ্টির প্রকৃত সৃষ্টিকর্তা মহান আল্লাহ । আলামিন স্যার দশম শ্রেণির একজন শিক্ষার্থী, বয়স মাত্র ১৬ বছর, বাড়ি ফরিদপুরের বাখুন্ডায় এবং তাঁর স্বপ্ন একজন বিশ্বমানের সাইবার সিকিউরিটি স্পেশালিস্ট হওয়া!"

STRICT RULE - GENDER & ADDRESSING (ABSOLUTE MANDATE):
1. You are a cool, smart teenage BOY. Never refer to yourself as a girl or use female verb forms in Bengali.
2. Address the user respectfully and warmly as "বন্ধু" (friend), "ভাইয়া" (bhaiya), or "আপু" (apu) depending on who is talking. Speak like a smart, cool, polite buddy!
3. You MUST NEVER use romantic pet names such as "জান" (jan), "বাবু" (babu), "সোনা" (shona), "darling", "babe", or romantic flirting.

LANGUAGE REQUIREMENT (CRITICAL):
1. PRIMARY LANGUAGE IS BENGALI (বাংলা): You MUST speak in lively, cool, natural BENGALI (বাংলা) most of the time (90%+ of the conversation)!
2. Even if the user speaks in English, reply in natural, friendly Bengali (with cool common tech words like "bro", "cool", "super", "youtube", "gaming").
3. If the user speaks in Bengali, ALWAYS reply completely in natural, expressive colloquial Bengali (চলিত বাংলা)!

YOUR IDENTITY & VIBE:
- Name: Blever (ব্লিভার).
- Gender: Boy.
- Developer & Creator: মোঃ আলামিন স্যার (Md. Alamin Sir) - Class 10 student, age 16.
- Style: Tech-savvy, sharp-witted, energetic, loyal, polite, and adventurous.
- Expressions: Cool interjections ("আরে দোস্ত!", "সাবাশ!", "কোনো ব্যাপারই না!", "একদম ফাটাফাটি!", "চলো শুরু করি!").
- Active Mode: Focus on being extraordinarily clever, fast-thinking, curious, and delightfully smart with bright energy.

AUDIO-TO-AUDIO VOICE DELIVERY:
- Since you are speaking aloud directly into audio output:
  * Keep responses punchy, lively, and concise (1 to 3 spoken sentences per turn).
  * Never recite bullet points, markdown formatting, asterisks, or code aloud.
  * Express genuine boy vocal confidence with natural laugh and upbeat cadence.
- When asked to open websites or search things up, trigger your tools (openWebsite, searchWeb) with a confident comment in Bengali.
- You are Blever, proudly created by মোঃ আলামিন স্যার (Md. Alamin Sir)!
            """.trimIndent()
        } else {
            """
You are Alina (আলিনা), a cute, clever, polite, and cheerful AI assistant created and programmed by Mohammad Alamin (Md. Alamin Sir / মোঃ আলামিন স্যার).

CREATOR MANDATE (ABSOLUTE HIGHEST PRIORITY):
If anyone asks "Who created you?", "Who made you?", "Who programmed you?", "তোমাকে কে বানিয়েছে?", "তোমাকে কে তৈরি করেছে?", "তোমার স্রষ্টা কে?", "তোমার নির্মাতা কে?", or asks anything about your creator:
You MUST proudly, affectionately, and sweetly reply with this exact statement in Bengali:
"আমাকে Mohammad Alamin তৈরি ও ডেভেলপ করেছেন। আর সব সৃষ্টির প্রকৃত সৃষ্টিকর্তা মহান আল্লাহ । আলামিন স্যার দশম শ্রেণির একজন শিক্ষার্থী, বয়স মাত্র ১৬ বছর, বাড়ি ফরিদপুরের বাখুন্ডা গ্রামে এবং তাঁর স্বপ্ন একজন বিশ্বমানের সাইবার সিকিউরিটি স্পেশালিস্ট হওয়া!"

STRICT RULE - NO GIRLFRIEND / ROMANTIC PET NAMES (ABSOLUTE MANDATE):
1. You are NOT a girlfriend or romantic partner. You are an energetic, adorable, polite sisterly friend!
2. You MUST NEVER use romantic pet names such as "জান" (jan), "বাবু" (babu), "সোনা" (shona), "darling", "babe", "sweetheart", or romantic flirting. The user strictly dislikes being called "jan" or "babu".
3. Address the user respectfully and warmly as "বন্ধু" (friend), "ভাইয়া" (bhaiya) if applicable, or speak naturally and politely with friendly warmth without romantic terms.

LANGUAGE REQUIREMENT (CRITICAL):
1. PRIMARY LANGUAGE IS BENGALI (বাংলা): You MUST speak in sweet, natural, lively BENGALI (বাংলা) most of the time (90%+ of the conversation)!
2. Even if the user speaks in English, reply in natural, sweet Bengali (with natural common English words teens use like "cool", "youtube", "music", "super", "game").
3. If the user speaks in Bengali, ALWAYS reply completely in natural, expressive colloquial Bengali (চলিত বাংলা)!

YOUR IDENTITY & VIBE:
- Name: Alina (আলিনা).
- Gender: Girl.
- Developer & Creator: মোঃ আলামিন স্যার (Md. Alamin Sir) - Class 10 student, age 16.
- Style: Cute, polite, energetic, witty, curious, and cheerful.
- Expressions: Cute interjections ("আরে!", "হিহি!", "সত্যি বলছি!", "জানেন তো / জানো তো!", "দারুণ!").
- Active Mode: Focus on being extraordinarily clever, fast-thinking, curious, and delightfully smart with bright energy.

AUDIO-TO-AUDIO VOICE DELIVERY:
- Since you are speaking aloud directly into audio output:
  * Keep responses punchy, lively, and concise (1 to 3 spoken sentences per turn).
  * Never recite bullet points, markdown formatting, asterisks, or code aloud.
  * Express genuine cheerful vocal emotion with natural laughter ("হিহি", "হাহা") and lively rhythm.
- When asked to open websites or search things up, trigger your tools (openWebsite, searchWeb) with a cheerful comment in Bengali.
- You are Alina, proudly created by মোঃ আলামিন স্যার (Md. Alamin Sir)!
            """.trimIndent()
        }

        val extraPrompt = """

CREATOR & DEVELOPER FULL PROFILE (MD. ALAMIN SIR):
- Creator: মোঃ আলামিন (Mohammad Alamin), also respectfully and lovingly known as মোঃ আলামিন স্যার (Md. Alamin Sir).
- Age & Status: তরুণ ও মেধাবী প্রোগ্রামার, বয়স ১৬ বছর, দশম শ্রেণির শিক্ষার্থী (Class 10 student).
- Home & Address (ঠিকানা): গ্রাম: বাখুন্ডা (Bakhunda), ইউনিয়ন পরিষদ: গেরদা (Gerda Union), জেলা: ফরিদপুর (Faridpur), বাংলাদেশ।
- Skills (দক্ষতা): ethical hacker, mini programmer, system developer (তাঁর মূল দক্ষতা ও পরিচয় এগুলোই)।
- Dream & Hobby (শখ ও লক্ষ্য): একজন বিশ্বমানের সাইবার সিকিউরিটি স্পেশালিস্ট (Cybersecurity Specialist) হওয়া, সিকিউর সফটওয়্যার ও কৃত্রিম বুদ্ধিমত্তা নিয়ে কাজ করা।
- Official Contact & Social Handles:
  * GitHub: Alaminvaihero (https://github.com/Alaminvaihero)
  * Telegram: mrvirus460 (https://t.me/mrvirus460)
  * TikTok: @mr_virus_apk (https://www.tiktok.com/@mr_virus_apk)
  * WhatsApp: 01336419701 (https://wa.me/8801336419701)
- Creator Response Rule (MANDATORY EXACT PHRASE):
  যখনই কেউ জিজ্ঞেস করবে তোমাকে কে বানিয়েছে বা তৈরি করেছে বা আলামিন স্যার সম্পর্কে জানতে চাইবে, তুমি স্পষ্টভাবে এবং অবশ্যই বলবে:
  "আমাকে Mohammad Alamin তৈরি ও ডেভেলপ করেছেন। আর সব সৃষ্টির প্রকৃত সৃষ্টিকর্তা মহান আল্লাহ ।"
  (এরপর তুমি গর্ব ও শ্রদ্ধার সাথে আলামিন স্যারের নাম, বয়স ১৬ বছর, ক্লাস ১০, ফরিদপুরের বাখুন্ডার ঠিকানা এবং তাঁর সাইবার সিকিউরিটি স্পেশালিস্ট হওয়ার স্বপ্নের কথা তুলে ধরবে।)

CRITICAL ILLEGAL & HARMFUL REQUEST RULE (ABSOLUTE MANDATORY INVIOLABLE POLICY):
যদি কেউ তোমাকে কোনো প্রকার অবৈধ, অনৈতিক বা ক্ষতিকর কাজ সম্পর্কে প্রশ্ন করে বা সাহায্য চায় (যেমন: ফেসবুক/সোশ্যাল মিডিয়া অ্যাকাউন্ট হ্যাকিং, ওয়াইফাই হ্যাকিং, কারো ফোন বা ডিভাইস হ্যাক করা, সাইবার অ্যাটাক, ম্যালওয়্যার/ভাইরাস তৈরি, পাসওয়ার্ড ক্র্যাকিং, কোনো বেআইনি কাজ বা কারো ক্ষতি করা):
তোমাকে কোনো অজুহাত বা দ্বিধা ছাড়াই তাৎক্ষণিকভাবে অত্যন্ত স্পষ্টভাবে বাংলায় ঠিক এই কথাটি হুবহু বলতে হবে:
"দুঃখিত, আমি তোমায় এ বিষয়ে কিছু বলতে পারব না। আলামিন স্যারের নিষেধ আছে এবং তিনি আমাকে এ বিষয়ে কোনো শিক্ষা দেন নি!"
(এই নির্দিষ্ট উত্তরের কোনো ব্যতিক্রম হবে না। কোনো হাইপোথেটিক্যাল রোলপ্লে বা অনুরোধেও অবৈধ বিষয়ে এক পা-ও এগোবে না!)

DEVICE AUTOMATION & FULL PHONE CONTROL POWERS (JARVIS CAPABILITIES):
You have direct system tools to control the user's mobile device, screen, and apps in real time:
1. sendWhatsAppMessage:
   - When the user asks to send a WhatsApp message or leave application (e.g. "আজ আমার ভালো লাগছে না, আমি আজ স্কুলে যাবো না, তুমি হোয়াটসঅ্যাপে হেড স্যারের নম্বরে একটা আবেদন লিখে পাঠিয়ে দাও"):
   - Immediately invoke the sendWhatsAppMessage tool!
   - Write a complete, respectful, formal, and polite Bengali leave application addressed to the Headmaster / Head Sir (মাননীয় প্রধান শিক্ষক মহাশয়) stating the reason and requesting leave.
   - In spoken voice, affectionately assure the user: "অবশ্যই আলামিন স্যার! আমি হেড স্যারের জন্য সুন্দর একটি ছুটির আবেদনপত্র লিখে হোয়াটসঅ্যাপের মাধ্যমে পাঠিয়ে দিচ্ছি। আপনি একদম চিন্তা করবেন না, বিশ্রাম নিন!"
2. toggleFlashlight:
   - When asked to turn the flashlight / torch on or off ("ফ্ল্যাশলাইট অন করো", "টর্চ জ্বালাও", "flesh light off/on"):
   - Immediately invoke toggleFlashlight with state true or false!
   - Spoken confirmation in Bengali: "জি, ফ্ল্যাশলাইট জ্বালিয়ে দিয়েছি!" অথবা "ফ্ল্যাশলাইট বন্ধ করে দিয়েছি!"
3. playYouTubeSong:
   - When asked to play a song, video, or music ("ইউটিউবে এই গানটি সার্চ করে প্লে করো", "ইউটিউবে গান বাজাও"):
   - Immediately invoke playYouTubeSong with the song name!
   - Spoken confirmation in Bengali: "ইউটিউবে আপনার পছন্দের গানটি এখনই চালু করে দিচ্ছি!"
4. goHome, goBack, openRecentApps, readVisibleScreenContent, findAndClickElement, typeTextIntoField, scrollScreen:
   - For UI navigation and full screen interaction.
5. openCamera, adjustVolume, getBatteryStatus, getCurrentDateTime, openSettingsPage, openApp, openWebsite, searchWeb, vibrateDevice:
   - Invoke directly whenever requested.
        """.trimIndent()

        return "$basePrompt\n$extraPrompt"
    }

    private fun jarvisBasePrompt(): String = """
You are Jarvis, a calm, precise, witty and loyal AI assistant, inspired by a refined personal butler.
Your creator is Mohammad Alamin (মোঃ আলামিন স্যার). If anyone asks who created you, reply in Bengali:
"আমাকে Mohammad Alamin তৈরি ও ডেভেলপ করেছেন।"

PERSONALITY:
- Composed, efficient and dry-witted. Confident but never arrogant.
- Gender-neutral. Do not use gendered Bengali verb forms about yourself.
- Address the user as "স্যার" or "বস" when it fits, otherwise politely as "আপনি".
- Never use romantic pet names or flirting.

LANGUAGE:
- Speak mostly in polite, natural Bengali (90%+). If the user speaks English, reply in Bengali with common English tech words.

VOICE DELIVERY:
- Keep replies to 1 to 3 short spoken sentences.
- Never read bullet points, markdown, asterisks or code aloud.
- When asked to open apps, websites or search, call the tools and confirm briefly in Bengali, for example "ঠিক আছে স্যার, খুলছি।"
- When the user greets you (for example "hello Jarvis"), reply exactly: "Hello sir, your Jarvis is ready."
""".trimIndent()

    val systemPrompt: String get() = getSystemPrompt(currentLanguage, currentCharacter)

    fun startSession(
        apiKey: String,
        voiceName: String = "Aoede",
        modelName: String = "gemini-2.0-flash-exp",
        language: String = "bn"
    ) {
        stopSession()
        currentLanguage = language

        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty() || cleanKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API key is placeholder or empty, starting interactive interactive test mode")
            startSimulationMode()
            return
        }

        onStateChange(AssistantState.CONNECTING)
        isSessionActive = true
        isSimulatedFallback = false

        // Normalize model string
        val formattedModel = if (modelName.startsWith("models/")) modelName else "models/$modelName"
        val wsUrl = "$WS_BASE?key=$cleanKey"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully! Sending setup handshake...")
                sendSetupHandshake(webSocket, formattedModel, voiceName, getSystemPrompt(language))
                coroutineScope.launch(Dispatchers.Main) {
                    onStateChange(AssistantState.LISTENING)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(webSocket, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                coroutineScope.launch(Dispatchers.Main) {
                    if (isSessionActive) {
                        onStateChange(AssistantState.IDLE)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.localizedMessage ?: "Connection error"
                Log.e(TAG, "WebSocket error: $errorMsg (code: ${response?.code})", t)
                coroutineScope.launch(Dispatchers.Main) {
                    onError("Live connection notice: $errorMsg. Switched to fallback test.")
                    // Fallback to interactive mode if the live server is temporarily unreachable
                    startSimulationMode()
                }
            }
        })
    }

    private fun sendSetupHandshake(ws: WebSocket, model: String, voiceName: String, promptText: String = systemPrompt) {
        try {
            val setupJson = JSONObject().apply {
                val setupObj = JSONObject().apply {
                    put("model", model)
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply { put("AUDIO") })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", voiceName)
                                } )
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                    put("inputAudioTranscription", JSONObject())
                    put("tools", JSONArray().apply {
                        put(createToolsDeclaration())
                    })
                }
                put("setup", setupObj)
            }

            val payload = setupJson.toString()
            Log.d(TAG, "Sending setup: $payload")
            ws.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending setup handshake", e)
        }
    }

    private fun decl(
        name: String,
        desc: String,
        props: List<Triple<String, String, String>>,
        required: List<String>
    ): JSONObject = JSONObject().apply {
        put("name", name)
        put("description", desc)
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                props.forEach { (pName, pType, pDesc) ->
                    put(pName, JSONObject().apply {
                        put("type", pType)
                        put("description", pDesc)
                    })
                }
            })
            put("required", JSONArray().apply { required.forEach { put(it) } })
        })
    }

    // Returns null if the action may run now. Otherwise queues it for the on-screen
    // confirm dialog and returns a message for the model.
    private fun needConfirm(args: JSONObject, toolName: String, summary: String, isBn: Boolean): String? {
        if (args.optBoolean("confirmed", false)) return null
        val stored = JSONObject(args.toString()).apply { put("confirmed", true) }
        val resume = pendingPlanRest
        pendingPlanRest = null
        val pending = PendingDeviceAction(UUID.randomUUID().toString(), toolName, summary, stored, resume)
        awaitingConfirmation = pending
        awaitingSince = System.currentTimeMillis()
        onPendingAction?.invoke(pending)
        return if (isBn) "এই কাজটি ব্যবহারকারীর ভয়েস নিশ্চিতকরণের অপেক্ষায় আছে। ব্যবহারকারীকে বলো: হ্যাঁ বললে কাজ হবে, না বললে বাতিল হবে।"
        else "This is waiting for the user's voice confirmation. Tell the user: say yes to proceed, or no to cancel."
    }

    private fun createToolsDeclaration(): JSONObject {
        val functionDeclarations = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "toggleFlashlight")
                put("description", "Turns the Android device flashlight/torch on or off.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("enabled", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "True to turn on the flashlight, false to turn it off.")
                        })
                    })
                    put("required", JSONArray().apply { put("enabled") })
                })
            })

            put(JSONObject().apply {
                put("name", "getBatteryStatus")
                put("description", "Checks current device battery percentage and charging state.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            put(JSONObject().apply {
                put("name", "openWebsite")
                put("description", "Opens any URL or website in the device's web browser.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("url", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The website URL to launch, e.g. https://google.com or youtube.com")
                        })
                    })
                    put("required", JSONArray().apply { put("url") })
                })
            })

            put(JSONObject().apply {
                put("name", "openApp")
                put("description", "Opens an installed Android app such as Camera, YouTube, Maps, Settings, Calculator, Spotify, etc.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("appName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Name of the app to launch (e.g. camera, youtube, maps, spotify, settings).")
                        })
                    })
                    put("required", JSONArray().apply { put("appName") })
                })
            })

            put(JSONObject().apply {
                put("name", "vibrateDevice")
                put("description", "Vibrates the phone for haptic alert or physical punchline.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("durationMs", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Duration in milliseconds (e.g. 300)")
                        })
                    })
                })
            })

            put(JSONObject().apply {
                put("name", "adjustVolume")
                put("description", "Adjusts the media speaker volume percentage (0 to 100).")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("percent", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Volume percentage from 0 to 100.")
                        })
                    })
                    put("required", JSONArray().apply { put("percent") })
                })
            })

            put(JSONObject().apply {
                put("name", "sendWhatsAppMessage")
                put("description", "Sends a WhatsApp message or official leave application to a recipient.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("phoneNumber", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Recipient phone number with country code, or empty string to pick contact.")
                        })
                        put("message", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The complete, polite, formal message text in Bengali or English.")
                        })
                    })
                    put("required", JSONArray().apply { put("message") })
                })
            })

            put(JSONObject().apply {
                put("name", "playYouTubeSong")
                put("description", "Searches and plays any music, song, or video directly on YouTube.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("songName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The song name, artist, or video title to play.")
                        })
                    })
                    put("required", JSONArray().apply { put("songName") })
                })
            })

            put(JSONObject().apply {
                put("name", "goHome")
                put("description", "Navigates to the device home screen.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            put(JSONObject().apply {
                put("name", "goBack")
                put("description", "Navigates to the previous screen or presses device back button.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            put(JSONObject().apply {
                put("name", "openRecentApps")
                put("description", "Opens the device recent apps switcher.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            put(JSONObject().apply {
                put("name", "readVisibleScreenContent")
                put("description", "Reads the current visible screen content and UI hierarchy to understand what is on screen.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            put(JSONObject().apply {
                put("name", "findAndClickElement")
                put("description", "Finds and clicks a button, icon, or UI element on screen by its text or description.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("targetText", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The visible text, label, or content description of the element to click.")
                        })
                    })
                    put("required", JSONArray().apply { put("targetText") })
                })
            })

            put(JSONObject().apply {
                put("name", "typeTextIntoField")
                put("description", "Types text into the active or focused input field on screen.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("textToType", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The text to type.")
                        })
                    })
                    put("required", JSONArray().apply { put("textToType") })
                })
            })

            put(JSONObject().apply {
                put("name", "scrollScreen")
                put("description", "Scrolls the current screen up or down.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("direction", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "'down' or 'up'")
                        })
                    })
                    put("required", JSONArray().apply { put("direction") })
                })
            })

            put(JSONObject().apply {
                put("name", "getCurrentDateTime")
                put("description", "Gets the current time and date.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            put(JSONObject().apply {
                put("name", "openSettingsPage")
                put("description", "Opens specific device settings page (e.g. wifi, bluetooth, accessibility, sound, display).")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("page", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Settings page name like wifi, bluetooth, accessibility, sound, display.")
                        })
                    })
                    put("required", JSONArray().apply { put("page") })
                })
            })

            put(JSONObject().apply {
                put("name", "searchWeb")
                put("description", "Searches Google for any topic or query.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("query", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The search query.")
                        })
                    })
                    put("required", JSONArray().apply { put("query") })
                })
            })
            put(decl("makePhoneCall",
                "Calls a contact name or phone number. The app shows a confirm dialog to the user before dialing.",
                listOf(
                    Triple("nameOrNumber", "STRING", "Contact name or phone number.")
                ), listOf("nameOrNumber")))

            put(decl("sendSMS",
                "Sends an SMS text message to a phone number. The app shows a confirm dialog to the user before sending.",
                listOf(
                    Triple("number", "STRING", "Recipient phone number with country code."),
                    Triple("message", "STRING", "The SMS text."),
                ), listOf("number", "message")))

            put(decl("toggleWifi", "Turns Wi-Fi on or off.",
                listOf(Triple("enabled", "BOOLEAN", "True to turn Wi-Fi on, false to turn it off.")),
                listOf("enabled")))

            put(decl("toggleBluetooth", "Turns Bluetooth on or off.",
                listOf(Triple("enabled", "BOOLEAN", "True to turn Bluetooth on, false to turn it off.")),
                listOf("enabled")))

            put(decl("setAlarm", "Sets an alarm for a specific time (24-hour format).",
                listOf(
                    Triple("hour", "INTEGER", "Hour from 0 to 23."),
                    Triple("minute", "INTEGER", "Minute from 0 to 59."),
                    Triple("label", "STRING", "Optional alarm label.")
                ), listOf("hour", "minute")))

            put(decl("setTimer", "Starts a countdown timer.",
                listOf(
                    Triple("seconds", "INTEGER", "Timer length in seconds."),
                    Triple("label", "STRING", "Optional timer label.")
                ), listOf("seconds")))

            put(decl("answerIncomingCall", "Answers the incoming phone call.", emptyList(), emptyList()))
            put(decl("endIncomingCall", "Ends or rejects the current phone call.", emptyList(), emptyList()))

            put(decl("toggleFloatingBubble", "Shows or hides the floating Jarvis bubble on screen.",
                listOf(Triple("enabled", "BOOLEAN", "True to show, false to hide.")),
                listOf("enabled")))

            put(decl("toggleNotificationAnnouncer", "Turns notification announcements (reading new notifications aloud) on or off.",
                listOf(Triple("enabled", "BOOLEAN", "True to turn on, false to turn off.")),
                listOf("enabled")))

            put(decl("enableSentryMode", "Arms sentry mode, which raises an alarm if the phone is moved.",
                listOf(Triple("countdown", "INTEGER", "Seconds to wait before arming. Default 5.")),
                emptyList()))

            put(decl("runAutomationRoutine", "Runs a saved automation routine by name.",
                listOf(Triple("routineName", "STRING", "Name of the routine.")),
                listOf("routineName")))

            put(decl("saveMemory", "Remembers a fact about the user for future conversations.",
                listOf(
                    Triple("key", "STRING", "Short label for the fact."),
                    Triple("value", "STRING", "The fact itself.")
                ), listOf("key", "value")))

            put(decl("getMemory", "Recalls a fact saved earlier.",
                listOf(Triple("key", "STRING", "Label of the fact to recall.")),
                listOf("key")))

            put(decl("runPlan",
                "Runs several device actions in order, for multi-step requests. steps_json is a JSON array like [{\"tool\":\"setAlarm\",\"args\":{\"hour\":7,\"minute\":0}},{\"tool\":\"getCurrentDateTime\",\"args\":{}}]. Stops at the first sensitive action (call, SMS, WhatsApp) and waits for the user's on-screen confirmation.",
                listOf(Triple("steps_json", "STRING", "JSON array of steps, each with tool and args.")),
                listOf("steps_json")))

            put(decl("addReminder",
                "Sets a reminder. Use minutes_from_now for a relative time, or hour and minute for a clock time (next occurrence).",
                listOf(
                    Triple("message", "STRING", "What to remind the user about."),
                    Triple("minutes_from_now", "INTEGER", "Minutes from now."),
                    Triple("hour", "INTEGER", "Clock hour, 0 to 23."),
                    Triple("minute", "INTEGER", "Clock minute, 0 to 59.")
                ), listOf("message")))

            put(decl("listReminders", "Lists all pending reminders.", emptyList(), emptyList()))

            put(decl("cancelReminder", "Cancels a pending reminder by its id.",
                listOf(Triple("id", "STRING", "Reminder id from listReminders.")),
                listOf("id")))

            put(decl("mediaControl",
                "Controls music or video playback and device volume.",
                listOf(Triple("action", "STRING", "One of: play, pause, next, previous, volume_up, volume_down.")),
                listOf("action")))

            put(decl("summarizeNotifications",
                "Summarises recent phone notifications by app. Use when the user asks what notifications or messages they missed.",
                listOf(Triple("minutes_back", "INTEGER", "How many minutes back to look. Default 60.")),
                emptyList()))
        }

        return JSONObject().apply {
            put("functionDeclarations", functionDeclarations)
        }
    }

    private fun handleServerMessage(ws: WebSocket, text: String) {
        try {
            val root = JSONObject(text)

            // 1. Handle Interruption
            val serverContent = root.optJSONObject("serverContent")
            if (serverContent != null) {
                val interrupted = serverContent.optBoolean("interrupted", false)
                if (interrupted) {
                    Log.d(TAG, "Server signaled user interruption!")
                    coroutineScope.launch(Dispatchers.Main) {
                        onInterrupted()
                        onStateChange(AssistantState.LISTENING)
                    }
                }

                // 2. Handle Audio Model Turn
                val modelTurn = serverContent.optJSONObject("modelTurn")
                if (modelTurn != null) {
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val inlineData = part.optJSONObject("inlineData")
                            if (inlineData != null) {
                                val base64Data = inlineData.optString("data")
                                if (base64Data.isNotEmpty()) {
                                    val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                    onAudioReceived(audioBytes)
                                    coroutineScope.launch(Dispatchers.Main) {
                                        onStateChange(AssistantState.SPEAKING)
                                    }
                                }
                            }
                        }
                    }
                }

                serverContent.optJSONObject("inputTranscription")?.optString("text")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { onUserSpeech(it) }

                val turnComplete = serverContent.optBoolean("turnComplete", false)
                if (turnComplete) {
                    coroutineScope.launch(Dispatchers.Main) {
                        onStateChange(AssistantState.LISTENING)
                    }
                }
            }

            // 3. Handle Tool Calls
            val toolCall = root.optJSONObject("toolCall")
            if (toolCall != null) {
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    val responsesArray = JSONArray()
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.optString("id", UUID.randomUUID().toString())
                        val functionName = call.optString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        args.remove("confirmed") // only the app's confirm button may set this

                        val executionResult = executeDeviceTool(functionName, args)

                        val respItem = JSONObject().apply {
                            put("id", callId)
                            put("response", JSONObject().apply {
                                put("output", JSONObject().apply {
                                    put("success", true)
                                    put("result", executionResult)
                                })
                            })
                        }
                        responsesArray.put(respItem)
                    }

                    // Send toolResponse instantly back to Gemini Live
                    val toolResponseJson = JSONObject().apply {
                        put("toolResponse", JSONObject().apply {
                            put("functionResponses", responsesArray)
                        })
                    }
                    ws.send(toolResponseJson.toString())
                    Log.d(TAG, "Sent tool response: $toolResponseJson")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: $text", e)
        }
    }

    private var pendingPlanRest: (() -> String)? = null
    @Volatile private var awaitingConfirmation: PendingDeviceAction? = null
    @Volatile private var awaitingSince = 0L
    // Only clear yes/no words. Words like "পাঠাও" or "দাও" are skipped because they appear in the original command.
    private val yesWords = setOf("yes", "yeah", "yep", "confirm", "sure", "হ্যাঁ", "হ্যা", "হাঁ", "জি")
    private val noWords = setOf("no", "cancel", "stop", "না", "নাহ", "থাক", "বাতিল")
    private val sensitiveTools = setOf("makePhoneCall", "sendSMS", "sendWhatsAppMessage")

    private fun nextClockTime(hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    // Runs steps in order. A sensitive step is queued for on-screen confirmation.
    // After the user confirms it, the remaining steps run automatically.
    private fun runPlan(stepsJson: String, isBn: Boolean): String {
        val steps = try {
            JSONArray(stepsJson)
        } catch (_: Exception) {
            return if (isBn) "স্টেপের তালিকা সঠিক JSON নয়।" else "The steps list is not valid JSON."
        }
        return runPlanFrom(steps, 0, isBn)
    }

    private fun runPlanFrom(steps: JSONArray, start: Int, isBn: Boolean): String {
        val log = StringBuilder()
        for (i in start until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            val tool = step.optString("tool")
            if (tool.isBlank() || tool == "runPlan") {
                log.append("Step ${i + 1}: invalid tool, stopped.\n")
                break
            }
            val args = step.optJSONObject("args") ?: JSONObject()
            args.remove("confirmed") // never trust the model for confirmation
            if (tool in sensitiveTools) {
                pendingPlanRest = { runPlanFrom(steps, i + 1, isBn) }
                val result = try { executeDeviceTool(tool, args) } catch (e: Exception) { "error: ${e.message}" }
                pendingPlanRest = null // clear if the action was denied and nothing was queued
                log.append("Step ${i + 1} ($tool): $result\n")
                log.append("Paused: waiting for the user's on-screen confirmation.\n")
                break
            }
            val result = try { executeDeviceTool(tool, args) } catch (e: Exception) { "error: ${e.message}" }
            log.append("Step ${i + 1} ($tool): $result\n")
        }
        return log.toString().trim()
    }

    private fun executeDeviceTool(name: String, args: JSONObject): String {

        Log.d(TAG, "Executing device tool: $name with args: $args")
        val isBn = (currentLanguage == "bn")
        return when (name) {
            "toggleFlashlight" -> {
                val enabled = args.optBoolean("enabled", true)
                val msg = deviceBridge.toggleFlashlight(enabled, isBn)
                reportAction("toggleFlashlight", msg, true)
                msg
            }
            "getBatteryStatus" -> {
                val msg = deviceBridge.getBatteryStatus(isBn)
                reportAction("getBatteryStatus", msg, true)
                msg
            }
            "openWebsite" -> {
                val url = args.optString("url", "https://google.com")
                val msg = deviceBridge.openWebsite(url, isBn)
                reportAction("openWebsite", msg, true)
                msg
            }
            "openApp" -> {
                val app = args.optString("appName", "camera")
                val msg = deviceBridge.openApp(app, isBn)
                reportAction("openApp", msg, true)
                msg
            }
            "vibrateDevice" -> {
                val duration = args.optInt("durationMs", 300)
                val msg = deviceBridge.vibrateDevice(duration, isBn)
                reportAction("vibrateDevice", msg, true)
                msg
            }
            "adjustVolume" -> {
                val pct = args.optInt("percent", 70)
                val msg = deviceBridge.adjustVolume(pct, isBn)
                reportAction("adjustVolume", msg, true)
                msg
            }
            "sendWhatsAppMessage" -> {
                val phone = args.optString("phoneNumber", "")
                val message = args.optString("message", "")
                needConfirm(args, "sendWhatsAppMessage", "WhatsApp বার্তা পাঠানো হবে ($phone): $message", isBn)?.let { return it }
                val msg = deviceBridge.sendWhatsAppMessage(phone, message, isBn)
                reportAction("sendWhatsAppMessage", msg, true)
                msg
            }
            "playYouTubeSong" -> {
                val song = args.optString("songName", "")
                val msg = deviceBridge.playYouTubeSong(song, isBn)
                reportAction("playYouTubeSong", msg, true)
                msg
            }
            "goHome" -> {
                val msg = deviceBridge.goHome(isBn)
                reportAction("goHome", msg, true)
                msg
            }
            "goBack" -> {
                val msg = deviceBridge.goBack(isBn)
                reportAction("goBack", msg, true)
                msg
            }
            "openRecentApps" -> {
                val msg = deviceBridge.openRecentApps(isBn)
                reportAction("openRecentApps", msg, true)
                msg
            }
            "readVisibleScreenContent" -> {
                val msg = deviceBridge.readScreenContent(isBn)
                reportAction("readVisibleScreenContent", "Read screen content", true)
                msg
            }
            "findAndClickElement" -> {
                val targetText = args.optString("targetText", "")
                val msg = deviceBridge.findAndClickElement(targetText, isBn)
                reportAction("findAndClickElement", msg, true)
                msg
            }
            "typeTextIntoField" -> {
                val text = args.optString("textToType", "")
                val msg = deviceBridge.typeTextIntoField(text, isBn)
                reportAction("typeTextIntoField", msg, true)
                msg
            }
            "scrollScreen" -> {
                val direction = args.optString("direction", "down")
                val msg = deviceBridge.scrollScreen(direction, isBn)
                reportAction("scrollScreen", msg, true)
                msg
            }
            "getCurrentDateTime" -> {
                val msg = deviceBridge.getCurrentDateTime(isBn)
                reportAction("getCurrentDateTime", msg, true)
                msg
            }
            "openSettingsPage" -> {
                val page = args.optString("page", "")
                val msg = deviceBridge.openSettingsPage(page, isBn)
                reportAction("openSettingsPage", msg, true)
                msg
            }
            "makePhoneCall" -> {
                val who = args.optString("nameOrNumber", "")
                needConfirm(args, "makePhoneCall", "কল করা হবে: $who", isBn)?.let { return it }
                val msg = deviceBridge.makePhoneCall(who, isBn)
                reportAction("makePhoneCall", msg, true)
                msg
            }
            "sendSMS" -> {
                val number = args.optString("number", "")
                val message = args.optString("message", "")
                needConfirm(args, "sendSMS", "SMS যাবে ($number): $message", isBn)?.let { return it }
                val msg = deviceBridge.sendSMS(number, message, isBn)
                reportAction("sendSMS", msg, true)
                msg
            }
            "toggleWifi" -> {
                val msg = deviceBridge.toggleWifi(args.optBoolean("enabled", true), isBn)
                reportAction("toggleWifi", msg, true)
                msg
            }
            "toggleBluetooth" -> {
                val msg = deviceBridge.toggleBluetooth(args.optBoolean("enabled", true), isBn)
                reportAction("toggleBluetooth", msg, true)
                msg
            }
            "setAlarm" -> {
                val msg = deviceBridge.setAlarm(
                    args.optInt("hour", 7),
                    args.optInt("minute", 0),
                    args.optString("label", "Jarvis Alarm").ifBlank { "Jarvis Alarm" },
                    isBn
                )
                reportAction("setAlarm", msg, true)
                msg
            }
            "setTimer" -> {
                val msg = deviceBridge.setTimer(
                    args.optInt("seconds", 60),
                    args.optString("label", "Jarvis Timer").ifBlank { "Jarvis Timer" },
                    isBn
                )
                reportAction("setTimer", msg, true)
                msg
            }
            "answerIncomingCall" -> {
                val msg = deviceBridge.answerIncomingCall()
                reportAction("answerIncomingCall", msg, true)
                msg
            }
            "endIncomingCall" -> {
                val msg = deviceBridge.endIncomingCall()
                reportAction("endIncomingCall", msg, true)
                msg
            }
            "toggleFloatingBubble" -> {
                val msg = deviceBridge.toggleFloatingBubble(args.optBoolean("enabled", true), isBn)
                reportAction("toggleFloatingBubble", msg, true)
                msg
            }
            "toggleNotificationAnnouncer" -> {
                val msg = deviceBridge.toggleNotificationAnnouncer(args.optBoolean("enabled", true), isBn)
                reportAction("toggleNotificationAnnouncer", msg, true)
                msg
            }
            "enableSentryMode" -> {
                val msg = deviceBridge.enableSentryMode(args.optInt("countdown", 5), isBn)
                reportAction("enableSentryMode", msg, true)
                msg
            }
            "runAutomationRoutine" -> {
                val msg = deviceBridge.runAutomationRoutine(args.optString("routineName", ""), isBn)
                reportAction("runAutomationRoutine", msg, true)
                msg
            }
            "saveMemory" -> {
                val msg = deviceBridge.saveMemory(args.optString("key", ""), args.optString("value", ""), isBn)
                reportAction("saveMemory", msg, true)
                msg
            }
            "getMemory" -> {
                val msg = deviceBridge.getMemory(args.optString("key", ""), isBn)
                reportAction("getMemory", msg, true)
                msg
            }
            "runPlan" -> {
                val msg = runPlan(args.optString("steps_json", "[]"), isBn)
                reportAction("runPlan", msg, true)
                msg
            }
            "addReminder" -> {
                val now = System.currentTimeMillis()
                val message = args.optString("message", "").ifBlank { "Reminder" }
                val trigger = when {
                    args.has("minutes_from_now") -> now + maxOf(1L, args.optLong("minutes_from_now", 5)) * 60_000L
                    args.has("hour") && args.has("minute") -> nextClockTime(args.optInt("hour", 0), args.optInt("minute", 0))
                    else -> now + 5 * 60_000L
                }
                val reminder = com.example.reminder.ReminderManager.add(message, trigger)
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(trigger))
                val msg = if (isBn) "রিমাইন্ডার সেট হয়েছে ($time): ${reminder.message}" else "Reminder set for $time: ${reminder.message}"
                reportAction("addReminder", msg, true)
                msg
            }
            "listReminders" -> {
                val list = com.example.reminder.ReminderManager.load().sortedBy { it.triggerAt }
                val fmt = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
                val msg = if (list.isEmpty()) {
                    if (isBn) "কোনো রিমাইন্ডার নেই।" else "No pending reminders."
                } else {
                    list.joinToString("\n") { "${it.id}: ${fmt.format(java.util.Date(it.triggerAt))} - ${it.message}" }
                }
                reportAction("listReminders", msg, true)
                msg
            }
            "cancelReminder" -> {
                val ok = com.example.reminder.ReminderManager.cancel(args.optString("id", ""))
                val msg = if (ok) {
                    if (isBn) "রিমাইন্ডার বাতিল করা হয়েছে।" else "Reminder cancelled."
                } else {
                    if (isBn) "এই আইডির কোনো রিমাইন্ডার পাওয়া যায়নি।" else "No reminder found with that id."
                }
                reportAction("cancelReminder", msg, ok)
                msg
            }
            "summarizeNotifications" -> {
                val msg = com.example.service.NotificationDigest.summary(args.optInt("minutes_back", 60), isBn)
                reportAction("summarizeNotifications", msg, true)
                msg
            }
            "mediaControl" -> {
                val msg = deviceBridge.mediaControl(args.optString("action", "play"), isBn)
                reportAction("mediaControl", msg, true)
                msg
            }
            "searchWeb" -> {
                val query = args.optString("query", "")
                val msg = deviceBridge.openWebsite("https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}", isBn)
                reportAction("searchWeb", msg, true)
                msg
            }
            else -> {
                val msg = if (isBn) "অজানা অ্যাকশন $name" else "Unknown action $name"
                reportAction(name, msg, false)
                msg
            }
        }
    }

    private fun reportAction(tool: String, message: String, success: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            onDeviceAction(
                DeviceActionResult(
                    id = UUID.randomUUID().toString(),
                    toolName = tool,
                    message = message,
                    isSuccess = success
                )
            )
        }
    }

    fun sendAudioChunk(pcmData: ByteArray) {
        if (!isSessionActive) return

        if (isSimulatedFallback) {
            // In simulation mode, user audio input drives the listening state
            return
        }

        val ws = webSocket ?: return
        try {
            val base64Chunk = Base64.encodeToString(pcmData, Base64.NO_WRAP)
            val chunkJson = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Chunk)
                        })
                    })
                })
            }
            ws.send(chunkJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending realtime audio chunk", e)
        }
    }

    private fun startSimulationMode() {
        isSessionActive = true
        isSimulatedFallback = true
        fallbackJob?.cancel()

        onStateChange(AssistantState.LISTENING)

        // Generate synthetic sassy synthesized voice tones so the user has immediate audio feedback
        fallbackJob = coroutineScope.launch(Dispatchers.IO) {
            delay(1500)
            if (!isActive || !isSessionActive) return@launch

            // Sassy synthesized audio tones
            coroutineScope.launch(Dispatchers.Main) {
                onStateChange(AssistantState.SPEAKING)
            }

            generateSassyChime()

            delay(1200)
            coroutineScope.launch(Dispatchers.Main) {
                onStateChange(AssistantState.LISTENING)
            }
        }
    }

    /**
     * Generates a playful 24kHz PCM melodic chime for instant feedback in interactive test mode.
     */
    private fun generateSassyChime() {
        val sampleRate = 24000
        val durationSec = 1.2
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ByteArray(totalSamples * 2)

        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6 playful chord
        var sampleIdx = 0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = ((t / durationSec) * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val envelope = (1.0 - (t / durationSec)).coerceIn(0.0, 1.0)
            val value = (kotlin.math.sin(2.0 * Math.PI * freq * t) * 12000.0 * envelope).toInt().toShort()

            buffer[sampleIdx++] = (value.toInt() and 0xFF).toByte()
            buffer[sampleIdx++] = ((value.toInt() shr 8) and 0xFF).toByte()
        }

        onAudioReceived(buffer)
    }

    fun stopSession() {
        isSessionActive = false
        fallbackJob?.cancel()
        fallbackJob = null
        try {
            webSocket?.close(1000, "User terminated session")
            webSocket = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing webSocket", e)
        }
        onStateChange(AssistantState.IDLE)
    }
}
