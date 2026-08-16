package com.pranav.flipbook

import android.app.Application
import com.pranav.flipbook.data.database.FlipBookDatabase

class FlipBookApplication : Application() {

    val database: FlipBookDatabase by lazy {
        FlipBookDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FlipBookApplication
            private set
    }
}
