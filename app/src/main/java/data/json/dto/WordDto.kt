package data.json.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects that mirror the structure of `assets/words.json`.
 *
 * DTOs are intentionally kept separate from Room entities so:
 *  - The on-disk JSON schema can evolve without touching Room types.
 *  - Fields that the database ignores (e.g. audio URLs) are not stored
 *    as nullable columns.
 *  - Mapping logic stays centralised in [data.mapper.WordMapper].
 *
 * Field nullability matches the JSON contract: any field that may be
 * missing in some entries is typed as nullable.
 */

// region: WordDto
/**
 * Top-level dictionary entry read from `words.json`.
 *
 * `difficulty` and `source` are kept as `String` here because the JSON
 * contract is free-form; the mapper promotes `difficulty` into the
 * strongly typed `Difficulty` enum with a safe default.
 */
data class WordDto(
    @SerializedName("id") val id: Int,
    @SerializedName("word") val word: String,
    @SerializedName("translation") val translation: String,
    @SerializedName("type") val type: String,
    @SerializedName("regular") val regular: Boolean?,
    @SerializedName("forms") val forms: FormsDto?,
    @SerializedName("pronunciation") val pronunciation: PronunciationDto?,
    @SerializedName("category") val category: List<String>?,
    @SerializedName("synonyms") val synonyms: List<String>?,
    @SerializedName("antonyms") val antonyms: List<String>?,
    @SerializedName("examples") val examples: List<ExampleDto>?,
    @SerializedName("tags") val tags: List<String>?,
    @SerializedName("difficulty") val difficulty: String?,
    @SerializedName("source") val source: String?
)
// endregion

// region: Nested DTOs
/** Verb conjugation table. All members are nullable because non-verbs omit this block. */
data class FormsDto(
    @SerializedName("base") val base: String?,
    @SerializedName("thirdPerson") val thirdPerson: String?,
    @SerializedName("presentParticiple") val presentParticiple: String?,
    @SerializedName("pastSimple") val pastSimple: String?,
    @SerializedName("pastParticiple") val pastParticiple: String?
)

/** IPA transcription plus optional audio asset URL/path. */
data class PronunciationDto(
    @SerializedName("ipa") val ipa: String?,
    @SerializedName("audio") val audio: String?
)

/** Bilingual example sentence tagged with a CEFR level (A1, A2, B1, …). */
data class ExampleDto(
    @SerializedName("english") val english: String,
    @SerializedName("spanish") val spanish: String,
    @SerializedName("level") val level: String
)
// endregion