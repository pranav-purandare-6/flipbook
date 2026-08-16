package com.pranav.flipbook.audio

import android.content.Context
import android.media.MediaPlayer

class AmbientSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: String = "none"

    fun playAmbientSound(soundName: String, volume: Float = 0.5f) {
        if (soundName == currentSound && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.setVolume(volume, volume)
            return
        }

        stop()
        currentSound = soundName

        if (soundName == "none") return

        try {
            // Prepared for raw resources if added, fallback to silent loop safely
            val resId = context.resources.getIdentifier(
                "ambient_$soundName", "raw", context.packageName
            )
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                    isLooping = true
                    setVolume(volume, volume)
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentSound = "none"
    }

    fun release() {
        stop()
    }
}
