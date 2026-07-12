package com.example.englishvault.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.dao.UserProfileDao
import data.database.entities.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Change name" sub-screen.
 *
 * Holds the editable name in a [MutableStateFlow] so the screen can
 * keep a `TextField` synced without round-tripping Room on every
 * keystroke. The current persisted value is loaded once at
 * construction time so the form opens pre-filled.
 *
 * Validation rules:
 *  - Whitespace is trimmed before validation.
 *  - Empty / whitespace-only strings are rejected so the greeting
 *    never collapses to nothing.
 */
@HiltViewModel
class SettingsEditNameViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    /**
     * Form state owned by this VM.
     *
     * @property name The current draft value displayed in the field.
     * @property error Optional validation error to surface under the
     *   field; `null` means the draft is currently valid.
     * @property saved `true` for one frame after a successful save so
     *   the screen can `popBackStack` reactively.
     */
    data class UiState(
        val name: String = "",
        val error: String? = null,
        val saved: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * Current persisted profile, kept as a StateFlow so callers can
     * show the latest avatar / XP alongside the form if they need to.
     */
    val profile: StateFlow<UserProfileEntity?> = userProfileDao.observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    init {
        // Pre-fill the form with the persisted name on first composition.
        viewModelScope.launch {
            val current = userProfileDao.observeProfile().first()
            current?.let { profile ->
                _state.update { it.copy(name = profile.name) }
            }
        }
    }

    /** Updates the draft value as the user types. */
    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    /**
     * Validates the current draft and, if valid, persists it and
     * flips [UiState.saved] so the screen can pop back.
     */
    fun save() {
        val sanitized = _state.value.name.trim()
        if (sanitized.isEmpty()) {
            _state.update { it.copy(error = ERROR_EMPTY) }
            return
        }
        viewModelScope.launch {
            userProfileDao.updateName(sanitized)
            _state.update { it.copy(name = sanitized, error = null, saved = true) }
        }
    }

    companion object {
        /** Subscription grace period before the upstream Flow is cancelled. */
        private const val STOP_TIMEOUT_MILLIS: Long = 5_000

        /** Stable error key for an empty / whitespace-only draft. */
        const val ERROR_EMPTY: String = "empty"
    }
}