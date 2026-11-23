package com.komal.ai

import android.app.Application
import com.komal.ai.accessibility.ScreenActionExecutor
import com.komal.ai.data.PreferencesManager
import com.komal.ai.gemini.GeminiApiClient
import com.komal.ai.gemini.GeminiAudioPlayer
import com.komal.ai.offline.OfflineIntentEngine
import com.komal.ai.tts.KomalTtsManager
import com.komal.ai.updater.AppUpdateManager

class KomalApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var geminiClient: GeminiApiClient
        private set

    lateinit var ttsManager: KomalTtsManager
        private set

    lateinit var actionExecutor: ScreenActionExecutor
        private set

    lateinit var offlineEngine: OfflineIntentEngine
        private set

    lateinit var audioPlayer: GeminiAudioPlayer
        private set

    lateinit var updateManager: AppUpdateManager
        private set

    companion object {
        lateinit var instance: KomalApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)
        geminiClient = GeminiApiClient()
        ttsManager = KomalTtsManager(this)
        actionExecutor = ScreenActionExecutor(this, ttsManager, geminiClient)
        offlineEngine = OfflineIntentEngine(this, actionExecutor, ttsManager)
        audioPlayer = GeminiAudioPlayer(this)
        updateManager = AppUpdateManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
        audioPlayer.stop()
    }
}
