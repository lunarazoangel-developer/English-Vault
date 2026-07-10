package data.json.loader

import android.content.Context
import com.google.gson.Gson
import data.json.dto.WordDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads dictionary entries from the bundled `assets/words.json` file.
 *
 * This class is the only place in the codebase that touches Gson for
 * decoding; everything downstream operates on strongly typed [WordDto]
 * or Room entities. It never writes to the database — persistence is the
 * caller's responsibility (see [data.mapper.WordMapper] + `WordDao`).
 *
 * Provided as a Hilt singleton so the same Gson instance is shared across
 * the data layer.
 */
@Singleton
class JsonLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = Gson()

    /**
     * Loads and decodes the words JSON file.
     *
     * @param fileName Asset-relative file name. Defaults to
     *   [DEFAULT_FILE]; tests can point at a fixture by passing an
     *   alternate path.
     * @return Decoded list of [WordDto]. Empty when the file is missing
     *   or contains an empty array.
     */
    suspend fun loadWords(fileName: String = DEFAULT_FILE): List<WordDto> =
        // File I/O on Dispatchers.IO so we never block the caller's
        // dispatcher (typically Main from a Composable scope).
        withContext(Dispatchers.IO) {
            context.assets.open(fileName).use { inputStream ->
                val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                // Gson returns a nullable array; coerce to an empty list so
                // callers can rely on a non-null result.
                val array = gson.fromJson(json, Array<WordDto>::class.java)
                array?.toList().orEmpty()
            }
        }

    companion object {
        /** Default dictionary asset shipped with the APK. */
        private const val DEFAULT_FILE = "words.json"
    }
}