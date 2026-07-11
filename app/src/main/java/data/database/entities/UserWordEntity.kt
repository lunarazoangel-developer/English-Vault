package data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity backing the `user_words` table.
 *
 * This table holds entries the learner added through the
 * `WordFormScreen`. Rows here are fully mutable: the DAO exposes
 * insert, update and delete operations against this table so the
 * user can curate their personal vocabulary.
 *
 * Schema parity with [CoreWordEntity] is intentional: the
 * `words_view` UNIONs both tables into a single read model
 * ([WordEntity]) and requires identical column lists and types.
 */
@Entity(tableName = "user_words")
data class UserWordEntity(
    /**
     * Auto-generated primary key. The form always sends `id = 0`
     * so SQLite's AUTOINCREMENT picks the next free value. This
     * sequence is independent from `core_words`, so dictionary
     * updates can never overwrite a user-added row.
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
    val favorite: Boolean = false,
    val learned: Boolean = false,
    val notes: String = "",
    val reviewCount: Int = 0,
    val lastReview: Long? = null,
    val nextReview: Long? = null,
    val customDifficulty: Difficulty? = null
)