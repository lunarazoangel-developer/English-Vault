package data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities for the English Vault data layer.
 *
 * This file bundles every Room-managed type used by the dictionary:
 *  - [WordEntity] — the root aggregate that owns dictionary fields plus
 *    the user's learning state.
 *  - [Forms], [Pronunciation], [Example] — nested value classes persisted
 *    via dedicated [androidx.room.TypeConverter]s.
 *  - [Difficulty] — enum shared by the entity and the converter layer.
 *
 * Phase 1 keeps the schema intentionally flat so it can be persisted with
 * a single table and a handful of converters; no relations are required.
 */

// region: WordEntity
/**
 * Room entity that represents a single dictionary word along with the
 * learning state owned by the user.
 *
 * The primary key ([id]) matches the source JSON so the same identifier
 * can be used during seeding and later when synchronising remote data.
 *
 * Dictionary fields are immutable for a given word; the trailing fields
 * (`favorite`, `learned`, `notes`, `reviewCount`, `lastReview`,
 * `nextReview`, `customDifficulty`) are user-owned and mutated by the
 * feature layer (Phase 3+).
 */
@Entity(tableName = "words")
data class WordEntity(
    /**
     * Auto-generated primary key.
     *
     * Seeding from `assets/words.json` leaves this default value; Room
     * assigns sequential ids (1, 2, …). User-added words inserted via
     * the form also rely on the default and get the next id, which
     * keeps the table stable across re-imports.
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
    val source: String?,
    val favorite: Boolean = false,
    val learned: Boolean = false,
    val notes: String = "",
    val reviewCount: Int = 0,
    val lastReview: Long? = null,
    val nextReview: Long? = null,
    val customDifficulty: Difficulty? = null
) {
    companion object {
        /** Source value used for entries seeded from `assets/words.json`. */
        const val SOURCE_CORE: String = "core"

        /** Source value used for entries the user added through the app. */
        const val SOURCE_USER: String = "user"
    }
}

/**
 * Returns `true` when the word was added by the user rather than seeded
 * from the bundled dictionary asset.
 *
 * Used by the UI to gate destructive actions (delete) on user-owned rows
 * and by the future DAO layer to enforce the same restriction at the
 * data tier.
 */
fun WordEntity.isUserAdded(): Boolean = source == WordEntity.SOURCE_USER
// endregion

// region: Nested value classes
/** Verb conjugation forms; nullable because adjectives/nouns do not have them. */
data class Forms(
    val base: String?,
    val thirdPerson: String?,
    val presentParticiple: String?,
    val pastSimple: String?,
    val pastParticiple: String?
)

/** IPA transcription plus optional audio asset reference. */
data class Pronunciation(
    val ipa: String?,
    val audio: String?
)

/** Bilingual example sentence tagged with a CEFR level (A1, A2, B1, …). */
data class Example(
    val english: String,
    val spanish: String,
    val level: String
)
// endregion

// region: Difficulty enum
/**
 * Coarse learning difficulty used for both the dictionary baseline
 * ([WordEntity.difficulty]) and the user-defined override
 * ([WordEntity.customDifficulty]).
 */
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD;

    companion object {
        /**
         * Parses a [String] into a [Difficulty].
         *
         * @param value Case-insensitive enum name, or null.
         * @return The matching [Difficulty], or null when the value is
         *   null or not part of the enum.
         */
        fun fromStringOrNull(value: String?): Difficulty? =
            value?.let { runCatching { valueOf(it.uppercase()) }.getOrNull() }

        /**
         * Same as [fromStringOrNull] but falls back to [default] when the
         * value cannot be parsed. Useful when seeding legacy data where
         * unknown difficulty labels should not crash the import.
         *
         * @param value Raw string coming from JSON or the database.
         * @param default Difficulty to use when [value] is unrecognised.
         */
        fun fromStringOrDefault(value: String?, default: Difficulty = MEDIUM): Difficulty =
            fromStringOrNull(value) ?: default
    }
}
// endregion