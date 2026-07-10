package data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import data.database.entities.Example

/**
 * Persists a `List<Example>` as a single JSON string column.
 *
 * Gson is reused across calls because allocating one per conversion is
 * wasteful and Gson itself is thread-safe for read operations.
 */
class ExampleConverter {

    private val gson = Gson()

    // TypeToken is required because Gson erases generic types at runtime;
    // declaring it once keeps the converter allocation-free on hot paths.
    private val listType = object : TypeToken<List<Example>>() {}.type

    @TypeConverter
    fun fromExamples(value: List<Example>?): String? =
        value?.let { gson.toJson(it, listType) }

    @TypeConverter
    fun toExamples(value: String?): List<Example>? =
        value?.takeIf { it.isNotBlank() }?.let {
            // runCatching swallows malformed JSON instead of crashing the
            // whole query — a corrupted row should not take down the UI.
            runCatching { gson.fromJson<List<Example>>(it, listType) }.getOrNull()
        }
}