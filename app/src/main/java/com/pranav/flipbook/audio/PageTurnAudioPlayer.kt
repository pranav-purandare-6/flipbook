package com.pranav.flipbook.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class PageTurnAudioPlayer(context: Context) {

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var isLoaded = false

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, _, status ->
                isLoaded = status == 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPageTurnSound(volume: Float = 0.5f) {
        if (isLoaded && soundPool != null && soundId != 0) {
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
    }
}
