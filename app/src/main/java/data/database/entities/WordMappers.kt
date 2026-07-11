package data.database.entities

/**
 * Bridges between the read model ([WordEntity]) and the write entities
 * ([CoreWordEntity] / [UserWordEntity]).
 *
 * The UI layer consumes [WordEntity] through the `words_view` UNION,
 * but persistence requires one of the table-bound entities. These
 * extensions centralise the conversion so callers never have to map
 * fields by hand.
 */

// region: WordEntity → UserWordEntity
/**
 * Converts a [WordEntity] coming from the view into a [UserWordEntity]
 * ready to be persisted via `WordDao.insertUserWord`.
 *
 * The `source` column is dropped because it is a view-projected
 * literal. The id defaults to `0` so SQLite's AUTOINCREMENT sequence
 * assigns the next free value in the `user_words` table — callers
 * passing a new word should leave the default in place.
 *
 * When [preserveId] is `true` (used by the edit flow), the original
 * id is kept so `OnConflictStrategy.REPLACE` updates the existing
 * row instead of inserting a new one.
 *
 * All user-state columns (`status`, `level`, `favorite`, `notes`,
 * `reviewCount`, `lastReview`, `nextReview`, `customDifficulty`) are
 * preserved so the caller's progress is not lost in translation.
 *
 * @param preserveId When `true`, keep the source entity's id; when
 *   `false` (the default), reset it to `0` so AUTOINCREMENT assigns a
 *   fresh value.
 * @return A [UserWordEntity] ready for persistence.
 */
fun WordEntity.toUserEntity(preserveId: Boolean = false): UserWordEntity = UserWordEntity(
    id = if (preserveId) id else 0,
    word = word,
    translation = translation,
    type = type,
    regular = regular,
    forms = forms,
    pronunciation = pronunciation,
    category = category,
    synonyms = synonyms,
    antonyms = antonyms,
    examples = examples,
    tags = tags,
    difficulty = difficulty,
    status = status,
    level = level,
    favorite = favorite,
    notes = notes,
    reviewCount = reviewCount,
    lastReview = lastReview,
    nextReview = nextReview,
    customDifficulty = customDifficulty
)
// endregion

// region: WordEntity → CoreWordEntity
/**
 * Converts a [WordEntity] into a [CoreWordEntity] for re-import into
 * the seed table. Rarely used outside of tooling or migration scripts
 * because seeding normally flows from [data.json.dto.WordDto] via
 * [data.mapper.WordMapper].
 *
 * `id` is reset to `0` so AUTOINCREMENT picks a fresh value, avoiding
 * collisions with rows already present in `core_words`.
 *
 * @return A [CoreWordEntity] ready for persistence.
 */
fun WordEntity.toCoreEntity(): CoreWordEntity = CoreWordEntity(
    id = 0,
    word = word,
    translation = translation,
    type = type,
    regular = regular,
    forms = forms,
    pronunciation = pronunciation,
    category = category,
    synonyms = synonyms,
    antonyms = antonyms,
    examples = examples,
    tags = tags,
    difficulty = difficulty,
    status = status,
    level = level,
    favorite = favorite,
    notes = notes,
    reviewCount = reviewCount,
    lastReview = lastReview,
    nextReview = nextReview,
    customDifficulty = customDifficulty
)
// endregion