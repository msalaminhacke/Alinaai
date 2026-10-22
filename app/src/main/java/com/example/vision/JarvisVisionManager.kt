package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.audio.AudioStreamer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class JarvisVisionManager(
    private val context: Context,
    private val audioStreamer: AudioStreamer
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeBitmap(
        bitmap: Bitmap,
        userQuery: String = "",
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "API Key দেওয়া হয়নি। দয়া করে সেটিংসে Gemini API Key দিন।"
        }

        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = if (userQuery.isNotBlank()) {
                "You are Alina, an intelligent, charming AI assistant. The user is showing you this camera image and asks: '$userQuery'. Analyze the image precisely and give a helpful, concise answer in natural Bengali (বাংলা). Max 3 sentences."
            } else {
                "You are Alina, an intelligent AI companion. Look at this camera view, identify the main objects, text, or scene, and tell the user what you see in natural, lively Bengali (বাংলা). Max 3 sentences."
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/" + VISION_MODEL + ":generateContent?key=" + apiKey
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Vision API error: " + response.code + " " + responseBody)
                val fallbackErr = "ছবি বিশ্লেষণ করতে সমস্যা হয়েছে (Error: " + response.code + ")"
                audioStreamer.speakText(fallbackErr)
                return@withContext fallbackErr
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val cleanText = text.trim()
            if (cleanText.isNotEmpty()) {
                audioStreamer.speakText(cleanText)
            }
            cleanText
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing bitmap with Gemini Vision", e)
            val err = "ভিজ্যুয়াল স্ক্যানিং ব্যর্থ হয়েছে: " + e.localizedMessage
            audioStreamer.speakText(err)
            err
        }
    }

    suspend fun explainScreen(
        screenText: String,
        userQuery: String = "",
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "API Key দেওয়া হয়নি। দয়া করে সেটিংসে Gemini API Key দিন।"
        }

        try {
            val prompt = "You are Alina, a personal AI companion. The user wants you to explain what is currently displayed on their phone screen. Here is the extracted screen UI content:\n---\n" + screenText + "\n---\nUser query: '" + userQuery + "'. Summarize and explain what is happening on the screen clearly and delightfully in natural Bengali (বাংলা). Max 3 sentences."

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/" + VISION_MODEL + ":generateContent?key=" + apiKey
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val rootJson = JSONObject(responseBody)
            val text = rootJson.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "স্ক্রিন কনটেন্ট বুঝতে পারিনি।"

            val cleanText = text.trim()
            audioStreamer.speakText(cleanText)
            cleanText
        } catch (e: Exception) {
            Log.e(TAG, "Error explaining screen", e)
            "স্ক্রিন ব্যাখ্যা করতে সমস্যা হয়েছে।"
        }
    }

    companion object {
        private const val TAG = "JarvisVisionManager"
        private const val VISION_MODEL = "gemini-2.5-flash"
    }
}