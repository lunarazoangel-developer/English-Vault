package data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import data.database.entities.WORDS_VIEW_BODY

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
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
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
                    `favorite` INTEGER NOT NULL,
                    `learned` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `reviewCount` INTEGER NOT NULL,
                    `lastReview` INTEGER,
                    `nextReview` INTEGER,
                    `customDifficulty` TEXT
                )
                """.trimIndent()
            )
        }
    }

    /**
     * Phase 4 — splits the monolithic `words` table into two siblings
     * (`core_words` and `user_words`) and adds a `words_view` UNION so
     * the rest of the app keeps reading a single combined model.
     *
     * Background: with a single table, the JSON seed id range (1..N)
     * and the AUTOINCREMENT ids assigned to user-added rows shared
     * the same numeric space. A JSON update adding a new word with an
     * id that happened to match an existing user-added row would
     * silently overwrite it via `OnConflictStrategy.REPLACE`. Splitting
     * the table isolates the two id sequences: even if a future seed
     * picks id=11, the user-added row at id=11 lives in `user_words`
     * and never collides.
     *
     * Steps performed by this migration (all idempotent-friendly):
     *  1. Create both new tables with identical schemas (no `source`
     *     column; the discriminator is computed by the view).
     *  2. Copy rows where `source = 'core'` into `core_words`.
     *  3. Copy rows where `source = 'user'` into `user_words`.
     *  4. Drop the legacy `words` table.
     *  5. Create the `words_view` UNION ALL view.
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create both tables with the unified column layout.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `core_words` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
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
                    `favorite` INTEGER NOT NULL,
                    `learned` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `reviewCount` INTEGER NOT NULL,
                    `lastReview` INTEGER,
                    `nextReview` INTEGER,
                    `customDifficulty` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_words` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
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
                    `favorite` INTEGER NOT NULL,
                    `learned` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `reviewCount` INTEGER NOT NULL,
                    `lastReview` INTEGER,
                    `nextReview` INTEGER,
                    `customDifficulty` TEXT
                )
                """.trimIndent()
            )

            // 2. Copy rows that were marked as core in the old table.
            db.execSQL(
                """
                INSERT INTO `core_words` (
                    `id`, `word`, `translation`, `type`, `regular`, `forms`,
                    `pronunciation`, `category`, `synonyms`, `antonyms`,
                    `examples`, `tags`, `difficulty`, `favorite`, `learned`,
                    `notes`, `reviewCount`, `lastReview`, `nextReview`,
                    `customDifficulty`
                )
                SELECT
                    `id`, `word`, `translation`, `type`, `regular`, `forms`,
                    `pronunciation`, `category`, `synonyms`, `antonyms`,
                    `examples`, `tags`, `difficulty`, `favorite`, `learned`,
                    `notes`, `reviewCount`, `lastReview`, `nextReview`,
                    `customDifficulty`
                FROM `words`
                WHERE `source` = 'core'
                """.trimIndent()
            )

            // 3. Copy rows that were marked as user in the old table.
            db.execSQL(
                """
                INSERT INTO `user_words` (
                    `id`, `word`, `translation`, `type`, `regular`, `forms`,
                    `pronunciation`, `category`, `synonyms`, `antonyms`,
                    `examples`, `tags`, `difficulty`, `favorite`, `learned`,
                    `notes`, `reviewCount`, `lastReview`, `nextReview`,
                    `customDifficulty`
                )
                SELECT
                    `id`, `word`, `translation`, `type`, `regular`, `forms`,
                    `pronunciation`, `category`, `synonyms`, `antonyms`,
                    `examples`, `tags`, `difficulty`, `favorite`, `learned`,
                    `notes`, `reviewCount`, `lastReview`, `nextReview`,
                    `customDifficulty`
                FROM `words`
                WHERE `source` = 'user'
                """.trimIndent()
            )

            // 4. Drop the legacy table now that both halves are split.
            db.execSQL("DROP TABLE IF EXISTS `words`")

            // 5. Recreate the unified read view. The SELECT body must be
            //    byte-for-byte identical to what the `@DatabaseView`
            //    annotation on `WordEntity` declares, otherwise Room's
            //    schema validation rejects the migration with
            //    "Migration didn't properly handle words_view". The body
            //    is shared through [data.database.entities.WORDS_VIEW_BODY].
            db.execSQL("CREATE VIEW `words_view` AS $WORDS_VIEW_BODY")
        }
    }

    /**
     * Phase 4.5 — seeds the `coreDictionaryVersion` column on
     * `user_profile` so [data.seed.DictionarySeeder] can detect when
     * the bundled dictionary has been updated.
     *
     * The column is added with a `DEFAULT 0`, which fills any
     * pre-existing profile row. Combined with the seeder's rule
     * "stored < bundled → re-seed", this guarantees that any
     * existing install triggers a one-time re-seed when the app is
     * first launched after the upgrade, regardless of which version
     * of `core_words` they were sitting on.
     *
     * User-added words in `user_words` are untouched by this
     * migration; only the `user_profile` schema changes.
     */
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `user_profile` ADD COLUMN `coreDictionaryVersion` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * Phase 5.5 — replaces the legacy `learned: Boolean` column with
     * a tri-state `status: LearningStatus` and adds a `level: Int`
     * column to both `core_words` and `user_words`.
     *
     * SQLite < 3.35 does not support `DROP COLUMN`, so each table is
     * recreated from scratch: a `_new` table with the target schema
     * is created, rows are copied with the boolean `learned` value
     * translated into the [LearningStatus] literal, the legacy table
     * is dropped, and the new one is renamed back. `level` defaults
     * to `1` for every existing row.
     *
     * The `words_view` is dropped and recreated from
     * [data.database.entities.WORDS_VIEW_BODY], which now projects
     * `status` and `level` instead of `learned`.
     *
     * User-added words in `user_words` are preserved with their
     * `favorite`, `notes`, `reviewCount`, etc. The boolean `learned`
     * field is translated as follows: `true → 'LEARNED'`,
     * `false → 'NOT_LEARNED'`. The intermediate `ALMOST` state is
     * unreachable from the v5 schema, so any pre-existing row that
     * was marked `learned = false` simply starts at `NOT_LEARNED`.
     */
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateWordsTable(
                db = db,
                legacyTable = "core_words",
                newTable = "core_words_new"
            )
            migrateWordsTable(
                db = db,
                legacyTable = "user_words",
                newTable = "user_words_new"
            )

            // Recreate the unified read view against the new schema.
            db.execSQL("DROP VIEW IF EXISTS `words_view`")
            db.execSQL("CREATE VIEW `words_view` AS $WORDS_VIEW_BODY")
        }

        /**
         * Recreates a single words table with the v6 schema. The
         * SQL is shared between `core_words` and `user_words` because
         * both tables have the same column layout.
         */
        private fun migrateWordsTable(
            db: SupportSQLiteDatabase,
            legacyTable: String,
            newTable: String
        ) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `$newTable` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
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
                    `status` TEXT NOT NULL DEFAULT 'NOT_LEARNED',
                    `level` INTEGER NOT NULL DEFAULT 1,
                    `favorite` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `reviewCount` INTEGER NOT NULL,
                    `lastReview` INTEGER,
                    `nextReview` INTEGER,
                    `customDifficulty` TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `$newTable` (
                    `id`, `word`, `translation`, `type`, `regular`, `forms`,
                    `pronunciation`, `category`, `synonyms`, `antonyms`,
                    `examples`, `tags`, `difficulty`, `status`, `level`,
                    `favorite`, `notes`, `reviewCount`, `lastReview`,
                    `nextReview`, `customDifficulty`
                )
                SELECT
                    `id`, `word`, `translation`, `type`, `regular`, `forms`,
                    `pronunciation`, `category`, `synonyms`, `antonyms`,
                    `examples`, `tags`, `difficulty`,
                    CASE WHEN `learned` = 1 THEN 'LEARNED' ELSE 'NOT_LEARNED' END,
                    1,
                    `favorite`, `notes`, `reviewCount`, `lastReview`,
                    `nextReview`, `customDifficulty`
                FROM `$legacyTable`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `$legacyTable`")
            db.execSQL("ALTER TABLE `$newTable` RENAME TO `$legacyTable`")
        }
    }

    /**
     * Phase 4.6 — adds the `category_progress` table that holds one
     * row per tracked grammatical category with the cumulative XP
     * earned, the highest unlocked level and the XP accumulated
     * since the last promotion.
     *
     * The schema is created fresh (`CREATE TABLE IF NOT EXISTS`) so
     * the migration is a no-op for installs that already have the
     * table from a previous (development) install. Initial rows are
     * inserted with `INSERT OR IGNORE` so re-running the migration
     * never duplicates or overwrites data.
     *
     * The list of tracked categories must stay in sync with
     * `com.example.englishvault.ui.words.WordTypeFilter.TRACKED`.
     * Hardcoding the keys here keeps the migration file standalone
     * and Room-friendly (it can only read SQL, not Kotlin constants).
     */
    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `category_progress` (
                    `categoryKey` TEXT NOT NULL PRIMARY KEY,
                    `xpTotal` INTEGER NOT NULL DEFAULT 0,
                    `unlockedLevel` INTEGER NOT NULL DEFAULT 1,
                    `xpSinceLevelUp` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            // Seed one row per tracked category. The literal list
            // mirrors `WordTypeFilter.TRACKED` so the gating logic
            // never observes a missing key.
            val trackedKeys = listOf(
                "VERBS_REGULAR",
                "VERBS_IRREGULAR",
                "ADJECTIVES",
                "ADVERBS",
                "NOUNS",
                "CONJUNCTIONS",
                "PREPOSITIONS",
                "INTERJECTIONS"
            )
            for (key in trackedKeys) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `category_progress` " +
                        "(`categoryKey`, `xpTotal`, `unlockedLevel`, `xpSinceLevelUp`, `updatedAt`) " +
                        "VALUES ('$key', 0, 1, 0, ${System.currentTimeMillis()})"
                )
            }
        }
    }
}