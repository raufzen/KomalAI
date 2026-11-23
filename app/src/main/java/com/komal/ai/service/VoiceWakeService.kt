package com.komal.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.komal.ai.KomalApp
import com.komal.ai.accessibility.KomalAccessibilityService
import com.komal.ai.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VoiceWakeService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var isListening = false
    private var isCommandMode = false

    companion object {
        private const val CHANNEL_ID = "komal_voice_wake_channel"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, VoiceWakeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceWakeService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Komal AI is active in background - Say 'Hey Komal'"))
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListening()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Komal Voice Wake Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors wake-word in background for hands-free phone control"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Komal AI Voice Controller")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("VoiceWakeService", "SpeechRecognizer not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                mainHandler.postDelayed({
                    startListening()
                }, 1000)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognized = matches[0].lowercase().trim()
                    Log.d("VoiceWakeService", "Heard speech: $recognized")
                    handleRecognizedSpeech(recognized)
                } else {
                    restartListeningLoop()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (partial != null && !isCommandMode) {
                    val lower = partial.lowercase()
                    if (containsWakeWord(lower)) {
                        speechRecognizer?.stopListening()
                        triggerWakeMode()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (speechRecognizer != null && recognizerIntent != null && !isListening) {
            try {
                speechRecognizer?.startListening(recognizerIntent)
                isListening = true
            } catch (e: Exception) {
                Log.e("VoiceWakeService", "Error starting listening", e)
            }
        }
    }

    private fun containsWakeWord(text: String): Boolean {
        val app = application as? KomalApp ?: return false
        val wakeWord = app.preferencesManager.wakeWord.lowercase().trim()
        val variations = listOf("komal", "hey komal", "suno komal", "ok komal", "hello komal", "कोमल", "सुनो कोमल")

        return text.contains(wakeWord) || variations.any { text.contains(it) }
    }

    private fun triggerWakeMode() {
        isCommandMode = true
        val app = application as? KomalApp ?: return
        val isHinglish = app.preferencesManager.appLanguage != "english"

        FloatingOverlayService.show(this, if (isHinglish) "Sun rahi hoon..." else "Listening...")
        app.ttsManager.speak(if (isHinglish) "Haan ji, boliye kya karun?" else "Yes, how can I help you?")

        mainHandler.postDelayed({
            startListening()
        }, 1500)
    }

    private fun handleRecognizedSpeech(recognized: String) {
        val app = application as? KomalApp ?: return
        val isHinglish = app.preferencesManager.appLanguage != "english"

        if (!isCommandMode) {
            if (containsWakeWord(recognized)) {
                triggerWakeMode()
            } else {
                restartListeningLoop()
            }
        } else {
            isCommandMode = false
            FloatingOverlayService.update(this, "Recognized: \"$recognized\"")

            // 1. Check Offline Intent Engine first!
            if (app.preferencesManager.offlineModeEnabled) {
                val executedOffline = app.offlineEngine.tryExecuteOffline(recognized, isHinglish)
                if (executedOffline) {
                    FloatingOverlayService.update(this, "Executed offline!")
                    hideOverlayAfterDelay(2500)
                    restartListeningLoop()
                    return
                }
            }

            // 2. If not handled offline, route to Gemini AI
            processCommandWithGemini(recognized)
        }
    }

    private fun processCommandWithGemini(command: String) {
        val app = application as? KomalApp ?: return
        val apiKey = app.preferencesManager.apiKey
        val model = app.preferencesManager.selectedMainModel
        val langMode = app.preferencesManager.appLanguage
        val voicePersona = app.preferencesManager.geminiVoicePersona
        val useNativeVoice = app.preferencesManager.useGeminiNativeVoice

        if (apiKey.isBlank()) {
            FloatingOverlayService.update(this, "Error: Gemini API Key missing!")
            app.ttsManager.speak(if (langMode == "english") "Please set your Gemini API Key in Komal AI settings" else "Please pehle Komal app me jaake Gemini API Key set karein")
            hideOverlayAfterDelay(3500)
            restartListeningLoop()
            return
        }

        scope.launch {
            try {
                val screenHierarchy = KomalAccessibilityService.instance?.dumpScreenHierarchy() ?: ""

                val result = app.geminiClient.planAction(
                    apiKey = apiKey,
                    modelName = model,
                    userPrompt = command,
                    screenContext = screenHierarchy,
                    languageMode = langMode,
                    voicePersona = voicePersona,
                    requestAudio = useNativeVoice
                )

                result.onSuccess { candidate ->
                    val part = candidate.content?.parts?.firstOrNull()

                    // Check for function call
                    if (part?.functionCall != null) {
                        app.actionExecutor.executeFunctionCall(
                            call = part.functionCall,
                            apiKey = apiKey,
                            selectedModel = model,
                            onStatusUpdate = { status ->
                                FloatingOverlayService.update(this@VoiceWakeService, status)
                            }
                        )
                    }

                    // Check for Gemini native speech audio
                    val audioPart = candidate.content?.parts?.firstOrNull { it.inlineData != null }
                    if (audioPart?.inlineData != null) {
                        val inline = audioPart.inlineData
                        app.audioPlayer.playBase64Audio(inline.data, inline.mimeType)
                    } else if (!part?.text.isNullOrBlank()) {
                        val reply = part!!.text!!
                        FloatingOverlayService.update(this@VoiceWakeService, reply)
                        app.ttsManager.speak(reply)
                    }
                }.onFailure { error ->
                    Log.e("VoiceWakeService", "Gemini error: ${error.message}", error)
                    FloatingOverlayService.update(this@VoiceWakeService, "Error: ${error.localizedMessage}")
                    app.ttsManager.speak(if (langMode == "english") "Sorry, could not process command" else "Command process karne me problem aayi hai")
                }
            } catch (e: Exception) {
                Log.e("VoiceWakeService", "Command process exception", e)
            } finally {
                hideOverlayAfterDelay(4000)
                restartListeningLoop()
            }
        }
    }

    private fun hideOverlayAfterDelay(delayMs: Long) {
        mainHandler.postDelayed({
            FloatingOverlayService.hide(this)
        }, delayMs)
    }

    private fun restartListeningLoop() {
        mainHandler.postDelayed({
            startListening()
        }, 800)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
        FloatingOverlayService.hide(this)
        (application as? KomalApp)?.audioPlayer?.stop()
    }
}
