package com.example.englishvault.ui.world

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.dao.UserProfileDao
import data.database.entities.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel backing the World screen (Phase 7 beta).
 *
 * The world map only needs to read the player's persistent counters
 * (hearts and coins), so the VM is intentionally minimal: a single
 * [StateFlow] over the `user_profile` row that the screen consumes
 * with `collectAsState`. Writes (`addHearts`, `addCoins`) will be
 * plumbed in once the level-clear reward logic lands — for now the
 * DAO methods exist so the dependency graph already provides them.
 */
@HiltViewModel
class WorldViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    val profile: StateFlow<UserProfileEntity?> = userProfileDao.observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    companion object {
        /** Subscription grace period before the upstream Flow is cancelled. */
        private const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
