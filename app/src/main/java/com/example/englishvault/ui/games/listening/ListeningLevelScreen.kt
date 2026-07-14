package com.example.englishvault.ui.games.listening

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.games.listening.viewmodel.ListeningViewModel

/**
 * Level selector for the Listening mini-game.
 *
 * Mirrors [com.example.englishvault.ui.games.wordmatchverbs.WordMatchVerbsLevelScreen]:
 * one card per dictionary level (`1..maxLevel`), disabled when either:
 *  - the level has no eligible words, OR
 *  - the player has not yet unlocked that level via the
 *    `LISTENING` `category_progress` row.
 *
 * Locked cards display a padlock icon and a short hint so the user
 * knows how to unlock them.
 */
@Composable
fun ListeningLevelScreen(
    onBack: () -> Unit,
    onLevelChosen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListeningViewModel = hiltViewModel()
) {
    var maxLevel by remember { mutableStateOf(1) }
    var unlockedLevel by remember { mutableStateOf(1) }
    val wordsPerLevel = remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        maxLevel = viewModel.maxLevel()
        unlockedLevel = viewModel.maxUnlockedListeningLevel()
        val counts = (1..maxLevel).associateWith { level ->
            viewModel.wordsAtLevel(level)
        }
        wordsPerLevel.value = counts
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.game_listening_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.game_listening_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = R.string.game_listening_choose_level),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        val counts = wordsPerLevel.value
        val levels = (1..maxLevel).toList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(levels) { level ->
                val toPlay = counts[level] ?: 0
                val locked = level > unlockedLevel
                LevelCard(
                    level = level,
                    toPlay = toPlay,
                    locked = locked,
                    onClick = { if (!locked) onLevelChosen(level) }
                )
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: Int,
    toPlay: Int,
    locked: Boolean,
    onClick: () -> Unit
) {
    val enabled = !locked && toPlay > 0
    val containerColor = when {
        locked -> MaterialTheme.colorScheme.surfaceVariant
        enabled -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        locked -> MaterialTheme.colorScheme.onSurfaceVariant
        enabled -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (locked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(
                                id = R.string.game_listening_level_locked
                            ),
                            tint = contentColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.game_listening_level_format, level),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (locked) {
                        stringResource(id = R.string.game_listening_level_locked_hint)
                    } else {
                        stringResource(id = R.string.game_listening_to_play_format, toPlay)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}