package com.komal.ai.offline

import android.content.Context
import android.util.Log
import com.komal.ai.accessibility.KomalAccessibilityService
import com.komal.ai.accessibility.ScreenActionExecutor
import com.komal.ai.tts.KomalTtsManager

class OfflineIntentEngine(
    private val context: Context,
    private val executor: ScreenActionExecutor,
    private val ttsManager: KomalTtsManager
) {
    /**
     * Checks if user command is a direct basic action that can be executed
     * 100% offline via Accessibility Service without calling Gemini API
     */
    fun tryExecuteOffline(command: String, isHinglish: Boolean): Boolean {
        val clean = command.lowercase().trim()

        // 1. WhatsApp Launch
        if (clean == "open whatsapp" || clean == "whatsapp" || clean.contains("whatsapp open") || clean.contains("whatsapp kholo")) {
            ttsManager.speak(if (isHinglish) "WhatsApp open kar rahi hoon" else "Opening WhatsApp")
            return executor.openAppByName("WhatsApp")
        }

        // 2. YouTube Launch
        if (clean == "open youtube" || clean == "youtube" || clean.contains("youtube open") || clean.contains("youtube kholo")) {
            ttsManager.speak(if (isHinglish) "YouTube open kar rahi hoon" else "Opening YouTube")
            return executor.openAppByName("YouTube")
        }

        // 3. Camera Launch
        if (clean == "open camera" || clean == "camera" || clean.contains("camera kholo") || clean.contains("camera open")) {
            ttsManager.speak(if (isHinglish) "Camera open kar rahi hoon" else "Opening Camera")
            return executor.openAppByName("Camera")
        }

        // 4. Settings Launch
        if (clean == "open settings" || clean == "settings" || clean.contains("settings kholo") || clean.contains("phone settings")) {
            ttsManager.speak(if (isHinglish) "Settings open kar rahi hoon" else "Opening Settings")
            return executor.openAppByName("Settings")
        }

        // 5. System Navigation: Home
        if (clean == "go home" || clean == "home" || clean == "home screen" || clean.contains("home pe jao") || clean.contains("home screen par jao")) {
            ttsManager.speak(if (isHinglish) "Home screen par aa gaye" else "Going home")
            return KomalAccessibilityService.instance?.performNavigation("home") ?: false
        }

        // 6. System Navigation: Back
        if (clean == "go back" || clean == "back" || clean.contains("piche jao") || clean.contains("back jao")) {
            return KomalAccessibilityService.instance?.performNavigation("back") ?: false
        }

        // 7. System Navigation: Recents / Overview
        if (clean == "recent apps" || clean == "recents" || clean.contains("recent apps kholo") || clean.contains("multitasking")) {
            ttsManager.speak(if (isHinglish) "Recent apps khol diye" else "Opening recent apps")
            return KomalAccessibilityService.instance?.performNavigation("recents") ?: false
        }

        // 8. Notifications
        if (clean == "notifications" || clean.contains("notification kholo") || clean.contains("show notifications")) {
            return KomalAccessibilityService.instance?.performNavigation("notifications") ?: false
        }

        // 9. Scroll Down
        if (clean == "scroll down" || clean.contains("niche scroll") || clean.contains("scroll karo")) {
            return KomalAccessibilityService.instance?.scrollScreen("down") ?: false
        }

        // 10. Scroll Up
        if (clean == "scroll up" || clean.contains("upar scroll")) {
            return KomalAccessibilityService.instance?.scrollScreen("up") ?: false
        }

        // 11. Chrome / Browser Launch
        if (clean == "open chrome" || clean == "open browser" || clean.contains("chrome kholo")) {
            ttsManager.speak(if (isHinglish) "Chrome browser open kar rahi hoon" else "Opening Chrome")
            return executor.openAppByName("Chrome")
        }

        // 12. Gallery Launch
        if (clean == "open gallery" || clean == "gallery" || clean.contains("gallery kholo") || clean.contains("photos kholo")) {
            ttsManager.speak(if (isHinglish) "Gallery open kar rahi hoon" else "Opening Gallery")
            return executor.openAppByName("Photos")
        }

        return false
    }
}
