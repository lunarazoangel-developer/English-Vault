package data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import data.database.converters.DifficultyConverter
import data.database.converters.ExampleConverter
import data.database.converters.FormsConverter
import data.database.converters.ListStringConverter
import data.database.converters.PronunciationConverter
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.UserProfileEntity
import data.database.entities.WordEntity

/**
 * Room database for the English Vault app.
 *
 * Schema overview:
 *  - `words` — dictionary entries plus user-owned learning state.
 *  - `user_profile` — single-row table holding XP, level, streak and
 *    daily goal counters introduced in Phase 3.
 *
 * The schema is wired through Hilt in `DatabaseModule`, which provides
 * both DAOs and registers the migration that takes the database from
 * version 1 to version 2.
 */
@Database(
    entities = [
        WordEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
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
    /** DAO for the `words` table. */
    abstract fun wordDao(): WordDao

    /** DAO for the single-row `user_profile` table. */
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        /** Filename used by `Room.databaseBuilder` to create the SQLite file. */
        const val DATABASE_NAME = "english_vault.db"
    }
}