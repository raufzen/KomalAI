package com.komal.ai.voice

import android.content.Context
import android.util.Log
import com.komal.ai.KomalApp
import com.komal.ai.gemini.GeminiApiClient
import com.komal.ai.gemini.GeminiAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeminiVoiceSpeaker(
    private val context: Context,
    private val geminiClient: GeminiApiClient,
    private val audioPlayer: GeminiAudioPlayer
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val audioCache = mutableMapOf<String, Pair<String, String>>()

    /**
     * Speaks using Google Gemini AI native voice output (NO local Android TTS)
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        val app = context.applicationContext as? KomalApp ?: return
        val apiKey = app.preferencesManager.apiKey
        val voiceModel = app.preferencesManager.selectedVoiceModel
        val voicePersona = app.preferencesManager.geminiVoicePersona

        if (apiKey.isBlank()) {
            Log.w("GeminiVoiceSpeaker", "Gemini API key is blank, cannot speak")
            onDone?.invoke()
            return
        }

        val cacheKey = "$voiceModel:$voicePersona:$text"
        val cached = audioCache[cacheKey]
        if (cached != null) {
            audioPlayer.playBase64Audio(cached.first, cached.second, onDone)
            return
        }

        scope.launch {
            val result = geminiClient.generateSpeechAudio(
                apiKey = apiKey,
                voiceModel = voiceModel,
                textToSpeak = text,
                voicePersona = voicePersona
            )

            result.onSuccess { (data, mimeType) ->
                audioCache[cacheKey] = Pair(data, mimeType)
                audioPlayer.playBase64Audio(data, mimeType, onDone)
            }.onFailure { e ->
                Log.e("GeminiVoiceSpeaker", "Failed to generate speech audio via Gemini", e)
                onDone?.invoke()
            }
        }
    }

    fun stop() {
        audioPlayer.stop()
    }
}
