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
 * re-imports `assets/words.json` through the existing mapper.
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
     * Re-seeds `core_words` from `assets/words.json` when the bundled
     * version is newer than what the profile records.
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
         * Bump this whenever `assets/words.json` changes; the seeder
         * compares it against the value persisted in
         * [UserProfileEntity.coreDictionaryVersion] and re-imports
         * the JSON when they differ.
         *
         * History:
         *  - 1 — implicit, 10 entries (never explicitly tracked).
         *  - 2 — 68 entries with rich examples / categories / tags.
         */
        const val CORE_DICTIONARY_VERSION: Int = 2
    }
}