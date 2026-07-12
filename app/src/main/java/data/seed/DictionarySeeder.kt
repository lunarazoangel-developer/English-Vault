package data.seed

import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.UserProfileEntity
import data.json.loader.JsonLoader
import data.mapper.WordMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports the bundled core dictionary into `core_words` when the
 * device's stored version lags behind the bundled version.
 *
 * The seeder is intentionally idempotent: it short-circuits when
 * the profile already records the current
 * [CORE_DICTIONARY_VERSION], so it is safe to call on every app
 * launch. When a bump is detected it wipes `core_words` (preserving
 * the contents of `user_words` and the user's learning state) and
 * re-imports every section file under `assets/dictionary/`
 * (concatenated in load order by [JsonLoader]).
 *
 * Provided as a Hilt singleton so `MainActivity` can inject it and
 * the seeding logic stays out of the UI layer.
 */
@Singleton
class DictionarySeeder @Inject constructor(
    private val wordDao: WordDao,
    private val userProfileDao: UserProfileDao,
    private val jsonLoader: JsonLoader,
    private val wordMapper: WordMapper
) {

    /**
     * Re-seeds `core_words` from the bundled `assets/dictionary/`
     * section files when the bundled version is newer than what the
     * profile records.
     *
     * Caller is responsible for ensuring the profile row exists
     * before invoking this method; when the profile is missing the
     * seeder returns without doing anything (the bootstrap in
     * `MainActivity` is expected to create it on the same launch).
     */
    suspend fun seedIfNeeded() {
        val profile = userProfileDao.getProfile() ?: return
        if (profile.coreDictionaryVersion >= CORE_DICTIONARY_VERSION) return

        val dtos = jsonLoader.loadWords()
        // Wipe core entries first so any id the JSON declares (we no
        // longer carry the JSON id through, but defensively) or any
        // leftover state on previous core rows is dropped. User-added
        // rows in `user_words` are unaffected because they live in a
        // separate table with its own AUTOINCREMENT sequence.
        wordDao.deleteAllCoreWords()
        wordDao.insertCoreWords(wordMapper.mapToCoreEntityList(dtos))
        userProfileDao.upsertProfile(
            profile.copy(coreDictionaryVersion = CORE_DICTIONARY_VERSION)
        )
    }

    /**
     * Forces a re-seed even when the stored version already matches
     * the bundled one. Intended for debug / Settings flows where the
     * user wants to refresh the dictionary explicitly.
     */
    suspend fun forceReseed() {
        val profile = userProfileDao.getProfile() ?: return
        val dtos = jsonLoader.loadWords()
        wordDao.deleteAllCoreWords()
        wordDao.insertCoreWords(wordMapper.mapToCoreEntityList(dtos))
        userProfileDao.upsertProfile(
            profile.copy(coreDictionaryVersion = CORE_DICTIONARY_VERSION)
        )
    }

    companion object {
        /**
         * Version of the bundled dictionary shipped with this APK.
         *
         * Bump this whenever anything under `assets/dictionary/`
         * changes (entries added, removed, edited, or section files
         * added/removed); the seeder compares it against the value
         * persisted in [UserProfileEntity.coreDictionaryVersion] and
         * re-imports the JSON when they differ.
         *
         * History:
         *  - 1 — implicit, 10 entries (never explicitly tracked).
         *  - 2 — 68 entries with rich examples / categories / tags.
         *  - 3 — same 68 entries, now distributed across two
         *    `level` buckets (34 in level 1, 34 in level 2) for the
         *    upcoming mini-games progression mechanic.
         *  - 4 — same 68 entries, split across eight per-type
         *    section files under `assets/dictionary/` with a
         *    `README.md` index. Per-entry content unchanged except
         *    `go` (4 → 3 examples for cross-entry consistency).
         *  - 5, 6, 7 — additional schema / migration bumps; total
         *    stays at 68 entries.
         *  - 8 — `conjunctions.json` extended from 2 to 62 entries
         *    (coordinating, subordinating time / condition /
         *    concession / cause / purpose / result / comparison /
         *    manner / place, correlative pairs, conjunctive
         *    adverbs). Total dictionary now 128 entries across the
         *    eight per-type section files.
         *  - 9 — `interjections.json` extended from 7 to 67 entries
         *    (greetings, polite markers, affirmation / negation,
         *    surprise, joy, frustration, pain, attention getters,
         *    hesitation fillers, discourse markers). Total
         *    dictionary now 193 entries across the eight per-type
         *    section files.
         *  - 10 — `nouns.json` extended from 9 to 69 entries
         *    (people / family, body parts, time, food, animals,
         *    home / furniture, places, common objects, abstract /
         *    communication, nature, education, work). Total
         *    dictionary now 262 entries across the eight per-type
         *    section files.
         *  - 11 — `prepositions.json` extended from 2 to 62 entries
         *    (place, time, direction, manner, possession, and
         *    common multi-word prepositions). Total dictionary now
         *    324 entries across the eight per-type section files.
         *  - 12 — `adjectives.json` extended from 87 to 147 entries
         *    (colors, taste, more emotions, personality, weather,
         *    time / state, abstract qualities, and physical
         *    descriptors). Total dictionary now 459 entries across
         *    the eight per-type section files.
         *  - 13 — `adverbs.json` extended from 105 to 165 entries
         *    (linking, frequency / period, direction, place,
         *    degree, certainty, and more manner descriptors).
         *    Total dictionary now 624 entries across the eight
         *    per-type section files.
         */
        const val CORE_DICTIONARY_VERSION: Int = 13
    }
}