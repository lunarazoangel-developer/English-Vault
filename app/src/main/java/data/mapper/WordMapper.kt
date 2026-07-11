package data.mapper

import data.database.entities.CoreWordEntity
import data.database.entities.Difficulty
import data.database.entities.Example
import data.database.entities.Forms
import data.database.entities.Pronunciation
import data.database.entities.UserWordEntity
import data.json.dto.WordDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates between JSON-derived [WordDto] objects and Room entities.
 *
 * Centralising the mapping here keeps the DTO layer free of Room
 * dependencies and ensures the entity layer never leaks Gson types.
 * Every translation rule (default user state, difficulty promotion,
 * nested-object conversion) lives in this file.
 */
@Singleton
class WordMapper @Inject constructor() {

    /**
     * Converts a single [WordDto] into a persistable [CoreWordEntity].
     *
     * User-owned fields are initialised with neutral defaults so newly
     * seeded words appear "fresh" until the user interacts with them.
     *
     * The [CoreWordEntity.id] is forced to `0` so SQLite's AUTOINCREMENT
     * picks the next free value in the `core_words` sequence. This
     * isolates dictionary ids from any value coming out of the source
     * JSON and prevents future JSON ids from colliding with rows in
     * `user_words`.
     *
     * @param dto Source DTO coming from [data.json.loader.JsonLoader].
     * @return A [CoreWordEntity] ready to be passed to
     *   `WordDao.insertCoreWords`.
     */
    fun mapToCoreEntity(dto: WordDto): CoreWordEntity = CoreWordEntity(
        id = 0,
        word = dto.word,
        translation = dto.translation,
        type = dto.type,
        regular = dto.regular,
        forms = dto.forms?.let { f ->
            Forms(
                base = f.base,
                thirdPerson = f.thirdPerson,
                presentParticiple = f.presentParticiple,
                pastSimple = f.pastSimple,
                pastParticiple = f.pastParticiple
            )
        },
        pronunciation = dto.pronunciation?.let { p ->
            Pronunciation(
                ipa = p.ipa,
                audio = p.audio
            )
        },
        category = dto.category,
        synonyms = dto.synonyms,
        antonyms = dto.antonyms,
        examples = dto.examples?.map { e ->
            Example(
                english = e.english,
                spanish = e.spanish,
                level = e.level
            )
        },
        tags = dto.tags,
        // Difficulty is free-form in JSON; promote it to the enum and
        // fall back to MEDIUM for unknown values.
        difficulty = Difficulty.fromStringOrDefault(dto.difficulty),
        // Level flows through from JSON; defaults to 1 if the asset
        // ever ships an entry without the field.
        level = dto.level.coerceAtLeast(1),
        // region: Default user-state — Phase 3 will let users edit these fields
        status = data.database.entities.LearningStatus.NOT_LEARNED,
        favorite = false,
        notes = "",
        reviewCount = 0,
        lastReview = null,
        nextReview = null,
        customDifficulty = null
        // endregion
    )

    /**
     * Converts a [WordDto] into a [UserWordEntity]. Kept for tooling
     * or future import flows that might seed user data directly; the
     * normal UI path builds [UserWordEntity] through
     * [data.database.entities.toUserEntity].
     *
     * @param dto Source DTO.
     * @return A [UserWordEntity] with `id = 0` so AUTOINCREMENT picks
     *   the next free value in the `user_words` sequence.
     */
    fun mapToUserEntity(dto: WordDto): UserWordEntity = UserWordEntity(
        id = 0,
        word = dto.word,
        translation = dto.translation,
        type = dto.type,
        regular = dto.regular,
        forms = dto.forms?.let { f ->
            Forms(
                base = f.base,
                thirdPerson = f.thirdPerson,
                presentParticiple = f.presentParticiple,
                pastSimple = f.pastSimple,
                pastParticiple = f.pastParticiple
            )
        },
        pronunciation = dto.pronunciation?.let { p ->
            Pronunciation(
                ipa = p.ipa,
                audio = p.audio
            )
        },
        category = dto.category,
        synonyms = dto.synonyms,
        antonyms = dto.antonyms,
        examples = dto.examples?.map { e ->
            Example(
                english = e.english,
                spanish = e.spanish,
                level = e.level
            )
        },
        tags = dto.tags,
        difficulty = Difficulty.fromStringOrDefault(dto.difficulty),
        level = dto.level.coerceAtLeast(1),
        status = data.database.entities.LearningStatus.NOT_LEARNED,
        favorite = false,
        notes = "",
        reviewCount = 0,
        lastReview = null,
        nextReview = null,
        customDifficulty = null
    )

    /**
     * Convenience wrapper that maps a list of DTOs into core entities.
     *
     * @param dtos Source DTOs.
     * @return Core entities ready for `WordDao.insertCoreWords`.
     */
    fun mapToCoreEntityList(dtos: List<WordDto>): List<CoreWordEntity> =
        dtos.map(::mapToCoreEntity)
}