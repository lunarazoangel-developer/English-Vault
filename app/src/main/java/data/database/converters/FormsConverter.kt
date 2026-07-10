package data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import data.database.entities.Forms

/**
 * Persists a [Forms] object as a single JSON string column.
 *
 * Gson is reused across conversions (thread-safe for read operations)
 * so we avoid the cost of allocating a new instance on every read.
 */
class FormsConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromForms(value: Forms?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toForms(value: String?): Forms? =
        value?.takeIf { it.isNotBlank() }?.let {
            // Swallow malformed JSON: a single bad row should not abort
            // the parent query.
            runCatching { gson.fromJson(it, Forms::class.java) }.getOrNull()
        }
}