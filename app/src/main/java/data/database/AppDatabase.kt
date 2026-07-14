package data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import data.database.converters.DifficultyConverter
import data.database.converters.ExampleConverter
import data.database.converters.FormsConverter
import data.database.converters.ListStringConverter
import data.database.converters.PronunciationConverter
import data.database.dao.CategoryProgressDao
import data.database.dao.SkillProgressDao
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity
import data.database.entities.CoreWordEntity
import data.database.entities.SkillProgressEntity
import data.database.entities.UserProfileEntity
import data.database.entities.UserWordEntity
import data.database.entities.WordEntity

/**
 * Room database for the English Vault app.
 *
 * Schema overview:
 *  - `core_words` — dictionary entries seeded from the section files
 *    under `assets/dictionary/`.
 *  - `user_words` — entries the learner added through the app.
 *  - `words_view` — read-only UNION ALL of both tables, surfaced as
 *    [WordEntity] so the rest of the app keeps a single data model.
 *  - `user_profile` — single-row table holding XP, level, streak and
 *    daily goal counters introduced in Phase 3.
 *  - `category_progress` — one row per tracked grammatical category
 *    holding XP and unlocked-level counters introduced in Phase 4.6.
 *  - `skill_progress` — one row per language skill (Listening,
 *    Speaking, Reading, Writing) holding cumulative XP introduced in
 *    Phase 7.6.
 *
 * The schema is wired through Hilt in `DatabaseModule`, which provides
 * all DAOs and registers the migrations that take the database from
 * version 1 to version 10.
 */
@Database(
    entities = [
        CoreWordEntity::class,
        UserWordEntity::class,
        UserProfileEntity::class,
        CategoryProgressEntity::class,
        SkillProgressEntity::class
    ],
    views = [
        WordEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(
    DifficultyConverter::class,
    ExampleConverter::class,
    FormsConverter::class,
    ListStringConverter::class,
    PronunciationConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    /** DAO for the `core_words` / `user_words` tables (read paths target `words_view`). */
    abstract fun wordDao(): WordDao

    /** DAO for the single-row `user_profile` table. */
    abstract fun userProfileDao(): UserProfileDao

    /** DAO for the per-category progression table (`category_progress`). */
    abstract fun categoryProgressDao(): CategoryProgressDao

    /** DAO for the four-skill progression table (`skill_progress`). */
    abstract fun skillProgressDao(): SkillProgressDao

    companion object {
        /** Filename used by `Room.databaseBuilder` to create the SQLite file. */
        const val DATABASE_NAME = "english_vault.db"
    }
}