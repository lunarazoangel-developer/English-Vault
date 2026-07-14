package data.database.entities

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.englishvault.R

/**
 * The canonical language skills tracked by the app.
 *
 * Each skill owns one row in `skill_progress` (see
 * [SkillProgressEntity]). The XP grant pipeline from each mini-game
 * is wired against one or more of these keys in a dedicated phase
 * — for now the table exists, the UI renders the bars, and every
 * skill starts at `0 XP`.
 *
 * The display order in the UI mirrors [entries]: Listening →
 * Speaking → Reading → Writing → Grammar. This is the order most
 * language apps follow (receptive skills first, productive skills
 * second, grammar last) so the layout reads naturally even before
 * the mapping lands.
 *
 * @property key Stable string identifier persisted in the database
 *   (matches `skill_progress.skillKey`). Renaming an entry is a
 *   breaking change for existing installs.
 * @property labelRes String resource rendered on the skill card.
 * @property icon Material icon shown next to the skill name.
 */
enum class Skill(
    val key: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    LISTENING("LISTENING", R.string.skill_listening, Icons.Filled.Headphones),
    SPEAKING("SPEAKING", R.string.skill_speaking, Icons.Filled.Mic),
    READING("READING", R.string.skill_reading, Icons.Filled.MenuBook),
    WRITING("WRITING", R.string.skill_writing, Icons.Filled.Edit),
    GRAMMAR("GRAMMAR", R.string.skill_grammar, Icons.Filled.Spellcheck);

    companion object {
        /** Canonical iteration order used by the UI and the DAO seed. */
        val ALL: List<Skill> = entries.toList()
    }
}