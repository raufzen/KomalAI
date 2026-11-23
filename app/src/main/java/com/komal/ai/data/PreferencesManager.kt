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

    /**
     * 1. Phone Controller Brain Model (For Accessibility, UI inspection, Tool Calling)
     */
    var selectedControllerModel: String
        get() = prefs.getString("selected_controller_model", "models/gemini-2.0-flash") ?: "models/gemini-2.0-flash"
        set(value) = prefs.edit().putString("selected_controller_model", value).apply()

    /**
     * 2. Voice Output Model (For Gemini AI Speech & Audio Generation)
     */
    var selectedVoiceModel: String
        get() = prefs.getString("selected_voice_model", "models/gemini-2.0-flash") ?: "models/gemini-2.0-flash"
        set(value) = prefs.edit().putString("selected_voice_model", value).apply()

    /**
     * 3. Test Console Model (For Interactive Testing Playground)
     */
    var selectedTestModel: String
        get() = prefs.getString("selected_test_model", "models/gemini-2.0-flash") ?: "models/gemini-2.0-flash"
        set(value) = prefs.edit().putString("selected_test_model", value).apply()

    /**
     * Gemini Voice Persona: "Aoede", "Kore", "Puck", "Charon", "Fenrir"
     */
    var geminiVoicePersona: String
        get() = prefs.getString("gemini_voice_persona", "Aoede") ?: "Aoede"
        set(value) = prefs.edit().putString("gemini_voice_persona", value).apply()

    var wakeWord: String
        get() = prefs.getString("wake_word", "hey komal") ?: "hey komal"
        set(value) = prefs.edit().putString("wake_word", value).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "hinglish") ?: "hinglish"
        set(value) = prefs.edit().putString("app_language", value).apply()

    /**
     * Fast offline action processing without calling Gemini API
     */
    var offlineModeEnabled: Boolean
        get() = prefs.getBoolean("offline_mode_enabled", true)
        set(value) = prefs.edit().putBoolean("offline_mode_enabled", value).apply()

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
