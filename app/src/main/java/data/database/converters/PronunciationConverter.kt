package data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import data.database.entities.Pronunciation

/**
 * Persists a [Pronunciation] object as a single JSON string column.
 *
 * Mirrors the structure used by [FormsConverter]: the same Gson instance
 * is reused across conversions for allocation efficiency.
 */
class PronunciationConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromPronunciation(value: Pronunciation?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toPronunciation(value: String?): Pronunciation? =
        value?.takeIf { it.isNotBlank() }?.let {
            // Defensive parse: never propagate Gson failures to the UI.
            runCatching { gson.fromJson(it, Pronunciation::class.java) }.getOrNull()
        }
}