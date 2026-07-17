package com.example.englishvault.ui.words.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.ui.games.common.TtsPlayer
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.dao.CategoryProgressDao
import data.database.dao.WordDao
import data.database.entities.LearningStatus
import data.database.entities.WordEntity
import data.database.entities.isUserAdded
import data.database.entities.toUserEntity
import data.game.PromotionEvent
import data.game.PromotionGate
import data.game.PromotionNotifier
import data.game.PromotionOutcome
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
 *  - Status changes to [LearningStatus.LEARNED] re-evaluate the
 *    promotion gate for the word's category via
 *    [PromotionGate.evaluate]. This is what allows a learner to
 *    unlock the next level by working through their vocabulary
 *    list — they do not have to play a mini-game first.
 *
 * Obtained from Compose via `hiltViewModel()`.
 */
@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordDao: WordDao,
    private val categoryProgressDao: CategoryProgressDao,
    private val promotionNotifier: PromotionNotifier,
    private val ttsPlayer: TtsPlayer
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

    /**
     * Mirrors the [TtsPlayer.currentTextFlow] reactive stream so the
     * Words screen can highlight whichever 🔊 button is currently
     * speaking. Exposed as a `StateFlow` because the screen subscribes
     * to it directly via `collectAsState`.
     */
    val speakingText: StateFlow<String?> = ttsPlayer.currentTextFlow

    // region: TTS
    /**
     * Speaks [word] through the device TTS engine. No-op when the
     * engine has not finished initialising — the button stays tappable
     * so the user can retry after the engine binds.
     *
     * The card also reuses this entry point for each individual verb
     * form (base, third person, present participle, past simple,
     * past participle) — every form has its own inline 🔊 button
     * wired to this method, so the VM does not need a separate
     * "speak past forms" helper.
     */
    fun speakWord(word: String) {
        if (word.isBlank()) return
        ttsPlayer.ensureInitialized()
        ttsPlayer.speak(word)
    }
    // endregion

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
     * through the dual-table DAO and, on the `→LEARNED` transition,
     * re-evaluates the hybrid promotion gate for the word's
     * grammatical category via [PromotionGate.evaluate].
     *
     * The promotion gate accepts `amount = 0` for this path — we are
     * not granting XP for the manual mark, only re-checking whether
     * the (XP + learned percentage) requirements are already met.
     * If they are, the category is promoted and a [PromotionEvent]
     * is broadcast on [promotionNotifier] so any listening screen
     * (currently the Progress dashboard) can celebrate the unlock.
     */
    fun setStatus(id: Int, status: LearningStatus) {
        viewModelScope.launch {
            val previous = wordDao.getWordById(id) ?: return@launch
            wordDao.setStatus(id, status)
            if (status != LearningStatus.LEARNED) return@launch
            if (previous.status == LearningStatus.LEARNED) return@launch
            val category = WordTypeFilter.classifyOrNull(previous) ?: return@launch
            val outcome = PromotionGate.evaluate(
                categoryKey = category.name,
                amount = 0,
                wordDao = wordDao,
                categoryProgressDao = categoryProgressDao
            )
            if (outcome is PromotionOutcome.Promoted) {
                promotionNotifier.emit(
                    PromotionEvent(
                        categoryKey = category.name,
                        previousLevel = outcome.previousLevel,
                        newLevel = outcome.newLevel
                    )
                )
            }
        }
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