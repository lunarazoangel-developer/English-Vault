package com.example.englishvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.englishvault.ui.app.MainScaffold
import com.example.englishvault.ui.theme.EnglishVaultTheme
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.UserProfileEntity
import data.json.loader.JsonLoader
import data.mapper.WordMapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity entry point for English Vault.
 *
 * Responsibilities:
 *  - Inflate the Compose tree wrapped in [EnglishVaultTheme].
 *  - Trigger the one-time seeds:
 *      - `words.json` → Room (only when the words table is empty).
 *      - Default user profile → Room (only when the profile row is empty).
 *  - Delegate all UI to [MainScaffold] which owns the bottom navigation
 *    and every screen.
 *
 * Hilt provides [wordDao], [userProfileDao], [jsonLoader] and
 * [wordMapper]; together they cover the Phase 1 dictionary import and
 * the Phase 3 user-profile bootstrap.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var wordDao: WordDao
    @Inject lateinit var userProfileDao: UserProfileDao
    @Inject lateinit var jsonLoader: JsonLoader
    @Inject lateinit var wordMapper: WordMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishVaultTheme {
                // region: One-time database seed (Phase 1 + Phase 3 bootstrap)
                LaunchedEffect(Unit) {
                    // Phase 1: import the bundled dictionary if Room is empty.
                    if (wordDao.countWords() == 0) {
                        val dtos = jsonLoader.loadWords()
                        wordDao.insertWords(wordMapper.mapToEntityList(dtos))
                    }

                    // Phase 3: create the default user profile if missing.
                    // The single-row table is keyed at SINGLE_USER_ID so the
                    // upsert is idempotent and safe to call on every launch.
                    if (userProfileDao.getProfile() == null) {
                        userProfileDao.upsertProfile(UserProfileEntity())
                    }
                }
                // endregion

                MainScaffold()
            }
        }
    }
}