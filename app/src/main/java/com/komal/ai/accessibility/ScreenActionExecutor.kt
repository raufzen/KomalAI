package com.komal.ai.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.komal.ai.gemini.GeminiApiClient
import com.komal.ai.gemini.GeminiFunctionCall
import com.komal.ai.tts.KomalTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

class ScreenActionExecutor(
    private val context: Context,
    private val ttsManager: KomalTtsManager,
    private val geminiClient: GeminiApiClient
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Executes a tool function call decided by Gemini
     */
    fun executeFunctionCall(
        call: GeminiFunctionCall,
        apiKey: String,
        selectedModel: String,
        onStatusUpdate: (String) -> Unit
    ) {
        val args = call.args ?: emptyMap()
        Log.d("ScreenActionExecutor", "Executing tool: ${call.name} with args: $args")

        when (call.name) {
            "open_app" -> {
                val appName = args["app_name"]?.toString() ?: ""
                onStatusUpdate("Opening $appName...")
                ttsManager.speak("$appName खोल रही हूँ")
                openAppByName(appName)
            }

            "whatsapp_send" -> {
                val recipient = args["recipient"]?.toString() ?: ""
                val message = args["message"]?.toString() ?: ""
                onStatusUpdate("Sending WhatsApp message to $recipient...")
                ttsManager.speak("$recipient को WhatsApp मैसेज भेज रही हूँ")
                sendWhatsAppMessage(recipient, message)
            }

            "youtube_search" -> {
                val query = args["query"]?.toString() ?: ""
                onStatusUpdate("Searching YouTube for '$query'...")
                ttsManager.speak("YouTube पर $query सर्च कर रही हूँ")
                searchYouTube(query)
            }

            "read_today_news" -> {
                val topic = args["topic"]?.toString() ?: "general"
                onStatusUpdate("Fetching today's news on $topic...")
                fetchAndSpeakNews(topic, apiKey, selectedModel)
            }

            "click_screen_element" -> {
                val targetText = args["target_text"]?.toString() ?: ""
                onStatusUpdate("Clicking '$targetText'...")
                val service = KomalAccessibilityService.instance
                if (service != null) {
                    val clicked = service.clickByText(targetText)
                    if (clicked) {
                        ttsManager.speak("$targetText पर क्लिक कर दिया")
                    } else {
                        ttsManager.speak("$targetText स्क्रीन पर नहीं मिला")
                    }
                } else {
                    onStatusUpdate("Accessibility service is disabled!")
                }
            }

            "type_text" -> {
                val text = args["text"]?.toString() ?: ""
                onStatusUpdate("Typing '$text'...")
                val service = KomalAccessibilityService.instance
                if (service != null) {
                    service.typeTextIntoActiveField(text)
                }
            }

            "scroll_screen" -> {
                val direction = args["direction"]?.toString() ?: "down"
                onStatusUpdate("Scrolling $direction...")
                val service = KomalAccessibilityService.instance
                service?.scrollScreen(direction)
            }

            "navigate_phone" -> {
                val action = args["action"]?.toString() ?: "home"
                onStatusUpdate("Navigating: $action...")
                val service = KomalAccessibilityService.instance
                service?.performNavigation(action)
            }

            "speak_to_user" -> {
                val speech = args["speech_text"]?.toString() ?: ""
                onStatusUpdate(speech)
                ttsManager.speak(speech)
            }

            else -> {
                Log.w("ScreenActionExecutor", "Unknown function call: ${call.name}")
            }
        }
    }

    /**
     * Launch application by name using PackageManager
     */
    fun openAppByName(name: String): Boolean {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val cleanQuery = name.lowercase().trim()

        var matchedPackage: String? = null

        // Specific overrides for common apps
        when {
            cleanQuery.contains("whatsapp") -> matchedPackage = "com.whatsapp"
            cleanQuery.contains("youtube") -> matchedPackage = "com.google.android.youtube"
            cleanQuery.contains("chrome") -> matchedPackage = "com.android.chrome"
            cleanQuery.contains("camera") -> {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    // fallback
                }
            }
        }

        if (matchedPackage == null) {
            for (app in packages) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(cleanQuery) || cleanQuery.contains(label)) {
                    matchedPackage = app.packageName
                    break
                }
            }
        }

        if (matchedPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        ttsManager.speak("माफ़ कीजिए, $name ऐप मुझे आपके फोन में नहीं मिला")
        return false
    }

    /**
     * Automate WhatsApp messaging:
     * Opens chat, inputs text, and uses AccessibilityService to hit Send!
     */
    fun sendWhatsAppMessage(recipient: String, message: String) {
        scope.launch {
            val cleanRecipient = recipient.replace("[^0-9+]".toRegex(), "")
            if (cleanRecipient.length >= 10) {
                // If phone number provided, launch direct chat intent
                val url = "https://api.whatsapp.com/send?phone=$cleanRecipient&text=${URLEncoder.encode(message, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(intent)
                    delay(2500) // wait for WhatsApp chat to open
                    // Auto-click Send button via accessibility
                    val service = KomalAccessibilityService.instance
                    service?.clickByText("Send")
                        ?: service?.clickByText("भेजें")
                } catch (e: Exception) {
                    Log.e("ScreenActionExecutor", "Error opening WhatsApp via URL", e)
                    // Fallback to opening WhatsApp app directly
                    openAppByName("WhatsApp")
                }
            } else {
                // Search contact name workflow
                openAppByName("WhatsApp")
                delay(2000)
                val service = KomalAccessibilityService.instance
                if (service != null) {
                    // Click search icon or input
                    val searchClicked = service.clickByText("Search") || service.clickByText("खोजें")
                    if (searchClicked) {
                        delay(1000)
                        service.typeTextIntoActiveField(recipient)
                        delay(1500)
                        service.clickByText(recipient)
                        delay(1500)
                        service.typeTextIntoActiveField(message)
                        delay(800)
                        service.clickByText("Send") || service.clickByText("भेजें")
                    }
                }
            }
        }
    }

    /**
     * Search and play on YouTube
     */
    fun searchYouTube(query: String) {
        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback to browser YouTube URL
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Fetch news summary via Gemini API and speak it aloud
     */
    fun fetchAndSpeakNews(topic: String, apiKey: String, selectedModel: String) {
        scope.launch {
            ttsManager.speak("आज की ताज़ा ख़बरें ला रही हूँ...")
            val prompt = "Give a concise 3-bullet summary of today's top news updates regarding: $topic. Answer in natural conversational Hindi so it sounds great when spoken aloud."
            val result = geminiClient.planAction(apiKey, selectedModel, prompt, "")
            result.onSuccess { candidate ->
                val newsText = candidate.content?.parts?.firstOrNull()?.text
                if (!newsText.isNullOrBlank()) {
                    ttsManager.speak(newsText)
                } else {
                    ttsManager.speak("आज की कोई विशेष ख़बर नहीं मिली")
                }
            }.onFailure {
                ttsManager.speak("ख़बर लाने में कुछ रुकावट आई है, कृपया इंटरनेट चेक करें")
            }
        }
    }
}
