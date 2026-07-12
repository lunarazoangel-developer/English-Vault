package data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity backing the `core_words` table.
 *
 * This table holds every entry that originates from the bundled
 * dictionary (`assets/dictionary/`). Rows here are conceptually
 * immutable from the user's perspective — the Words screen hides
 * edit/delete controls for them and the DAO never exposes a
 * `deleteCoreWord` operation. The user state columns
 * ([favorite], [status], [notes], [reviewCount], [lastReview],
 * [nextReview], [customDifficulty]) are still updatable so the
 * learner can mark a core word as learned, almost, etc.
 *
 * Schema parity with [UserWordEntity] is intentional: the
 * `words_view` UNIONs both tables into a single read model and
 * requires identical column lists and types.
 */
@Entity(tableName = "core_words")
data class CoreWordEntity(
    /**
     * Auto-generated primary key. Always inserted with `id = 0` so
     * SQLite's AUTOINCREMENT picks the next free value, decoupling
     * the dictionary identifier from any value in the source JSON.
     * This prevents collisions with user-added rows in `user_words`,
     * which owns its own AUTOINCREMENT sequence.
     */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val type: String,
    val regular: Boolean?,
    val forms: Forms?,
    val pronunciation: Pronunciation?,
    val category: List<String>?,
    val synonyms: List<String>?,
    val antonyms: List<String>?,
    val examples: List<Example>?,
    val tags: List<String>?,
    val difficulty: Difficulty,
    /**
     * Tri-state learning progress. Defaults to [LearningStatus.NOT_LEARNED]
     * for freshly seeded rows; promoted manually by the user through
     * the status button on each [WordCard].
     */
    val status: LearningStatus = LearningStatus.NOT_LEARNED,
    /**
     * Progression bucket the word belongs to. Independent from
     * [difficulty]: this gates when the word becomes available
     * inside mini-games.
     */
    val level: Int = 1,
    val favorite: Boolean = false,
    val notes: String = "",
    val reviewCount: Int = 0,
    val lastReview: Long? = null,
    val nextReview: Long? = null,
    val customDifficulty: Difficulty? = null
)