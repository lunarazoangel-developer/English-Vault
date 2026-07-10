package data.database.converters

import androidx.room.TypeConverter

/**
 * Stores a `List<String>` as a single column by joining with a delimiter
 * unlikely to appear in normal English or Spanish text.
 *
 * Used by fields such as `WordEntity.category` and `WordEntity.tags`.
 *
 * Trade-off: this format is not as expressive as JSON (no escaping) and
 * assumes the chosen delimiter never appears inside a list element. If
 * the dictionary ever needs to store punctuation-heavy tags, switch to
 * a Gson-based converter similar to [ExampleConverter].
 */
class ListStringConverter {

    // "¬" (U+00AC, NOT SIGN) is rarely used in natural language text,
    // making accidental collisions in word/tag content extremely unlikely.
    private val delimiter = "¬"

    @TypeConverter
    fun fromList(value: List<String>?): String =
        value?.joinToString(separator = delimiter).orEmpty()

    @TypeConverter
    fun toList(value: String?): List<String>? =
        if (value.isNullOrEmpty()) null
        else value.split(delimiter)
}