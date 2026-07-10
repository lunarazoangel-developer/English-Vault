package com.example.englishvault.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishvault.R
import com.example.englishvault.ui.components.PrimaryButton

/**
 * Test selection screen.
 *
 * Phase 2 mockup. Displays three test options with selectable difficulty
 * chips and a Start button. Tap interactions are local to this composable;
 * no navigation is wired yet.
 */
@Composable
fun TestScreen(modifier: Modifier = Modifier) {
    // region: Mock catalogue
    val tests = listOf(
        TestItem(
            titleRes = R.string.test_quick_title,
            subtitleRes = R.string.test_quick_subtitle,
            icon = Icons.Filled.Bolt,
            accent = MaterialTheme.colorScheme.primary
        ),
        TestItem(
            titleRes = R.string.test_full_title,
            subtitleRes = R.string.test_full_subtitle,
            icon = Icons.Filled.SportsEsports,
            accent = MaterialTheme.colorScheme.secondary
        ),
        TestItem(
            titleRes = R.string.test_custom_title,
            subtitleRes = R.string.test_custom_subtitle,
            icon = Icons.Filled.Tune,
            accent = MaterialTheme.colorScheme.tertiary
        )
    )
    // endregion

    // Local-only state so the chips can switch selection visually.
    var selectedDifficulty by remember { mutableStateOf(TestDifficulty.MEDIUM) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.test_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.test_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TestDifficulty.entries.forEach { level ->
                val selected = level == selectedDifficulty
                AssistChip(
                    onClick = { selectedDifficulty = level },
                    label = {
                        Text(
                            text = stringResource(id = level.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        labelColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(tests, key = { it.titleRes }) { test ->
                TestCard(test = test)
            }
        }
    }
}

@Composable
private fun TestCard(test: TestItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(test.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = test.icon,
                        contentDescription = null,
                        tint = test.accent
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = test.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = test.subtitleRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                text = stringResource(id = R.string.test_start),
                onClick = { /* Phase 3: navigate to test runner */ },
                containerColor = test.accent
            )
        }
    }
}

// region: Local mock models
private data class TestItem(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val accent: Color
)

private enum class TestDifficulty(val labelRes: Int) {
    EASY(R.string.games_difficulty_easy),
    MEDIUM(R.string.games_difficulty_medium),
    HARD(R.string.games_difficulty_hard)
}
// endregion