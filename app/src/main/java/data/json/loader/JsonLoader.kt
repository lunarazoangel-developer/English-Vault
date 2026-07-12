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
 * Reads dictionary entries from the bundled `assets/dictionary/` folder.
 *
 * The folder is split into per-type section files (`verbs_irregular.json`,
 * `verbs_regular.json`, etc.). Each file is a flat JSON array of [WordDto]
 * entries; this loader concatenates them in the order declared by
 * [SECTION_FILES]. The shape of each entry is unchanged from the previous
 * single-file layout — only the file boundary moved.
 *
 * Keeping the dictionary sectioned makes it easier to:
 *  - Grep across a single grammatical category.
 *  - Distribute word authoring across multiple contributors.
 *  - Grow the dictionary to hundreds of entries without one giant file.
 *
 * `assets/dictionary/README.md` documents each section and acts as the
 * "comment block" that JSON itself cannot carry.
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
     * Loads every section file in [SECTION_FILES] order and concatenates
     * the decoded [WordDto]s into a single list.
     *
     * File I/O on Dispatchers.IO so we never block the caller's dispatcher
     * (typically Main from a Composable scope).
     *
     * @param folder Asset-relative folder holding the section files.
     *   Defaults to [DEFAULT_FOLDER]; tests can point at a fixture folder
     *   (e.g. `"fixtures/dictionary_small"`).
     * @return Decoded list, in section order, alphabetised within each
     *   section as authored. Empty when the folder is missing.
     */
    suspend fun loadWords(folder: String = DEFAULT_FOLDER): List<WordDto> =
        withContext(Dispatchers.IO) {
            SECTION_FILES
                .map { fileName -> resolvePath(folder, fileName) }
                .map(::decodeFile)
                .flatten()
        }

    /**
     * Loads a single section file. Mostly intended for tests and tooling
     * that need to validate one section in isolation.
     *
     * @param filePath Asset-relative path to the section JSON file
     *   (e.g. `"dictionary/verbs_irregular.json"`).
     */
    suspend fun loadWordsFile(filePath: String): List<WordDto> =
        withContext(Dispatchers.IO) { decodeFile(filePath) }

    private fun decodeFile(filePath: String): List<WordDto> {
        // Gson returns a nullable array; coerce to an empty list so callers
        // can rely on a non-null result.
        return context.assets.open(filePath).use { inputStream ->
            val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = gson.fromJson(json, Array<WordDto>::class.java)
            array?.toList().orEmpty()
        }
    }

    private fun resolvePath(folder: String, fileName: String): String =
        if (folder.isEmpty()) fileName else "$folder/$fileName"

    companion object {
        /** Asset folder holding the bundled section files. */
        const val DEFAULT_FOLDER = "dictionary"

        /**
         * Section files concatenated in load order.
         *
         * This order **defines** the order entries appear in the Words
         * screen, so it must stay in sync with the table in
         * `assets/dictionary/README.md`. Adding a new section requires
         * appending the file name here AND bumping
         * `DictionarySeeder.CORE_DICTIONARY_VERSION`.
         */
        val SECTION_FILES: List<String> = listOf(
            "verbs_irregular.json",
            "verbs_regular.json",
            "interjections.json",
            "nouns.json",
            "adjectives.json",
            "adverbs.json",
            "prepositions.json",
            "conjunctions.json"
        )
    }
}
