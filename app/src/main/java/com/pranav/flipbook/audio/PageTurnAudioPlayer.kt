package com.pranav.flipbook.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File

class PageTurnAudioPlayer(context: Context) {

    private val soundPool: SoundPool
    private var soundId: Int = 0
    @Volatile private var loaded = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (id == soundId) loaded = status == 0
        }

        soundId = try {
            val soundFile: File = ProceduralSoundGenerator.ensurePageTurnSound(context.cacheDir)
            soundPool.load(soundFile.absolutePath, 1)
        } catch (_: Exception) {
            0
        }
    }

    fun play(volume: Float) {
        if (!loaded || soundId == 0) return
        val v = volume.coerceIn(0f, 1f)
        soundPool.play(soundId, v, v, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
        loaded = false
    }
}
