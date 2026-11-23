package com.komal.ai.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.komal.ai.KomalApp
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

    private val isHinglish: Boolean
        get() = (context.applicationContext as? KomalApp)?.preferencesManager?.appLanguage != "english"

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
                ttsManager.speak(if (isHinglish) "$appName open kar rahi hoon" else "Opening $appName")
                openAppByName(appName)
            }

            "whatsapp_send" -> {
                val recipient = args["recipient"]?.toString() ?: ""
                val message = args["message"]?.toString() ?: ""
                onStatusUpdate("Sending WhatsApp message to $recipient...")
                ttsManager.speak(if (isHinglish) "$recipient ko WhatsApp message bhej rahi hoon" else "Sending WhatsApp message to $recipient")
                sendWhatsAppMessage(recipient, message)
            }

            "youtube_search" -> {
                val query = args["query"]?.toString() ?: ""
                onStatusUpdate("Searching YouTube for '$query'...")
                ttsManager.speak(if (isHinglish) "YouTube pe $query play kar rahi hoon" else "Searching $query on YouTube")
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
                        ttsManager.speak(if (isHinglish) "$targetText pe click kar diya" else "Clicked on $targetText")
                    } else {
                        ttsManager.speak(if (isHinglish) "$targetText screen pe nahi mila" else "Could not find $targetText on screen")
                    }
                } else {
                    onStatusUpdate("Accessibility service is disabled!")
                }
            }

            "type_text" -> {
                val text = args["text"]?.toString() ?: ""
                onStatusUpdate("Typing '$text'...")
                val service = KomalAccessibilityService.instance
                service?.typeTextIntoActiveField(text)
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

        ttsManager.speak(if (isHinglish) "Sorry, $name app nahi mila" else "Sorry, I could not find $name on your phone")
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
                val url = "https://api.whatsapp.com/send?phone=$cleanRecipient&text=${URLEncoder.encode(message, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(intent)
                    delay(2500) // wait for WhatsApp chat to open
                    val service = KomalAccessibilityService.instance
                    service?.clickByText("Send")
                        ?: service?.clickByText("भेजें")
                } catch (e: Exception) {
                    Log.e("ScreenActionExecutor", "Error opening WhatsApp via URL", e)
                    openAppByName("WhatsApp")
                }
            } else {
                openAppByName("WhatsApp")
                delay(2000)
                val service = KomalAccessibilityService.instance
                if (service != null) {
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
            ttsManager.speak(if (isHinglish) "Aaj ki top news la rahi hoon..." else "Fetching today's top headlines...")
            val langDesc = if (isHinglish) "conversational Hinglish (English alphabet + simple Hindi)" else "clear English"
            val prompt = "Give a crisp 3-bullet summary of today's top news updates regarding: $topic. Answer in $langDesc so it sounds pleasant and natural when spoken aloud."
            val result = geminiClient.planAction(apiKey, selectedModel, prompt, "", if (isHinglish) "hinglish" else "english")
            result.onSuccess { candidate ->
                val newsText = candidate.content?.parts?.firstOrNull()?.text
                if (!newsText.isNullOrBlank()) {
                    ttsManager.speak(newsText)
                } else {
                    ttsManager.speak(if (isHinglish) "Koi khaas news nahi mili" else "No recent updates found for $topic")
                }
            }.onFailure {
                ttsManager.speak(if (isHinglish) "News fetch karne me issue aaya hai, please internet check karein" else "Could not fetch news, please check your internet connection")
            }
        }
    }
}
