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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
        startForeground(NOTIFICATION_ID, buildNotification("Komal AI पृष्ठभूमि में सक्रिय है - 'सुनो कोमल' बोलें"))
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
            .setContentTitle("Komal AI Voice Assistant")
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
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
                // Auto restart speech listener after brief delay
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
        val hindiVariations = listOf("komal", "hey komal", "कोमल", "सुनो कोमल", "ok komal", "hello komal")

        return text.contains(wakeWord) || hindiVariations.any { text.contains(it) }
    }

    private fun triggerWakeMode() {
        isCommandMode = true
        val app = application as? KomalApp ?: return

        FloatingOverlayService.show(this, "सुन रही हूँ... आज्ञा दीजिए")
        app.ttsManager.speak("हाँ जी, कहिए क्या करूँ?")

        // Wait for TTS to finish speaking and capture command
        mainHandler.postDelayed({
            startListening()
        }, 1600)
    }

    private fun handleRecognizedSpeech(recognized: String) {
        val app = application as? KomalApp ?: return

        if (!isCommandMode) {
            if (containsWakeWord(recognized)) {
                triggerWakeMode()
            } else {
                restartListeningLoop()
            }
        } else {
            // Process the full phone command!
            isCommandMode = false
            FloatingOverlayService.update(this, "Thinking: \"$recognized\"")

            processCommandWithGemini(recognized)
        }
    }

    private fun processCommandWithGemini(command: String) {
        val app = application as? KomalApp ?: return
        val apiKey = app.preferencesManager.apiKey
        val model = app.preferencesManager.selectedMainModel

        if (apiKey.isBlank()) {
            FloatingOverlayService.update(this, "Error: Gemini API Key missing!")
            app.ttsManager.speak("कृपया पहले Komal ऐप में जाकर Gemini API Key सेट करें")
            hideOverlayAfterDelay(3500)
            restartListeningLoop()
            return
        }

        scope.launch {
            try {
                // Get current screen tree from accessibility service
                val screenHierarchy = KomalAccessibilityService.instance?.dumpScreenHierarchy() ?: ""

                val result = app.geminiClient.planAction(
                    apiKey = apiKey,
                    modelName = model,
                    userPrompt = command,
                    screenContext = screenHierarchy
                )

                result.onSuccess { candidate ->
                    val part = candidate.content?.parts?.firstOrNull()
                    if (part?.functionCall != null) {
                        app.actionExecutor.executeFunctionCall(
                            call = part.functionCall,
                            apiKey = apiKey,
                            selectedModel = model,
                            onStatusUpdate = { status ->
                                FloatingOverlayService.update(this@VoiceWakeService, status)
                            }
                        )
                    } else if (!part?.text.isNullOrBlank()) {
                        val reply = part!!.text!!
                        FloatingOverlayService.update(this@VoiceWakeService, reply)
                        app.ttsManager.speak(reply)
                    }
                }.onFailure { error ->
                    Log.e("VoiceWakeService", "Gemini error: ${error.message}", error)
                    FloatingOverlayService.update(this@VoiceWakeService, "Gemini Error: ${error.localizedMessage}")
                    app.ttsManager.speak("कमांड पूरा करने में समस्या आई है")
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
    }
}
