package data.database.converters

import androidx.room.TypeConverter
import data.database.entities.Difficulty

/**
 * Persists the [Difficulty] enum as a textual column.
 *
 * Mapping strategy:
 *  - `fromDifficulty` writes `value.name` (e.g. `EASY`).
 *  - `toDifficulty` parses the column back into the enum using
 *    [Difficulty.fromStringOrNull]; unknown labels become `null` so the
 *    caller can decide whether to fall back to a default.
 */
class DifficultyConverter {

    @TypeConverter
    fun fromDifficulty(value: Difficulty?): String? = value?.name

    @TypeConverter
    fun toDifficulty(value: String?): Difficulty? =
        // Defensive parse: future JSON or schema migrations may introduce
        // values that the current enum does not know about.
        Difficulty.fromStringOrNull(value)
}