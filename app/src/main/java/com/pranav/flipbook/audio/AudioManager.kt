package com.pranav.flipbook.audio

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single authoritative audio controller for page-turn and ambient playback.
 * Ensures one ambient stream at a time and correct lifecycle cleanup.
 */
class AudioManager(context: Context) {

    private val appContext = context.applicationContext
    private val pageTurnPlayer = PageTurnAudioPlayer(appContext)
    private val ambientPlayer = AmbientSoundPlayer(appContext)

    private var pageSoundEnabled = false
    private var pageSoundVolume = 0.6f
    private var ambientEnabled = false
    private var ambientSound = "none"
    private var ambientVolume = 0.5f
    private var readerActive = false
    private var backgroundPaused = false

    private val _isAmbientPlaying = MutableStateFlow(false)
    val isAmbientPlaying: StateFlow<Boolean> = _isAmbientPlaying.asStateFlow()

    fun updateSettings(
        pageSound: Boolean,
        pageVolume: Float = 0.6f,
        ambient: String,
        ambientVol: Float
    ) {
        pageSoundEnabled = pageSound
        pageSoundVolume = pageVolume.coerceIn(0f, 1f)
        ambientVolume = ambientVol.coerceIn(0f, 1f)

        val newAmbient = ambient.lowercase()
        val ambientChanged = newAmbient != ambientSound
        ambientSound = newAmbient
        ambientEnabled = newAmbient != "none"

        if (!readerActive || backgroundPaused) return

        if (!ambientEnabled) {
            ambientPlayer.stop()
            _isAmbientPlaying.value = false
        } else if (ambientChanged) {
            ambientPlayer.play(ambientSound, ambientVolume)
            _isAmbientPlaying.value = true
        } else {
            ambientPlayer.setVolume(ambientVolume)
        }
    }

    fun onReaderEnter() {
        readerActive = true
        backgroundPaused = false
        if (ambientEnabled && ambientSound != "none") {
            ambientPlayer.play(ambientSound, ambientVolume)
            _isAmbientPlaying.value = true
        }
    }

    fun onReaderExit() {
        readerActive = false
        backgroundPaused = false
        ambientPlayer.stop()
        _isAmbientPlaying.value = false
    }

    fun onReaderPause() {
        if (!readerActive || backgroundPaused) return
        backgroundPaused = true
        ambientPlayer.stop()
        _isAmbientPlaying.value = false
    }

    fun onReaderResume() {
        if (!readerActive || !backgroundPaused) return
        backgroundPaused = false
        if (ambientEnabled && ambientSound != "none") {
            ambientPlayer.play(ambientSound, ambientVolume)
            _isAmbientPlaying.value = true
        }
    }

    /** Called once when a page turn animation begins (validated turn, not cancel). */
    fun onPageTurnStart() {
        if (readerActive && !backgroundPaused && pageSoundEnabled) {
            pageTurnPlayer.play(pageSoundVolume)
        }
    }

    fun release() {
        ambientPlayer.release()
        pageTurnPlayer.release()
        _isAmbientPlaying.value = false
        readerActive = false
        backgroundPaused = false
    }
}
