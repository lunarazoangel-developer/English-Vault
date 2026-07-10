package data.database

/**
 * Pure helpers for deriving a user level from accumulated XP.
 *
 * The level curve is intentionally quadratic so each level requires more
 * XP than the previous one, matching the "almost like a game" feel of
 * the rest of the app. The math is kept here (and not inside the entity)
 * so it stays testable, side-effect-free and independent of Room.
 */
object UserLevel {

    /**
     * XP "unit" used by the curve. Increasing this constant flattens the
     * curve; lowering it makes levelling-up faster.
     */
    private const val XP_PER_LEVEL_UNIT: Int = 100

    /**
     * Returns the 1-based level for the given total XP.
     *
     * Curve: `level = floor(sqrt(totalXp / XP_PER_LEVEL_UNIT)) + 1`.
     * Negative or zero XP always maps to level 1.
     */
    fun levelFromXp(totalXp: Int): Int =
        if (totalXp <= 0) 1
        else floor(sqrt(totalXp.toDouble() / XP_PER_LEVEL_UNIT.toDouble())).toInt() + 1

    /**
     * Total XP needed to *reach* the start of the given [level].
     *
     * @param level Target level (clamped to >= 1).
     */
    fun xpForLevelStart(level: Int): Int {
        val safe = level.coerceAtLeast(1)
        val previous = safe - 1
        return previous * previous * XP_PER_LEVEL_UNIT
    }

    /**
     * Total XP needed to *complete* the given [level], i.e. the start of
     * the next level.
     */
    fun xpForLevelEnd(level: Int): Int = xpForLevelStart(level + 1)

    /**
     * @return `Pair(xpIntoCurrentLevel, xpRequiredForCurrentLevel)` ready
     * to feed a linear progress bar.
     */
    fun levelProgress(totalXp: Int): Pair<Int, Int> {
        val level = levelFromXp(totalXp)
        val start = xpForLevelStart(level)
        val end = xpForLevelEnd(level)
        val into = (totalXp - start).coerceAtLeast(0)
        // Defensive: avoid division by zero for level 0 callers.
        val required = (end - start).coerceAtLeast(1)
        return into to required
    }
}

// region: Local math aliases
// sqrt / floor come from kotlin.math; aliased at file scope so the public
// API reads naturally without extra imports.
private fun sqrt(value: Double): Double = kotlin.math.sqrt(value)
private fun floor(value: Double): Double = kotlin.math.floor(value)
// endregion