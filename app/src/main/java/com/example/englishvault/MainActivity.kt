package com.example.englishvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.englishvault.ui.app.MainScaffold
import com.example.englishvault.ui.theme.EnglishVaultTheme
import data.database.dao.UserProfileDao
import data.database.entities.UserProfileEntity
import data.seed.DictionarySeeder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity entry point for English Vault.
 *
 * Responsibilities:
 *  - Inflate the Compose tree wrapped in [EnglishVaultTheme].
 *  - Trigger the one-time bootstrap:
 *      - Default user profile → Room (only when the profile row is empty).
 *      - Core dictionary → Room (only when the bundled version is
 *        newer than the stored one, see [DictionarySeeder]).
 *  - Delegate all UI to [MainScaffold] which owns the bottom navigation
 *    and every screen.
 *
 * Hilt provides [dictionarySeeder] and [userProfileDao]; together they
 * cover the Phase 1 dictionary import and the Phase 3 user-profile
 * bootstrap.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dictionarySeeder: DictionarySeeder
    @Inject lateinit var userProfileDao: UserProfileDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishVaultTheme {
                // region: One-time database bootstrap
                LaunchedEffect(Unit) {
                    // Phase 3: create the default user profile if missing.
                    // Must run before the dictionary seeder because the
                    // seeder reads the profile to decide whether a re-seed
                    // is required.
                    if (userProfileDao.getProfile() == null) {
                        userProfileDao.upsertProfile(UserProfileEntity())
                    }

                    // Phase 4.5: import or refresh the bundled core
                    // dictionary. No-op when the stored version already
                    // matches the bundled one.
                    dictionarySeeder.seedIfNeeded()
                }
                // endregion

                MainScaffold()
            }
        }
    }
}