package data.game

import com.example.englishvault.ui.words.WordTypeFilter
import data.database.dao.CategoryProgressDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity

/**
 * Outcome of evaluating the hybrid promotion gate for a single
 * grammatical category.
 *
 * The gate lives behind [PromotionGate.evaluate] and is shared by
 * every call site that wants to bump a category's [unlockedLevel]
 * (the `category_progress` table):
 *
 *  - The Words screen, when the user manually marks a word as
 *    [data.database.entities.LearningStatus.LEARNED].
 *  - Each mini-game ViewModel, when a run finishes and grants XP.
 *
 * Centralising the evaluation in [PromotionGate.evaluate] guarantees
 * every call site uses the same strict rule documented in
 * [CategoryGating]:
 *
 *  - [XP_MIN_PER_LEVEL] XP earned at the current level, **and**
 *  - [LEARNED_PCT_REQUIRED] of the words at the current level marked
 *    as LEARNED.
 *
 * Earlier revisions of the mini-game ViewModels hardcoded
 * `meetsLearnedPct = true` when calling
 * [CategoryProgressDao.grantXpAndMaybeUnlock], which silently bypassed
 * the second requirement. Routing everything through [evaluate]
 * removes that inconsistency.
 */
sealed class PromotionOutcome {
    /**
     * The [categoryKey] did not resolve to a tracked grammatical
     * category (it was `ALL`, `MINE`, or a synthetic bucket such as
     * `LETTER_SOUP` / `LISTENING`). The DAO was not touched.
     */
    object Skipped : PromotionOutcome()

    /**
     * The hybrid gate did not pass; XP was added but
     * [data.database.entities.CategoryProgressEntity.unlockedLevel]
     * stayed where it was.
     */
    object Held : PromotionOutcome()

    /**
     * The gate passed and the category was promoted. [previousLevel]
     * is the value before the call (always >= 1) and [newLevel] is the
     * value after (always `previousLevel + 1`, capped at the category
     * max level).
     */
    data class Promoted(val previousLevel: Int, val newLevel: Int) : PromotionOutcome()
}

/**
 * Hybrid promotion gate shared by every call site that mutates
 * `category_progress.unlockedLevel` for grammatical categories.
 *
 * The "hybrid" name comes from [CategoryGating.XP_MIN_PER_LEVEL] +
 * [CategoryGating.LEARNED_PCT_REQUIRED]: both must hold at the same
 * evaluation moment for the category to advance by one level. The
 * DAO is wrapped in a `@Transaction` so the XP grant and the
 * promotion commit atomically; readers never observe an intermediate
 * state where the XP was bumped but the level has not moved yet.
 *
 * Synthetic buckets (`LETTER_SOUP`, `LISTENING`) are intentionally
 * out of scope — those have their own progression rules and are not
 * tied to the per-word learned percentage. Callers that want to bump
 * a synthetic bucket must continue calling
 * [CategoryProgressDao.grantXpAndMaybeUnlock] directly.
 */
object PromotionGate {

    /**
     * Reads the current [data.database.entities.CategoryProgressEntity]
     * for [categoryKey], evaluates the hybrid gate, applies the
     * XP grant through [CategoryProgressDao.grantXpAndMaybeUnlock]
     * and returns whether the category was promoted.
     *
     * @param categoryKey Stable category identifier. Must match a
     *   `WordTypeFilter.name` of a tracked grammatical category
     *   (e.g. `VERBS_REGULAR`, `ADJECTIVES`). Anything else returns
     *   [PromotionOutcome.Skipped] without touching the database.
     * @param amount XP to grant. Negative values are rejected to keep
     *   the DAO's invariants intact (see
     *   [CategoryProgressDao.grantXpAndMaybeUnlock]). Pass `0` when
     *   the caller only wants to re-evaluate the gate without
     *   granting XP — this is the path the Words screen takes when
     *   the user marks a word as LEARNED.
     * @param wordDao DAO used to look up the per-level word counts
     *   (`countWordsAt` / `countLearnedAt` / `maxLevelByType`).
     * @param categoryProgressDao DAO used to seed, read and update
     *   `category_progress`.
     * @return [PromotionOutcome.Promoted] when the gate fires,
     *   [PromotionOutcome.Held] when XP was added but the gate did
     *   not pass, or [PromotionOutcome.Skipped] when [categoryKey]
     *   is not a tracked grammatical category.
     */
    suspend fun evaluate(
        categoryKey: String,
        amount: Int,
        wordDao: WordDao,
        categoryProgressDao: CategoryProgressDao
    ): PromotionOutcome {
        require(amount >= 0) { "amount must be non-negative, was $amount" }

        // Resolve the category. ALL / MINE / synthetic buckets fall
        // through to Skipped; tracked grammatical categories carry a
        // non-null type literal.
        val filter = WordTypeFilter.entries.firstOrNull { it.name == categoryKey }
            ?: return PromotionOutcome.Skipped
        val typeLiteral = filter.type ?: return PromotionOutcome.Skipped

        // Defensive seed: without this, a missing row (e.g. an
        // install that pre-dates MIGRATION_6_7) would silently drop
        // the XP grant because Room's @Update does not insert.
        categoryProgressDao.seedIfMissing(categoryKey)
        val progress = categoryProgressDao.get(categoryKey)
            ?: CategoryProgressEntity.initial(categoryKey)

        val maxLevel = wordDao.maxLevelByType(typeLiteral, filter.regular)
            .coerceAtLeast(1)
        val previousLevel = progress.unlockedLevel.coerceIn(1, maxLevel)
        val nextLevel = (previousLevel + 1).coerceAtMost(maxLevel)

        val xpAfter = progress.xpSinceLevelUp + amount
        val totalAtLevel = wordDao.countWordsAt(typeLiteral, filter.regular, previousLevel)
        val learnedAtLevel = wordDao.countLearnedAt(typeLiteral, filter.regular, previousLevel)
        val learnedPct = if (totalAtLevel == 0) 1f else learnedAtLevel.toFloat() / totalAtLevel

        val meetsXp = xpAfter >= CategoryGating.XP_MIN_PER_LEVEL
        val meetsLearnedPct = learnedPct >= CategoryGating.LEARNED_PCT_REQUIRED
        val shouldUnlock = nextLevel > previousLevel && meetsXp && meetsLearnedPct

        categoryProgressDao.grantXpAndMaybeUnlock(
            categoryKey = categoryKey,
            amount = amount,
            meetsXp = shouldUnlock,
            meetsLearnedPct = shouldUnlock,
            targetUnlockedLevel = if (shouldUnlock) nextLevel else previousLevel
        )

        return if (shouldUnlock) {
            PromotionOutcome.Promoted(previousLevel = previousLevel, newLevel = nextLevel)
        } else {
            PromotionOutcome.Held
        }
    }
}