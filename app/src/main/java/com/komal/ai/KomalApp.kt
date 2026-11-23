package com.komal.ai

import android.app.Application
import com.komal.ai.accessibility.ScreenActionExecutor
import com.komal.ai.data.PreferencesManager
import com.komal.ai.gemini.GeminiApiClient
import com.komal.ai.tts.KomalTtsManager

class KomalApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var geminiClient: GeminiApiClient
        private set

    lateinit var ttsManager: KomalTtsManager
        private set

    lateinit var actionExecutor: ScreenActionExecutor
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
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
    }
}
