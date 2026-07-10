package data.mapper

import data.database.entities.Difficulty
import data.database.entities.Example
import data.database.entities.Forms
import data.database.entities.Pronunciation
import data.database.entities.WordEntity
import data.json.dto.WordDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates between JSON-derived [WordDto] objects and Room [WordEntity]
 * rows.
 *
 * Centralising the mapping here keeps the DTO layer free of Room
 * dependencies and ensures the entity layer never leaks Gson types.
 * Every translation rule (default user state, difficulty promotion,
 * nested-object conversion) lives in this file.
 */
@Singleton
class WordMapper @Inject constructor() {

    /**
     * Converts a single [WordDto] into a persistable [WordEntity].
     *
     * User-owned fields are initialised with neutral defaults so newly
     * seeded words appear "fresh" until the user interacts with them.
     *
     * @param dto Source DTO coming from [data.json.loader.JsonLoader].
     * @return A [WordEntity] ready to be passed to `WordDao.insertWords`.
     */
    fun mapToEntity(dto: WordDto): WordEntity = WordEntity(
        id = dto.id,
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
        source = dto.source,
        // region: Default user-state — Phase 3 will let users edit these fields
        favorite = false,
        learned = false,
        notes = "",
        reviewCount = 0,
        lastReview = null,
        nextReview = null,
        customDifficulty = null
        // endregion
    )

    /**
     * Convenience wrapper that maps a list of DTOs.
     *
     * @param dtos Source DTOs.
     * @return Entities ready to be inserted in bulk.
     */
    fun mapToEntityList(dtos: List<WordDto>): List<WordEntity> =
        dtos.map(::mapToEntity)
}