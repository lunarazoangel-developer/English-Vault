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
 * Converts a [WordEntity] coming from the view into a fresh
 * [UserWordEntity] ready to be inserted via `WordDao.insertUserWord`.
 *
 * The `source` column is dropped because it is a view-projected
 * literal, and `id` is forced to `0` so SQLite's AUTOINCREMENT
 * sequence assigns the next free value in the `user_words` table.
 * All user-state columns are preserved so the caller's `favorite`,
 * `learned`, `notes`, `reviewCount`, `lastReview`, `nextReview` and
 * `customDifficulty` are not lost in translation.
 *
 * @return A [UserWordEntity] ready for persistence.
 */
fun WordEntity.toUserEntity(): UserWordEntity = UserWordEntity(
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
    favorite = favorite,
    learned = learned,
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
    favorite = favorite,
    learned = learned,
    notes = notes,
    reviewCount = reviewCount,
    lastReview = lastReview,
    nextReview = nextReview,
    customDifficulty = customDifficulty
)
// endregion