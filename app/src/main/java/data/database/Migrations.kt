package data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for the English Vault database.
 *
 * Each migration is a forward-only script that brings an older schema
 * to a newer version. Every time [AppDatabase] bumps its `version`
 * constant a new entry must be added here so existing installs keep
 * their data instead of being wiped by a destructive fallback.
 *
 * Conventions:
 *  - Migration constants follow the `MIGRATION_<from>_<to>` pattern.
 *  - Migrations use plain SQL via `db.execSQL`; KSP does not generate
 *    any code for them so they cannot use entity references.
 *  - Migrations must be idempotent-friendly: every `CREATE TABLE`
 *    uses `IF NOT EXISTS` and every column add uses SQLite's
 *    `ALTER TABLE ADD COLUMN` with safe defaults.
 */
object Migrations {

    /**
     * Phase 3 — introduces the single-row `user_profile` table.
     *
     * The column types mirror [data.database.entities.UserProfileEntity]:
     *  - `id INTEGER PRIMARY KEY` (fixed to 1, single-user schema)
     *  - `name TEXT NOT NULL`
     *  - `totalXp INTEGER NOT NULL DEFAULT 0`
     *  - `streakDays INTEGER NOT NULL DEFAULT 0`
     *  - `lastStreakDate INTEGER` (nullable epoch millis)
     *  - `dailyGoalXp INTEGER NOT NULL DEFAULT 50`
     *  - `createdAt INTEGER NOT NULL` (epoch millis)
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_profile` (
                    `id` INTEGER NOT NULL PRIMARY KEY,
                    `name` TEXT NOT NULL,
                    `totalXp` INTEGER NOT NULL DEFAULT 0,
                    `streakDays` INTEGER NOT NULL DEFAULT 0,
                    `lastStreakDate` INTEGER,
                    `dailyGoalXp` INTEGER NOT NULL DEFAULT 50,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    /**
     * Phase 2.5 — switches `words.id` to AUTOINCREMENT.
     *
     * Background: in schema v1/v2 the id column came from the source
     * JSON (1-10 for the seed) so user-added words had no obvious
     * place to live. v3 makes the primary key auto-generated so:
     *  - Seed JSON keeps working: `insertWords` is called with id=0 in
     *    each row, Room allocates 1, 2, 3, … in insertion order.
     *  - The Words screen form can build a [data.database.entities.WordEntity]
     *    with the default id and Room assigns the next free number.
     *
     * This migration recreates the table because SQLite cannot alter
     * an existing column to add `AUTOINCREMENT`. Existing rows are
     * intentionally dropped: `MainActivity` re-imports the seed JSON
     * on every fresh launch, so the dictionary is recoverable without
     * data loss for the user.
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `words`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `words` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                    `word` TEXT NOT NULL,
                    `translation` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `regular` INTEGER,
                    `forms` TEXT,
                    `pronunciation` TEXT,
                    `category` TEXT,
                    `synonyms` TEXT,
                    `antonyms` TEXT,
                    `examples` TEXT,
                    `tags` TEXT,
                    `difficulty` TEXT NOT NULL,
                    `source` TEXT,
                    `favorite` INTEGER NOT NULL DEFAULT 0,
                    `learned` INTEGER NOT NULL DEFAULT 0,
                    `notes` TEXT NOT NULL DEFAULT '',
                    `reviewCount` INTEGER NOT NULL DEFAULT 0,
                    `lastReview` INTEGER,
                    `nextReview` INTEGER,
                    `customDifficulty` TEXT
                )
                """.trimIndent()
            )
        }
    }
}