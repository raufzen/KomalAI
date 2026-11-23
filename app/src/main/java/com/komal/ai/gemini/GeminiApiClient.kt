package com.komal.ai.gemini

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Dynamically fetch all models supported by the provided API key
     */
    suspend fun fetchModels(apiKey: String): Result<List<GeminiModelItem>> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty."))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val listResponse = gson.fromJson(body, GeminiModelsListResponse::class.java)
            val filtered = listResponse.models
                ?.filter { it.isChatCapable }
                ?.sortedByDescending { it.name }
                ?: emptyList()

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get tool definitions for full phone automation
     */
    private fun getPhoneTools(): List<GeminiToolConfig> {
        val tools = listOf(
            GeminiFunctionDeclaration(
                name = "open_app",
                description = "Opens an application installed on the phone by its common name (e.g., 'WhatsApp', 'YouTube', 'Chrome', 'Camera', 'Settings', etc.)",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "app_name" to mapOf("type" to "STRING", "description" to "The name of the app to launch")
                    ),
                    "required" to listOf("app_name")
                )
            ),
            GeminiFunctionDeclaration(
                name = "whatsapp_send",
                description = "Automates sending a WhatsApp message to a specific contact or phone number.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "recipient" to mapOf("type" to "STRING", "description" to "The contact name or phone number with country code"),
                        "message" to mapOf("type" to "STRING", "description" to "The exact message content to send")
                    ),
                    "required" to listOf("recipient", "message")
                )
            ),
            GeminiFunctionDeclaration(
                name = "youtube_search",
                description = "Opens YouTube and searches for a video or music query.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "query" to mapOf("type" to "STRING", "description" to "The search query for YouTube")
                    ),
                    "required" to listOf("query")
                )
            ),
            GeminiFunctionDeclaration(
                name = "read_today_news",
                description = "Fetches and reads today's latest news, headlines, or updates on a specified topic.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "topic" to mapOf("type" to "STRING", "description" to "The news topic or category (e.g. 'general', 'technology', 'sports', 'India', 'world')")
                    ),
                    "required" to listOf("topic")
                )
            ),
            GeminiFunctionDeclaration(
                name = "click_screen_element",
                description = "Clicks an interactive element, button, or link currently visible on the phone screen matching the text or identifier.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "target_text" to mapOf("type" to "STRING", "description" to "Visible text or description of the button/element to click")
                    ),
                    "required" to listOf("target_text")
                )
            ),
            GeminiFunctionDeclaration(
                name = "type_text",
                description = "Types text into the currently active or focused input field on the phone screen.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "text" to mapOf("type" to "STRING", "description" to "Text string to write or paste")
                    ),
                    "required" to listOf("text")
                )
            ),
            GeminiFunctionDeclaration(
                name = "scroll_screen",
                description = "Scrolls the current phone screen up or down.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "direction" to mapOf("type" to "STRING", "enum" to listOf("down", "up"), "description" to "Direction to scroll")
                    ),
                    "required" to listOf("direction")
                )
            ),
            GeminiFunctionDeclaration(
                name = "navigate_phone",
                description = "Executes global system navigation actions: home, back, recents, or notifications.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "action" to mapOf("type" to "STRING", "enum" to listOf("home", "back", "recents", "notifications"), "description" to "System navigation action")
                    ),
                    "required" to listOf("action")
                )
            ),
            GeminiFunctionDeclaration(
                name = "speak_to_user",
                description = "Directly speaks a spoken response back to the user.",
                parameters = mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "speech_text" to mapOf("type" to "STRING", "description" to "The speech text to speak out loud")
                    ),
                    "required" to listOf("speech_text")
                )
            )
        )
        return listOf(GeminiToolConfig(functionDeclarations = tools))
    }

    /**
     * Ask Gemini model to decide which phone action or response to execute
     * Supports optional native voice audio generation config
     */
    suspend fun planAction(
        apiKey: String,
        modelName: String,
        userPrompt: String,
        screenContext: String = "",
        languageMode: String = "hinglish",
        voicePersona: String = "Aoede",
        requestAudio: Boolean = false
    ): Result<GeminiCandidate> = withContext(Dispatchers.IO) {
        try {
            val cleanModel = modelName.removePrefix("models/")
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=${apiKey.trim()}"

            val langGuidance = if (languageMode.equals("english", ignoreCase = true)) {
                "Speak in clear, professional English. (e.g. 'Opening WhatsApp...', 'Searching YouTube for you', etc.)"
            } else {
                "Speak in natural, friendly Hinglish (English alphabet + conversational Hindi, e.g. 'Opening WhatsApp...', 'YouTube pe search kar rahi hoon', 'Rahul ko message bhej diya'). Avoid pure Devanagari script."
            }

            val systemPrompt = """
                You are Komal, an intelligent voice-controlled AI assistant and Android Phone Controller.
                Your job is to control the user's phone, execute user commands, navigate apps, and answer queries.
                
                Language Style:
                $langGuidance
                
                Current Screen Context:
                $screenContext
                
                Rules:
                1. If user asks to open WhatsApp or send a message, call whatsapp_send or open_app.
                2. If user asks to search or play something on YouTube, call youtube_search.
                3. If user asks for news or info, call read_today_news or speak_to_user.
                4. If user asks for system navigation (home, back, recents), call navigate_phone.
                5. If an on-screen element or input is requested, call click_screen_element or type_text.
                6. Accompany actions with a brief polite voice reply using speak_to_user.
            """.trimIndent()

            val generationConfig = if (requestAudio) {
                GeminiGenerationConfig(
                    temperature = 0.2f,
                    responseModalities = listOf("TEXT", "AUDIO"),
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = voicePersona)
                        )
                    )
                )
            } else {
                GeminiGenerationConfig(temperature = 0.2f)
            }

            val requestPayload = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userPrompt))
                    )
                ),
                systemInstruction = GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                tools = getPhoneTools(),
                generationConfig = generationConfig
            )

            val jsonBody = gson.toJson(requestPayload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // If model does not support audio modalities, retry without audio config
                if (requestAudio && response.code == 400) {
                    return@withContext planAction(apiKey, modelName, userPrompt, screenContext, languageMode, voicePersona, requestAudio = false)
                }
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseText"))
            }

            val genResponse = gson.fromJson(responseText, GeminiGenerateResponse::class.java)
            val candidate = genResponse.candidates?.firstOrNull()
            if (candidate != null) {
                Result.success(candidate)
            } else {
                Result.failure(Exception("No candidate returned by Gemini API."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
