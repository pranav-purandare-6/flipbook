package com.pranav.flipbook

import android.app.Application
import com.pranav.flipbook.audio.AudioManager
import com.pranav.flipbook.data.database.FlipBookDatabase

class FlipBookApplication : Application() {

    val database: FlipBookDatabase by lazy {
        FlipBookDatabase.getInstance(this)
    }

    val audioManager: AudioManager by lazy {
        AudioManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        audioManager.release()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: FlipBookApplication
            private set
    }
}
