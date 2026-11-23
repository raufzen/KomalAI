package com.komal.ai.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.komal.ai.gemini.GeminiModelItem

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("komal_ai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var apiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var selectedMainModel: String
        get() = prefs.getString("selected_main_model", "models/gemini-1.5-flash") ?: "models/gemini-1.5-flash"
        set(value) = prefs.edit().putString("selected_main_model", value).apply()

    var selectedVoiceModel: String
        get() = prefs.getString("selected_voice_model", "models/gemini-1.5-flash") ?: "models/gemini-1.5-flash"
        set(value) = prefs.edit().putString("selected_voice_model", value).apply()

    var wakeWord: String
        get() = prefs.getString("wake_word", "hey komal") ?: "hey komal"
        set(value) = prefs.edit().putString("wake_word", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "hinglish") ?: "hinglish"
        set(value) = prefs.edit().putString("app_language", value).apply()

    /**
     * Gemini Native Speech Voice Persona: "Aoede", "Kore", "Puck", "Charon", "Fenrir"
     */
    var geminiVoicePersona: String
        get() = prefs.getString("gemini_voice_persona", "Aoede") ?: "Aoede"
        set(value) = prefs.edit().putString("gemini_voice_persona", value).apply()

    var useGeminiNativeVoice: Boolean
        get() = prefs.getBoolean("use_gemini_native_voice", true)
        set(value) = prefs.edit().putBoolean("use_gemini_native_voice", value).apply()

    /**
     * Fast offline action processing without calling Gemini API
     */
    var offlineModeEnabled: Boolean
        get() = prefs.getBoolean("offline_mode_enabled", true)
        set(value) = prefs.edit().putBoolean("offline_mode_enabled", value).apply()

    /**
     * UI Customization: "cyberpunk", "emerald", "ocean", "sunset"
     */
    var interfaceTheme: String
        get() = prefs.getString("interface_theme", "cyberpunk") ?: "cyberpunk"
        set(value) = prefs.edit().putString("interface_theme", value).apply()

    var overlayPillSize: String
        get() = prefs.getString("overlay_pill_size", "standard") ?: "standard"
        set(value) = prefs.edit().putString("overlay_pill_size", value).apply()

    var voiceLanguage: String
        get() = prefs.getString("voice_language", "en_IN") ?: "en_IN"
        set(value) = prefs.edit().putString("voice_language", value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat("tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("tts_pitch", value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat("tts_speed", 1.0f)
        set(value) = prefs.edit().putFloat("tts_speed", value).apply()

    var isVoiceWakeEnabled: Boolean
        get() = prefs.getBoolean("voice_wake_enabled", false)
        set(value) = prefs.edit().putBoolean("voice_wake_enabled", value).apply()

    var cachedModels: List<GeminiModelItem>
        get() {
            val json = prefs.getString("cached_gemini_models", null) ?: return emptyList()
            val type = object : TypeToken<List<GeminiModelItem>>() {}.type
            return try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString("cached_gemini_models", json).apply()
        }
}
