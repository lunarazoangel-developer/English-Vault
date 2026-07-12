package com.example.englishvault.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.dao.UserProfileDao
import data.database.entities.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings hub.
 *
 * Surfaces the [UserProfileEntity] as a reactive [StateFlow] so the
 * slider values stay in sync with Room. Each setter delegates to a
 * dedicated DAO method so the writes remain atomic and the schema
 * stays the single source of truth.
 *
 * Phase 7.1: only the profile name and the music / effects volume
 * sliders are exposed. Future settings (theme, daily goal, reminder
 * time) will hang off the same VM without breaking callers.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    /** Live profile, or `null` until the seeder has produced one. */
    val profile: StateFlow<UserProfileEntity?> = userProfileDao.observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    /**
     * Updates the display name. Empty / blank input is rejected so the
     * greeting never collapses to whitespace.
     */
    fun updateName(name: String) {
        val sanitized = name.trim()
        if (sanitized.isEmpty()) return
        viewModelScope.launch {
            userProfileDao.updateName(sanitized)
        }
    }

    /** Persists the music slider value. Clamped to `[0.0, 1.0]`. */
    fun updateMusicVolume(volume: Float) {
        viewModelScope.launch {
            userProfileDao.updateMusicVolume(volume.coerceIn(0f, 1f))
        }
    }

    /** Persists the effects slider value. Clamped to `[0.0, 1.0]`. */
    fun updateEffectsVolume(volume: Float) {
        viewModelScope.launch {
            userProfileDao.updateEffectsVolume(volume.coerceIn(0f, 1f))
        }
    }

    companion object {
        /** Subscription grace period before the upstream Flow is cancelled. */
        private const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}