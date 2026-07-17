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

    /**
     * Phase 7 — adds the player's `hearts` and `coins` counters to
     * `user_profile`. The world map HUD (Phase 7) renders both; the
     * values default to 5 hearts and 0 coins so freshly migrated
     * installs keep a balanced starting state.
     *
     * SQLite requires one `ALTER TABLE ADD COLUMN` per statement, so
     * each column gets its own `execSQL` call.
     */
    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `user_profile` ADD COLUMN `hearts` INTEGER NOT NULL DEFAULT 5"
            )
            db.execSQL(
                "ALTER TABLE `user_profile` ADD COLUMN `coins` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * Phase 7.1 — adds the music and effects volume sliders to
     * `user_profile` so the Settings screen can persist preferences
     * before the audio engine itself ships.
     *
     * Both columns use `REAL` (SQLite's floating-point type) and
     * default to `1.0` so freshly migrated installs keep full volume.
     */
    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `user_profile` ADD COLUMN `musicVolume` REAL NOT NULL DEFAULT 1.0"
            )
            db.execSQL(
                "ALTER TABLE `user_profile` ADD COLUMN `effectsVolume` REAL NOT NULL DEFAULT 1.0"
            )
        }
    }

    /**
     * Phase 7.6 — introduces the `skill_progress` table that holds
     * one row per language skill (Listening, Speaking, Reading,
     * Writing) with the cumulative XP earned in each.
     *
     * The schema is created fresh (`CREATE TABLE IF NOT EXISTS`) so
     * the migration is a no-op for installs that already have the
     * table from a previous (development) install. Initial rows are
     * inserted with `INSERT OR IGNORE` so re-running the migration
     * never duplicates or overwrites data.
     *
     * The list of skill keys must stay in sync with
     * `data.database.entities.Skill`. Hardcoding the keys here keeps
     * the migration file standalone and Room-friendly (it can only
     * read SQL, not Kotlin constants).
     */
    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `skill_progress` (
                    `skillKey` TEXT NOT NULL PRIMARY KEY,
                    `xpTotal` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            // Seed one row per skill. The literal list mirrors
            // `Skill.ALL` so the UI never observes a missing key.
            val skillKeys = listOf(
                "LISTENING",
                "SPEAKING",
                "READING",
                "WRITING",
                "GRAMMAR"
            )
            for (key in skillKeys) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `skill_progress` " +
                        "(`skillKey`, `xpTotal`, `updatedAt`) " +
                        "VALUES ('$key', 0, ${System.currentTimeMillis()})"
                )
            }
        }
    }

    /**
     * Phase 7.15 — adds the `consecutiveCorrect` counter to both
     * `core_words` and `user_words`. Drives the auto-marking
     * feature: every correct answer in a mini-game bumps this
     * counter on the corresponding `WordEntity`, every wrong
     * answer resets it to `0`, and
     * [data.game.AutoStatusEvaluator] maps the value to a
     * [data.database.entities.LearningStatus] (`>=1 → ALMOST`,
     * `>=3 → LEARNED`) without ever downgrading a manual mark.
     *
     * `lastReview` is also touched by the same DAO write so the
     * existing "last reviewed N hours ago" copy on the Words
     * screen keeps reflecting actual mini-game activity.
     *
     * The column defaults to `0` so every existing row keeps its
     * current `status` — the auto system simply has nothing to
     * evaluate until the user plays a mini-game.
     *
     * The `words_view` is dropped and recreated so the new column
     * is projectable from the view (Room's `Schema` validator
     * would otherwise complain about the projection list not
     * matching the entity). The new SQL is the same string the
     * `@DatabaseView` annotation now declares on `WordEntity` —
     * see [data.database.entities.WORDS_VIEW_BODY].
     */
    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `core_words` ADD COLUMN `consecutiveCorrect` INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE `user_words` ADD COLUMN `consecutiveCorrect` INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL("DROP VIEW IF EXISTS `words_view`")
            db.execSQL("CREATE VIEW `words_view` AS $WORDS_VIEW_BODY")
        }
    }

    /**
     * Phase 8.x — theme toggle.
     *
     * Adds the `themeMode` column to `user_profile` so the Settings
     * screen can persist the user's choice between the dark and the
     * light color scheme. Defaults to `'DARK'` (the design the app
     * is currently being iterated on) so existing installs land in
     * dark mode after the upgrade.
     *
     * Allowed values are kept in [data.database.entities.UserProfileEntity]:
     * `THEME_MODE_DARK` and `THEME_MODE_LIGHT`. The DAO accepts any
     * string but the Settings UI only writes those two.
     */
    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `user_profile` ADD COLUMN `themeMode` TEXT NOT NULL DEFAULT 'DARK'"
            )
        }
    }

    /**
     * Phase 7.x — synthetic-bucket hybrid gate.
     *
     * Adds the `game_covered_words` table that backs the coverage
     * half of the new hybrid promotion gate for the `LETTER_SOUP`
     * and `LISTENING` synthetic buckets. Each row records a single
     * `(categoryKey, wordId, level)` triple so the
     * [data.database.dao.GameCoveredWordsDao] can dedupe coverage
     * automatically with `INSERT OR IGNORE` and count distinct
     * covered words per level in O(1) (`SELECT COUNT(*)` over an
     * indexed PK).
     *
     * The schema is purely additive: no existing table is touched
     * and no rows are backfilled. Players who already unlocked
     * higher levels keep their `unlockedLevel` value; the table
     * starts empty for them so the first unlock attempt after the
     * upgrade will require the new coverage rule instead of the
     * old XP-only rule.
     *
     * The composite primary key `(categoryKey, wordId, level)`
     * guarantees:
     *  - The same word at the same level is counted once.
     *  - A word covered at level 1 does NOT contribute to the
     *    coverage count at level 2 (each level tracks its own
     *    coverage).
     *  - Two games with different `categoryKey` values do not
     *    collide on the same word.
     */
    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `game_covered_words` (
                    `categoryKey` TEXT NOT NULL,
                    `wordId` INTEGER NOT NULL,
                    `level` INTEGER NOT NULL,
                    `coveredAt` INTEGER NOT NULL,
                    PRIMARY KEY (`categoryKey`, `wordId`, `level`)
                )
                """.trimIndent()
            )
        }
    }
}