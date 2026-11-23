package com.komal.ai.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class KomalTtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Default to Indian English / Hinglish natural speech
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.05f)
            isInitialized = true
            Log.d("KomalTTS", "TTS Initialized successfully.")
        } else {
            Log.e("KomalTTS", "TTS Initialization failed: $status")
        }
    }

    fun setLanguage(langCode: String) {
        if (!isInitialized || tts == null) return
        when (langCode.lowercase()) {
            "hinglish", "hi_in" -> {
                val res = tts?.setLanguage(Locale("hi", "IN"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale("en", "IN"))
                }
            }
            "english", "en_in" -> {
                tts?.setLanguage(Locale("en", "IN"))
            }
            else -> {
                tts?.setLanguage(Locale.getDefault())
            }
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f, onDone: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w("KomalTTS", "TTS not ready yet.")
            return
        }

        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "KomalUtterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
