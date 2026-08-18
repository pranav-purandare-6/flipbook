package com.pranav.flipbook.audio

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class AmbientSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var activeSound: String = "none"

    @Synchronized
    fun play(soundName: String, volume: Float) {
        val normalized = soundName.lowercase()
        if (normalized == "none") {
            stop()
            return
        }

        if (normalized == activeSound && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.setVolume(volume, volume)
            return
        }

        stopInternal()

        try {
            val file: File = ProceduralSoundGenerator.ensureAmbientLoop(context.cacheDir, normalized)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
            activeSound = normalized
        } catch (e: Exception) {
            stopInternal()
        }
    }

    @Synchronized
    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(v, v)
    }

    @Synchronized
    fun stop() {
        stopInternal()
    }

    @Synchronized
    fun release() {
        stopInternal()
    }

    private fun stopInternal() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        activeSound = "none"
    }
}
