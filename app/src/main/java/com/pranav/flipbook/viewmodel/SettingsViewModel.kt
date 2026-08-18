package com.pranav.flipbook.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.ui.reader.pagecurl.PageTransitionStyle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val TRANSITION_STYLE = stringPreferencesKey("transition_style")
    val ANIMATION_SPEED = intPreferencesKey("animation_speed")
    val PAGE_SOUND = booleanPreferencesKey("page_sound")
    val PAGE_SOUND_VOLUME = floatPreferencesKey("page_sound_volume")
    val AMBIENT_SOUND = stringPreferencesKey("ambient_sound")
    val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
    val READER_BRIGHTNESS = floatPreferencesKey("reader_brightness")
    val READER_THEME = stringPreferencesKey("reader_theme")
    val MARGIN_SIZE = stringPreferencesKey("margin_size")
    val AUTO_HIDE_CONTROLS = booleanPreferencesKey("auto_hide_controls")
    val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
    val SORT_ORDER = stringPreferencesKey("sort_order")
    val PAGE_MODE = stringPreferencesKey("page_mode")
    val SHOW_BOOK_OPENING = booleanPreferencesKey("show_book_opening")
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.settingsDataStore

    val darkMode: StateFlow<Boolean> = dataStore.data
        .map { it[SettingsKeys.DARK_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val transitionStyle: StateFlow<String> = dataStore.data
        .map { it[SettingsKeys.TRANSITION_STYLE] ?: "CURL" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "CURL")

    val animationSpeed: StateFlow<Int> = dataStore.data
        .map { it[SettingsKeys.ANIMATION_SPEED] ?: 400 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 400)

    val pageSoundEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[SettingsKeys.PAGE_SOUND] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pageSoundVolume: StateFlow<Float> = dataStore.data
        .map { it[SettingsKeys.PAGE_SOUND_VOLUME] ?: 0.6f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.6f)

    val ambientSound: StateFlow<String> = dataStore.data
        .map { it[SettingsKeys.AMBIENT_SOUND] ?: "none" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "none")

    val ambientVolume: StateFlow<Float> = dataStore.data
        .map { it[SettingsKeys.AMBIENT_VOLUME] ?: 0.5f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    val readerBrightness: StateFlow<Float> = dataStore.data
        .map { it[SettingsKeys.READER_BRIGHTNESS] ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val readerTheme: StateFlow<String> = dataStore.data
        .map { it[SettingsKeys.READER_THEME] ?: "light" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "light")

    val marginSize: StateFlow<String> = dataStore.data
        .map { it[SettingsKeys.MARGIN_SIZE] ?: "medium" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "medium")

    val autoHideControls: StateFlow<Boolean> = dataStore.data
        .map { it[SettingsKeys.AUTO_HIDE_CONTROLS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showBookOpening: StateFlow<Boolean> = dataStore.data
        .map { it[SettingsKeys.SHOW_BOOK_OPENING] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun <T> updateSetting(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    fun setDarkMode(enabled: Boolean) = updateSetting(SettingsKeys.DARK_MODE, enabled)
    fun setTransitionStyle(style: String) = updateSetting(SettingsKeys.TRANSITION_STYLE, style)
    fun setAnimationSpeed(speed: Int) = updateSetting(SettingsKeys.ANIMATION_SPEED, speed)
    fun setPageSound(enabled: Boolean) = updateSetting(SettingsKeys.PAGE_SOUND, enabled)
    fun setPageSoundVolume(volume: Float) = updateSetting(SettingsKeys.PAGE_SOUND_VOLUME, volume)
    fun setAmbientSound(sound: String) = updateSetting(SettingsKeys.AMBIENT_SOUND, sound)
    fun setAmbientVolume(volume: Float) = updateSetting(SettingsKeys.AMBIENT_VOLUME, volume)
    fun setReaderBrightness(brightness: Float) = updateSetting(SettingsKeys.READER_BRIGHTNESS, brightness)
    fun setReaderTheme(theme: String) = updateSetting(SettingsKeys.READER_THEME, theme)
    fun setMarginSize(size: String) = updateSetting(SettingsKeys.MARGIN_SIZE, size)
    fun setAutoHideControls(enabled: Boolean) = updateSetting(SettingsKeys.AUTO_HIDE_CONTROLS, enabled)
    fun setShowBookOpening(show: Boolean) = updateSetting(SettingsKeys.SHOW_BOOK_OPENING, show)
}
