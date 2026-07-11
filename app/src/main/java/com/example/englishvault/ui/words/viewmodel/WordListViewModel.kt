package com.example.englishvault.ui.words.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.dao.WordDao
import data.database.entities.LearningStatus
import data.database.entities.WordEntity
import data.database.entities.isUserAdded
import data.database.entities.toUserEntity
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
        viewModelScope.launch { wordDao.deleteUserWord(word.id) }
        return true
    }

    /**
     * Promotes the [LearningStatus] of any word (core or user-added)
     * through the dual-table DAO. Available on every card so the
     * learner can mark progress even on dictionary entries.
     */
    fun setStatus(id: Int, status: LearningStatus) {
        viewModelScope.launch { wordDao.setStatus(id, status) }
    }

    /**
     * Inserts a brand-new user-owned word. The [WordEntity] coming
     * from the form (or any other UI source) is converted into a
     * [data.database.entities.UserWordEntity] via [toUserEntity] with
     * `preserveId = false`, which resets `id` to `0` so SQLite's
     * AUTOINCREMENT assigns the next free value in the `user_words`
     * sequence.
     */
    fun addUserWord(word: WordEntity) {
        viewModelScope.launch {
            wordDao.insertUserWord(word.toUserEntity(preserveId = false))
        }
    }

    /**
     * Updates an existing user-owned word. The id from [word] is
     * preserved so `OnConflictStrategy.REPLACE` overwrites the row
     * instead of inserting a duplicate.
     */
    fun updateUserWord(word: WordEntity) {
        viewModelScope.launch {
            wordDao.insertUserWord(word.toUserEntity(preserveId = true))
        }
    }
    // endregion

    // region: One-shot lookups
    /**
     * Loads a single user-added word by id for the edit form.
     *
     * @param id Primary key in the `user_words` table.
     * @return The persisted [WordEntity] or `null` when no user-added
     *   row matches.
     */
    suspend fun loadWordForEdit(id: Int): WordEntity? = wordDao.getUserWordById(id)
    // endregion

    companion object {
        /** Subscription grace period before the upstream Flow is cancelled. */
        private const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}