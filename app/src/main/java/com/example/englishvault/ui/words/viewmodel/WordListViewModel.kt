package com.example.englishvault.ui.words.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.dao.WordDao
import data.database.entities.WordEntity
import data.database.entities.isUserAdded
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Words screen.
 *
 * Surfaces every word from Room as a [StateFlow] that the screen can
 * collect directly, and exposes the two write operations the screen
 * triggers (delete + add).
 *
 * Domain rules enforced here:
 *  - Deletes are gated on [WordEntity.isUserAdded] so the UI cannot
 *    accidentally wipe seeded content even if the trash icon somehow
 *    becomes visible.
 *  - Inserts always tag the new word with [WordEntity.SOURCE_USER] so
 *    the schema invariant "every user-added row has source='user'"
 *    holds without the caller having to remember.
 *
 * Obtained from Compose via `hiltViewModel()`.
 */
@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordDao: WordDao
) : ViewModel() {

    /**
     * Reactive list of every word in the dictionary, sorted by Room's
     * default query (`ORDER BY word ASC`).
     *
     * Backed by [SharingStarted.WhileSubscribed] so the upstream
     * collector only stays alive while a screen is observing it.
     */
    val allWords: StateFlow<List<WordEntity>> = wordDao.getAllWords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    // region: Mutations
    /**
     * Persists the deletion of [word] if it is user-owned. Default /
     * seeded rows are silently ignored.
     *
     * @return `true` when a row was deleted, `false` when the row was
     *   protected or did not exist.
     */
    fun deleteWord(word: WordEntity): Boolean {
        if (!word.isUserAdded()) return false
        viewModelScope.launch { wordDao.deleteWord(word.id) }
        return true
    }

    /**
     * Persists a brand-new user-owned word. The [WordEntity.id] passed
     * by the caller is overridden (or kept as the default 0) — Room
     * assigns the next free id thanks to the AUTOINCREMENT primary key.
     */
    fun addUserWord(word: WordEntity) {
        viewModelScope.launch {
            wordDao.insertWord(
                word.copy(
                    id = 0,
                    source = WordEntity.SOURCE_USER
                )
            )
        }
    }
    // endregion

    companion object {
        /** Subscription grace period before the upstream Flow is cancelled. */
        private const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}