# English Vault

> Your personal English vocabulary trainer for Android, built with Kotlin and Jetpack Compose.

A Duolingo-inspired vocabulary app with streak tracking, per-category XP / level progression, mini-games, a Super Mario Bros-style world map and a personal word database you fully own. Built offline-first on top of Room and a versioned seed JSON asset.

---

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Changelog](#changelog)
- [Contributing](#contributing)
- [License](#license)
- [Credits](#credits)

---

## Features

### Available now

- **Offline dictionary** seeded from `assets/dictionary/` — eight per-type section files (`verbs_irregular.json`, `verbs_regular.json`, `interjections.json`, `nouns.json`, `adjectives.json`, `adverbs.json`, `prepositions.json`, `conjunctions.json`) totalling **hundreds of entries** with bilingual examples tagged by CEFR level. `assets/dictionary/README.md` acts as the section index.
- **Room storage v8** with four artefacts:
  - `core_words` — dictionary entries seeded from the `assets/dictionary/` section files. Conceptually read-only; the user can only update its user-state columns (`favorite`, `status`, `level`, `notes`, …) via the dual-table DAO pattern.
  - `user_words` — entries the learner added through the form. Fully mutable.
  - `words_view` — `UNION ALL` of both tables, exposed as the read-only `WordEntity` data class so the UI consumes a single model regardless of origin.
  - `user_profile` — single-row table for global XP, streak, daily goal, display name, dictionary seed version and the player's persistent hearts and coins counters.
  - `category_progress` — one row per tracked grammatical category (`VERBS_REGULAR`, `ADJECTIVES`, …) holding cumulative XP, unlocked level and XP-since-last-promotion. Drives the per-category progression system on the Progress screen.
- **Five `TypeConverter`s** for nested objects (`Forms`, `Pronunciation`, `Example`) and string lists.
- **Hilt DI graph** wiring `AppDatabase`, three DAOs (`WordDao`, `UserProfileDao`, `CategoryProgressDao`), `JsonLoader`, `WordMapper`, `DictionarySeeder` and the seed routine.
- **Seven versioned migrations** that keep every previous install alive:
  - `MIGRATION_1_2` — adds the `user_profile` table.
  - `MIGRATION_2_3` — recreates `words` with `AUTOINCREMENT` ids.
  - `MIGRATION_3_4` — splits `words` into `core_words` + `user_words` + `words_view`.
  - `MIGRATION_4_5` — adds the `coreDictionaryVersion` column for versioned seeding.
  - `MIGRATION_5_6` — replaces the boolean `learned` column with a tri-state `status` enum and adds a `level: Int` to both word tables.
  - `MIGRATION_6_7` — adds `category_progress` and seeds one row per tracked category.
  - `MIGRATION_7_8` — adds the persistent `hearts` and `coins` counters to `user_profile` so the World map HUD can render the player's gamified state.
- **Versioned seeding** via `DictionarySeeder` (`@Singleton`): bumping `CORE_DICTIONARY_VERSION` in code triggers an automatic re-import of the bundled JSON on next launch, without losing user-added words or learning state.
- **Pure XP/Level math** in `data.database.UserLevel` (quadratic curve) — reused by both the global XP card and the per-category levels.
- **Per-category progression (Phase 4.6)** — eight parallel tracks. Correct answers in the mini-game grant XP per category. Advancing to the next level requires a hybrid gate: at least `XP_MIN_PER_LEVEL` XP earned at the current level **and** at least `LEARNED_PCT_REQUIRED` of the words at that level marked `LEARNED`. The DAO wraps each grant + promotion in a single transaction.
- **Bottom navigation shell** with four tabs: Progress, World, Games, Words. The Test tab was replaced by a beta SMB-style World map that turns the level selector into a horizontal scrolling journey, complete with a 10-node path, a branching shop and a castle finale.
- **Words screen wired to Room** via `WordListViewModel` (`@HiltViewModel`, `StateFlow`). Create + Delete persist.
- **Eight type-filter chips** on the Words screen — regular verbs, irregular verbs, adjectives, adverbs, nouns, conjunctions, prepositions, interjections — plus `All` and `Mine`, in a horizontally-scrollable row.
- **Live sort row** — A-Z, Z-A, level ascending, level descending — combined with the type filter and persisted across rotations via `rememberSaveable`.
- **Read-only defaults** — `core_words` rows show a Dictionary badge (Material `MenuBook` icon) and have no edit / delete affordances; `user_words` rows show a Mine badge (Material `Person` icon) plus edit / delete icons. The `WordListViewModel` re-checks `isUserAdded()` before any delete so the invariant holds even if the UI regresses.
- **Rich expandable cards** — each card collapses by default to the header + badges and expands in place (with smooth animation and per-card state persisted via `rememberSaveable`) to reveal pronunciation, verb forms, examples with CEFR badges, synonyms, antonyms, tags and category.
- **Live tab counts** — every chip in the Words screen shows the number of words matching its filter.
- **Tri-state learning progress** — every word carries a `LearningStatus` (`NOT_LEARNED` / `ALMOST` / `LEARNED`) with a dedicated status menu button on every card. Picking a status persists immediately via the dual-table DAO.
- **Word progression levels** — both core and user words have an independent `level: Int` that gates availability in mini-games, so the learner is never overwhelmed by the whole dictionary at once.
- **Progress screen** — `ProgressViewModel` exposes the global profile, level / xp slice, daily-goal estimate, streak and one `CategoryProgressUi` per tracked grammatical category. Each per-category card carries its own level (1..N), an XP bar, a learned-percentage bar and a hybrid-gate status message.
- **Word Match Verbs mini-game** — tap a level card to start a run of up to 20 randomly-picked questions; each asks about the past simple, 3rd person or past participle of one verb at the chosen level. Distractors come from `DistractorGenerator` (vowel swaps + phonetically close consonants). When the user picks wrong the correct answer is revealed with a blue check while their pick gets a red X. At end of run the per-category XP grant fires, the hybrid gate is evaluated, and the next level unlocks automatically when both requirements are met. The level selector dims cards beyond the player's current `unlockedLevel`.
- **World map (Phase 7, beta)** — Super Mario Bros-inspired level selector rendered as a single Canvas. The map is 2200 dp wide so the user must scroll horizontally to discover all 10 nodes, which trace an almost-straight path across a grass-and-sky scene. A branching dirt path leads to a small shop drawn from primitive shapes, a castle with two stone towers and a yellow flag stands on the last waypoint, and five clouds float in the sky band. A HUD in the header shows the player's persistent hearts and coins (read live from `UserProfileEntity` through `WorldViewModel`). Tapping the next waypoint advances the protagonist with a smooth `Animatable` interpolation.
- **Settings hub (Phase 7.1)** — reachable from the Progress screen via the greeting button (tap "Hello, Name" to open). Two sections: **Profile** (rename the user in a dedicated sub-screen with form validation) and **Sound** (music and effects volume sliders in `[0.0, 1.0]`, both persisted via Room; music is wired as a placeholder until the audio engine lands). The schema bump v8 → v9 brings `musicVolume` and `effectsVolume` columns to `user_profile` via `MIGRATION_8_9` without data loss.
- **Audio foundation (Phase 7.2)** — `audio/SoundEffectPlayer` plays short SFX on game events, currently backed by `ToneGenerator` so the app ships zero audio assets. WordMatchVerbs plays a positive beep (`TONE_PROP_ACK`, 300 ms) when the user picks the correct answer, and the volume reacts live to the **Effects** slider in Settings because the VM observes `user_profile.effectsVolume` through a `StateFlow`. `SoundKey` already declares `Wrong` and `Victory` placeholders so the wrong-answer / end-of-run sounds are a one-line addition when the team is ready.
- **SoundPool-backed SFX + custom assets (Phase 7.3)** — `SoundEffectPlayer` now wraps `android.media.SoundPool` with `AudioAttributes(USAGE_GAME, CONTENT_TYPE_SONIFICATION)` and loads `res/raw/correct_sound.mp3` once per process. `setMaxStreams(4)` lets overlapping beeps stack without losing the first one. `SoundKey.Correct` reuses that single asset; the per-playback `volume` argument honours the Effects slider 1:1 (the previous `ToneGenerator`-based path was gated by the system notification-stream volume, which `SoundPool.setVolume` bypasses).
- **Letter Soup mini-game (Phase 7.3)** — second playable mini-game, gated by `category_progress.LETTER_SOUP`. The level selector mirrors `WordMatchVerbsLevelScreen` (one card per `level` 1..10, dimmed past the unlocked threshold). The play screen renders an 8×8 grid by default and auto-scales to **10×10** when any placement has 9+ characters. Each round places up to **5** words at the chosen level; each placement carries ONE wrong letter (badge ❌ + pulse animation) and the player must find the correct letter in the soup and swap to fix it. The Spanish translations list is **always visible** above the board; tapping the 💡 button reveals the wrong-letter cell for 3 s, tapping the 🔤 button swaps that chip's translation for the English word inline for 3 s. The board does **not** reset mid-run; the run ends when every placement is fixed (`wordsFixed == wordsToWin`). 20 moves budget per run. The `redistribute_levels.py` script guarantees every category has words at every level, so the selector renders 10 cards for every grammar bucket.
- **Dev toggle for both mini-games (Phase 7.4)** — a small `ModeToggleButton` in the top-right of each play screen flips between `NORMAL` and `WORLD` (or `HintMode.WORLD` for Letter Soup). Flipping the toggle restarts the active run in the new mode so the change is applied without navigating away. World mode adds **lives** (3), a **per-question countdown** (10 s) for Word Match Verbs / a **per-run countdown** (5 min) for Letter Soup, and **limited hint items** in Letter Soup (2 location + 2 English). Hiding the toggle is one boolean flip (`DEV_MODE_TOGGLE_ENABLED`) at the top of each screen; removing it is one delete of the `if (...) ModeToggleButton(...)` block. The dedicated player-inventory system that will eventually back these items is out of scope for the beta — `consumeHintItemIfWorldMode` / `consumeHintItemIfWorldMode` are intentional `TODO` stubs and the inventories are hard-coded caps surfaced through the `*HintsRemaining` state fields.
- **Dictionary levelled up to 10 (Phase 7.4)** — `tools/redistribute_levels.py` re-bucketed every entry's `level` field across all 10 levels. Previously the levels were uneven: verbs went up to 5, nouns / prepositions / conjunctions / interjections only reached 2. The script sorts each section by word length (alphabetical tiebreaker) and assigns `level = ceil((i + 1) * 10 / n)` so every level is populated in every category. **Entry count unchanged at 794; per-entry content untouched.** Word Match Verbs and Letter Soup both render up to 10 level cards on their selectors; total XP to unlock everything per category scales from 250 (5 × `XP_MIN_PER_LEVEL`) to 500 (10 × `XP_MIN_PER_LEVEL`). `DictionarySeeder.CORE_DICTIONARY_VERSION` bumped 13 → 14 so existing installs re-seed automatically.
- **Word Match Verbs: 3rd-person removed (Phase 7.4)** — `WordMatchAskType.THIRD_PERSON` removed from the rotation because it was deemed too predictable from the base verb. The game now alternates between `PAST_SIMPLE` and `PAST_PARTICIPLE`.

### Planned

- **Custom audio assets** — drop `sfx_correct.ogg` / `sfx_wrong.ogg` / `sfx_victory.ogg` / `bgm_world.ogg` / `bgm_game.ogg` into `res/raw/` and swap `ToneGenerator` for `SoundPool` (`SoundEffectPlayer`'s public API stays the same).
- **Background music** — `MediaPlayer` reading the music slider live, looped per-screen (World map + mini-games only).
- **SRS-based review scheduling** powered by `lastReview` / `nextReview` fields (Phase 7.1).
- **Other mini-games** — Speed Quiz, Memory Cards, Listening, Fill the Blank, Translation Race (Phase 7.1).
- **More user settings** — theme mode, daily goal editor, reminder time (Phase 7.1).
- **Search + filters** inside the Words screen (search bar, difficulty / category chips) (Phase 7.1).
- **Shop economy wiring** — connect the World shop to `addCoins` / `addHearts` so spending coins actually buys lives (Phase 7.1).
- **Cloud sync / backup** (Phase 8).

---

## Screenshots

> Placeholder — UI mockups will be added once the app is run on a device.

| Progress | World | Games | Words |
| :------: | :---: | :---: | :---: |
|   TBD    |  TBD  |  TBD  |  TBD  |

> Word Match Verbs is reachable from **Games → Word Match Verbs** and walks through `Level Select → Branded Loading → Verb / 3 Options → green / blue / red feedback → Results`.

---

## Tech Stack

| Layer        | Technology                                          |
| ------------ | --------------------------------------------------- |
| Language     | Kotlin 2.2.0                                        |
| UI           | Jetpack Compose (Material 3)                        |
| Navigation   | `androidx.navigation:navigation-compose` 2.9.3      |
| DI           | Hilt 2.57.1                                         |
| Persistence  | Room 2.7.2 + `@DatabaseView` + Type Converters      |
| Async        | Kotlin Coroutines 1.10.2 + `Flow` / `StateFlow`     |
| JSON         | Gson 2.13.1                                         |
| Build        | Gradle 9.x, AGP 8.11.1, KSP 2.2.0-2.0.2           |
| Min SDK      | 28 (Android 9)                                      |
| Target SDK   | 36 (Android 16)                                     |

---

## Architecture

English Vault follows a clean **offline-first** pipeline plus an MVVM-flavoured UI layer wired through Hilt.

```
   +------------------------------+
   |  assets/dictionary/*.json    |
   |  + dictionary/README.md      |   <- section index
   +------------+-----------------+
                |  Gson (UTF-8), ordered by
                |  JsonLoader.SECTION_FILES
                v
   +---------------------+
   |  data/json/dto      |   WordDto + FormsDto + PronunciationDto + ExampleDto
   |      WordDto        |
   +----------+----------+
              |  JsonLoader.loadWords()  (Dispatchers.IO)
              v
   +---------------------+
   |  data/mapper        |   WordMapper.mapToCoreEntity(...)
   |     WordMapper      |
   +----------+----------+
              |  List<CoreWordEntity>
              v
    +-------------------------------------------------------+
    | data/database  (AppDatabase v8)                       |
    |   +- CoreWordEntity   -> core_words  (AUTOINCREMENT)   |
    |   +- UserWordEntity   -> user_words  (AUTOINCREMENT)   |
    |   +- WordEntity       -> words_view  (@DatabaseView)   |
    |   +- UserProfileEntity -> user_profile (+ version,     |
    |   |                       hearts, coins)               |
    |   +- CategoryProgressEntity -> category_progress       |
    |   +- WordDao                                            |
    |   +- UserProfileDao                                     |
    |   +- CategoryProgressDao                                |
    |   +- 5 TypeConverters                                   |
    +----------+--------------------------------------------+
               |  Flow<List<WordEntity>> via words_view
               v
    +---------------------+
    |  data/seed          |   DictionarySeeder.seedIfNeeded()
    |   DictionarySeeder  |   (re-imports JSON when bundled
    +----------+----------+    version > stored version)
               |
               v
    +---------------------+
    | WordListViewModel   |   @HiltViewModel - add / delete
    |  (StateFlow + ops)  |   gating (isUserAdded)
    +----------+----------+
               |  collectAsState
               v
    +---------------------+          +---------------------+
    |       ui/words      |          |      ui/progress    |
    |   Compose UI        |          |   Compose UI        |
    | (WordListScreen +   |          | (ProgressScreen +   |
    |  WordFormScreen +   |          |  8 CategoryProgress |
    |  WordCard)          |          |  Cards)             |
    +---------------------+          +---------------------+

+---------------------+
     |   ui/games/wordmatch  |   Word Match Verbs mini-game
     |   (WordMatchVerbs...) |
     +---------------------+
     +---------------------+
     |   ui/games/lettersoup |   Letter Soup mini-game (Phase 7.3+)
     |   (LetterSoup...)    |
     +---------------------+

               +---------------------+
               |   ui/world          |   World map (Phase 7, beta)
               |   (WorldScreen +    |   reads profile.hearts / profile.coins
               |    WorldViewModel)  |
               +---------------------+
                               ^
                               | reads UserProfileEntity
                               |
    +---------------------------+
    |  data/database           |
    |   WordDao               |  getCoreWordsForGame(level)
    |   UserProfileDao        |  observeProfile, addHearts, addCoins
    |   observeProgressStats(now)
    |   setStatus(id, status)  (dual-table)
    +---------------------------+

   Mini-game flow (Phase 4.6):
     submitAnswer(correct) -> accumulate XP in state.correctXpByCategory
     acknowledgeAnswer (last Q -> Finished) -> grantXpAndMaybeUnlock per category
       -> CategoryProgressDao (atomic: XP + reset xpSinceLevelUp on promotion)
```

Key principles:

- **DTO / Entity separation**: `WordDto` mirrors JSON; `CoreWordEntity` / `UserWordEntity` are the Room aggregates; `WordEntity` is the read-only view that unifies them. `CategoryProgressEntity` is a standalone one-row-per-category tracker. The mapper keeps DTO / entity in sync.
- **Two tables, one view**: writes explicitly target `core_words` or `user_words`; reads always target `words_view`. The view's `source` literal (`'core'` / `'user'`) is what `isUserAdded()` checks, so the rest of the app stays source-agnostic.
- **Independent id sequences**: each table has its own `AUTOINCREMENT`. The JSON id is never carried into Room, so future JSON updates can never collide with user-added ids.
- **Dual-table update pattern**: state-mutating DAO methods (`setStatus`, `setFavorite`, …) cannot be expressed as a single `UPDATE words` because there is no unified table. They are implemented as default methods that fan out into two `@Query` calls — one per underlying table. Because the two sequences are independent, only the table that actually holds the id touches a row; the other is a no-op.
- **Atomic per-category grant**: `CategoryProgressDao.grantXpAndMaybeUnlock` wraps the XP increment and the `xpSinceLevelUp` reset inside a single `@Transaction` so readers never observe a mid-promotion state.
- **Reactive data flow**: DAO queries expose `Flow<...>`; ViewModels turn them into `StateFlow`; Compose collects with `collectAsState`.
- **Single source of truth for state**: the Room database. Compose observes; mutations go through the ViewModel.
- **Game flow isolation**: mini-game VMs (`WordMatchVerbsViewModel`, …) live in their own scoped composables; their `LaunchedEffect(level)` patterns rely on the level being passed as a navigation argument so cross-screen state pollution cannot hang the loading screen.
- **Domain rules live in the VM**: `WordListViewModel.deleteWord` re-checks `isUserAdded()` so the read-only defaults invariant holds even if a stale dialog slips through. `WordMatchVerbsViewModel.tryUnlockCategory` is the single place that evaluates the hybrid gate.
- **Hilt singletons**: `AppDatabase`, `JsonLoader`, `WordMapper`, `DictionarySeeder` are application-scoped.
- **Type safety**: enums (`Difficulty`, `LearningStatus`, `WordTypeFilter`, `WordMatchAskType`) replace free-form strings as soon as data crosses the JSON boundary.
- **Versioned migrations**: every schema bump registers an explicit `Migration` rather than wiping user data.
- **Versioned seeding**: the bundled `CORE_DICTIONARY_VERSION` constant is compared to `UserProfileEntity.coreDictionaryVersion` on every launch; when the bundled version is newer, `DictionarySeeder` wipes `core_words` and re-imports the JSON, leaving `user_words` and the learning state untouched.

---

## Project Structure

```
EnglishVault/
|-- README.md                          <- you are here
|-- build.gradle.kts                   <- top-level Gradle config
|-- settings.gradle.kts
|-- gradle/
|   `-- libs.versions.toml             <- version catalog
|-- tools/                             <- one-shot maintenance scripts
|   `-- redistribute_levels.py        <- re-bucket every entry's `level` field (Phase 7.4)
`-- app/
    |-- build.gradle.kts
    |-- proguard-rules.pro
    `-- src/main/
        |-- AndroidManifest.xml
        |-- assets/
        |   `-- dictionary/            <- bundled dictionary (per-type section files,
        |       |-- README.md             README index explains load order and field rules)
        |       |-- verbs_irregular.json
        |       |-- verbs_regular.json
        |       |-- interjections.json
        |       |-- nouns.json
        |       |-- adjectives.json
        |       |-- adverbs.json
        |       |-- prepositions.json
        |       `-- conjunctions.json
|-- res/
         |   |-- values/
         |   |   |-- strings.xml        <- UI strings
         |   |   |-- colors.xml
         |   |   `-- themes.xml
         |   |-- raw/                    <- binary audio assets (Phase 7.3+)
         |   |   `-- correct_sound.mp3
         |   `-- mipmap-*/              <- launcher icons
        `-- java/
            |-- com/example/englishvault/
            |   |-- EnglishVaultApp.kt <- @HiltAndroidApp
            |   |-- MainActivity.kt    <- single-activity host + bootstrap
            |   `-- ui/
            |       |-- app/MainScaffold.kt          <- bottom nav + NavHost
            |       |-- components/                  <- AppBottomBar, PrimaryButton, SectionHeader
|       |-- games/
             |       |   |-- GamesScreen.kt
             |       |   |-- wordmatchverbs/          <- Word Match Verbs mini-game
             |       |   |   |-- WordMatchVerbsLevelScreen.kt
             |       |   |   |-- WordMatchVerbsGameScreen.kt
             |       |   |   |-- WordMatchVerbsEndScreen.kt   (WordMatchVerbsEndContent composable)
             |       |   |   |-- model/
             |       |   |   |   |-- WordMatchQuestion.kt
             |       |   |   |   `-- WordMatchGameState.kt
             |       |   |   |-- util/DistractorGenerator.kt
             |       |   |   `-- viewmodel/WordMatchVerbsViewModel.kt
             |       |   `-- lettersoup/             <- Letter Soup mini-game (Phase 7.3+)
             |       |       |-- LetterSoupLevelScreen.kt
             |       |       |-- LetterSoupGameScreen.kt
             |       |       |-- LetterSoupEndScreen.kt
             |       |       |-- model/
             |       |       |   |-- LetterSoupBoard.kt
             |       |       |   |-- LetterSoupCell.kt
             |       |       |   |-- LetterSoupGameState.kt
             |       |       |   `-- LetterSoupWord.kt
             |       |       |-- util/
             |       |       |   |-- BoardGenerator.kt
             |       |       |   |-- EnglishLetterFrequency.kt
             |       |       |   `-- LetterPalette.kt
             |       |       `-- viewmodel/LetterSoupViewModel.kt
            |       |-- navigation/                  <- Destination + BottomNavItem
             |       |-- progress/
             |       |   |-- ProgressScreen.kt         <- 8 per-category cards + global XP
             |       |   `-- viewmodel/ProgressViewModel.kt
             |       |-- world/
             |       |   |-- WorldScreen.kt            <- horizontal SMB-style level map
             |       |   `-- WorldViewModel.kt         <- hearts / coins from UserProfileEntity
            |       |-- theme/                       <- blue palette
            |       `-- words/
            |           |-- WordListScreen.kt        <- 8 type chips + sort row + expandable cards
            |           |-- WordFormScreen.kt        <- loads existing word on edit
            |           |-- WordTypeFilter.kt        <- shared enum across screens
            |           |-- components/WordCard.kt   <- rich, expandable, status menu
            |           `-- viewmodel/WordListViewModel.kt   <- @HiltViewModel
            `-- data/
                |-- database/
                |   |-- AppDatabase.kt               (v8)
                |   |-- Migrations.kt                <- 7 migrations (1->2, 2->3, 3->4, 4->5, 5->6, 6->7, 7->8)
                |   |-- UserLevel.kt                 <- XP/Level pure math (global + per-category)
                |   |-- dao/
                |   |   |-- WordDao.kt                <- dual-table updates, game queries, per-category counts
                |   |   |-- UserProfileDao.kt
                |   |   `-- CategoryProgressDao.kt    <- per-category XP / unlocked level
                |   |-- entities/
                |   |   |-- CoreWordEntity.kt         <- @Entity(core_words), status + level
                |   |   |-- UserWordEntity.kt         <- @Entity(user_words), status + level
                |   |   |-- WordEntity.kt             <- @DatabaseView(words_view)
                |   |   |-- CategoryProgressEntity.kt <- @Entity(category_progress)
                |   |   |-- LearningStatus.kt         <- enum: NOT_LEARNED / ALMOST / LEARNED
                |   |   |-- WordMappers.kt            <- toUserEntity() / toCoreEntity()
                |   |   |-- UserProfileEntity.kt
                |   |   `-- ProgressStats.kt
                |   `-- converters/                  <- 5 type converters
                |-- game/
                |   `-- CategoryGating.kt            <- XP thresholds, learned %, tracked categories
                |-- json/
                |   |-- dto/WordDto.kt               <- +level field
                |   `-- loader/JsonLoader.kt
                |-- mapper/WordMapper.kt
                `-- seed/DictionarySeeder.kt          <- versioned re-seed
        `-- di/DatabaseModule.kt                  <- Hilt graph + migrations
```

---

## Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer.
- **JDK 21** (the bundled JBR works out of the box).
- **Android SDK 36** installed via SDK Manager.
- A device or emulator running **Android 9 (API 28)** or newer.

### Clone

```bash
git clone https://github.com/lunarazoangel-developer/English-Vault.git
cd English-Vault
```

### Open in Android Studio

1. `File -> Open...` and select the `EnglishVault` directory.
2. Wait for Gradle sync to complete.
3. Run the `app` configuration on your device or emulator.

### Build from the command line

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (unsigned)
./gradlew :app:assembleRelease
```

The generated APK lives under `app/build/outputs/apk/`.

---

## Testing

```bash
# JVM unit tests
./gradlew :app:testDebugUnitTest

# Instrumented tests (requires a connected device or emulator)
./gradlew :app:connectedDebugAndroidTest
```

> The repository still ships with the default `ExampleUnitTest` and `ExampleInstrumentedTest` scaffolds. Real coverage for the data layer (`WordDao` dual-table pattern, `WordMapper`, `JsonLoader`, `UserProfileDao`), the seed flow (`DictionarySeeder` + the seven migrations) and the Words screen flow (`WordListViewModel` + chip + sort filters) will land in a dedicated testing phase.

---

## Roadmap

| Phase  |   Status   | Scope                                                                          |
| :----: | :--------: | ------------------------------------------------------------------------------ |
| 1      |   Done     | Data layer: DTO, mapper, entity, DAO, Room, one-time seed from JSON            |
| 2      |   Done     | UI shell + bottom navigation + visual mockups + blue theme                      |
| 2.5    |   Done     | Real CRUD for Words screen (Room-backed) + user_profile table + migrations       |
| 3      |   Done     | Split `words` into `core_words` + `user_words` + `words_view` + @DatabaseView   |
| 4      |   Done     | Rich expandable `WordCard` + Dictionary / Mine badges + tab counts + richer JSON |
| 4.5    |   Done     | Versioned seed (`DictionarySeeder`, `CORE_DICTIONARY_VERSION`, `MIGRATION_4_5`)  |
| 4.6    |   Done     | Dictionary split into per-type section files                                   |
| 5      |   Done     | Progress screen + tri-state `LearningStatus` + word `level` field              |
| 5.5    |   Done     | Edit form loads from Room + preserves id for `OnConflictStrategy.REPLACE`       |
| 6      |   Done     | Word Match Verbs mini-game + `DistractorGenerator` + branded loading screen    |
| 6.5    |   Done     | Per-category progression: `category_progress` table, XP grant + hybrid gate      |
| 6.6    |   Done     | Words screen: 8 type chips + sort row, in-place feedback colours on the game    |
| 7      |  Beta      | World map (replaces Test tab), persistent hearts and coins on `user_profile`   |
| 7.1    |  Done     | Settings hub (rename + sound sliders) + dictionary expansions                 |
| 7.2    |  Done     | Audio foundation: SFX on mini-game events, settings volume hooked in live    |
| 7.3    |  Done     | `SoundPool`-backed SFX + custom `correct_sound.mp3` + Letter Soup mini-game  |
| 7.4    |  Beta      | Dev toggle + WORLD mode in both mini-games, 10-level dictionary re-bucket    |
| 8      |  Planned   | Cloud sync, user accounts, multi-device                                        |

---

## Changelog

### Phase 7.4 - World-mode dev toggle + 10-level dictionary re-bucket

- **World-mode toggle on both mini-games** — `ModeToggleButton` in the top-right of `WordMatchVerbsGameScreen` and `LetterSoupGameScreen`. Flip between `NORMAL` and `WORLD` (or `HintMode.WORLD`) at any time; the toggle restarts the active run in the new mode without navigating away.
- **`Word Match Verbs` world mode** — 3 lives, 10-second per-question countdown, two `50/50` help items and two `+5s` time-boost items. Wrong answers and time-outs each cost a life; when lives reach zero the run ends with `outOfLives = true` instead of running through every question. The `dev/MAX_TIME_REMAINING_MS` cap stops boost chains from pinning a question open forever.
- **`Letter Soup` world mode** — 5-minute per-run countdown, two location-hint uses and two English-hint uses (the dedicated player-inventory system is out of scope for the beta; the counts are hard-coded caps surfaced through `locationHintsRemaining` / `englishHintsRemaining`). `WorldModeTimerBar` sits above the HUD row; turning red in the final 30 s. Hint buttons carry an `X / Y` counter and disable themselves when the inventory hits zero. Game-over is immediate when the countdown hits zero (`Finished.timedOut = true`).
- **`WordMatchAskType.THIRD_PERSON` removed** — the form was too predictable from the base verb. The game now alternates between `PAST_SIMPLE` and `PAST_PARTICIPLE`.
- **Dictionary re-bucketed to 10 levels (every category)** — `tools/redistribute_levels.py` sorts each section file by word length (alphabetical tiebreaker) and assigns `level = ceil((i + 1) * 10 / n)` so every level is populated in every category. **Entry count unchanged at 794**; per-entry content untouched. `DictionarySeeder.CORE_DICTIONARY_VERSION` bumped 13 → 14 so existing installs re-seed automatically. Total XP to unlock everything per category scales from 250 to 500 (10 × `XP_MIN_PER_LEVEL`).
- Both screens gate the toggle behind `private const val DEV_MODE_TOGGLE_ENABLED = true`. Set it to `false` to hide the toggle without touching anything else; delete the `if (...) ModeToggleButton(...)` block plus the `currentHintMode` / `currentMode` `StateFlow` to remove the feature entirely.

### Phase 7.3 - SoundPool-backed SFX + Letter Soup mini-game

- **`SoundEffectPlayer` rewritten with `SoundPool`** — the previous `ToneGenerator`-based implementation was gated by the system notification-stream volume, which made the Settings Effects slider useless. `SoundPool.Builder().setMaxStreams(4).setAudioAttributes(USAGE_GAME, CONTENT_TYPE_SONIFICATION).build()` plus a single loaded asset lets `setVolume(left, right)` apply per playback and ignore the OS gate. `SoundKey.Correct` now points at `R.raw.correct_sound` (the new `correct_sound.mp3` the user dropped into `app/src/main/res/raw/`); `Wrong` was removed because the user asked for one-shot feedback only.
- **New `res/raw/correct_sound.mp3`** — the first binary asset shipped with the APK. Plays on every successful answer in both mini-games.
- **New `ui/games/lettersoup/` package** — second playable mini-game, gated by `category_progress.LETTER_SOUP`. `LetterSoupLevelScreen` mirrors `WordMatchVerbsLevelScreen` (one card per level 1..10, dimmed past `unlockedLevel`). `LetterSoupGameScreen` renders the auto-scaling 8×8 / 10×10 grid, an always-visible Spanish translations list above it, and two hint buttons (`💡` location, `🔤` English). `BoardGenerator` places up to 5 words with one wrong letter each; the player taps the wrong-letter cell, taps the correct letter in the soup, and the swap is evaluated. The board never resets mid-run; the run ends when every placement is fixed. `LetterPalette` produces 26 distinct background hues (HSL evenly distributed) so each letter cell stands out. `EnglishLetterFrequency` fills the soup with the canonical English letter-frequency table (E=13, T=9, A=8, …). `LetterSoupEndContent` mirrors the Word Match Verbs end-of-run panel with score + "Play again" / "Back to games".
- **DAO: `WordDao.getCoreWordsByLengthAndLevel(level, min, max)` + `maxCoreLevelByLength(min, max)`** — the per-level pool the Letter Soup generator and level selector both query. Filters `source = 'core'`, `level = :level`, `LENGTH(word) BETWEEN :min AND :max`.
- **XP gating: `LetterSoupViewModel.tryUnlockLetterSoupLevel`** — pure-XP gate (no learned-percentage requirement because Letter Soup is a game, not a study tool). Caps the new unlocked level at the highest dictionary level available for the mini-game.

### Phase 7.2 - Audio foundation (correct-answer SFX)

- New `audio/SoundKey.kt` — enum carrying every short SFX the app can play. Phase 7.2 ships only `Correct` (active) plus `Wrong` and `Victory` as documented placeholders so future call sites compile against a stable shape. Each entry pairs a placeholder `ToneGenerator` constant with a relative `gain` so individual effects stay tunable from one place.
- New `audio/SoundEffectPlayer.kt` — `@Singleton @Inject constructor()` wrapper around `ToneGenerator`. Phase 7.2 ships zero audio assets so the player produces system DTMF-style beeps that work out of the box. Volume is recomputed and the generator is reallocated on every `play(key, effectsVolume)` call because `ToneGenerator.startTone` does not expose a per-call volume — recreating is the cleanest way to react to slider changes in real time. A volume of 0 short-circuits to a no-op before any allocation happens.
- `WordMatchVerbsViewModel` now reads `user_profile.effectsVolume` through a `StateFlow<Float>` and calls `soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)` from `submitAnswer(...)` whenever the user picks the right answer. The wrong-answer branch is intentionally silent for now; `Wrong` is declared but not fired so the call sites compile cleanly when the next iteration wires it up.
- The Effects slider in Settings has an immediate effect on playback: moving it during a mini-game re-evaluates on the next correct answer because the VM reads the current value straight from Room each time.

### Phase 7.1 - Settings hub + dictionary expansions

- New `ui/settings/SettingsScreen.kt` - two-section settings hub reachable from the Progress screen. The greeting row is now tappable (text + `Icons.Filled.Settings`) and surfaces a `Role.Button` semantics. Profile section shows the current display name and navigates to a dedicated sub-screen for renaming; Sound section hosts two `Slider`s (music + effects) with a `format("%d%%")` value label. Music carries a "Coming soon — background music will be added in a future update" hint under the slider so the placeholder is explicit.
- New `ui/settings/SettingsEditNameScreen.kt` - dedicated sub-screen with a pre-filled `OutlinedTextField` and `PrimaryButton`. Validation rejects empty / whitespace-only input through `SettingsEditNameViewModel`, which surfaces a stable error key and triggers `popBackStack` once the save flag flips.
- New `ui/settings/viewmodel/SettingsViewModel.kt` - `@HiltViewModel` exposing `profile: StateFlow<UserProfileEntity?>` plus `updateName`, `updateMusicVolume`, `updateEffectsVolume` setters. Each setter delegates to a dedicated atomic DAO method and clamps slider input to `[0.0, 1.0]`.
- New `ui/settings/viewmodel/SettingsEditNameViewModel.kt` - holds the form draft in a `MutableStateFlow<UiState>` (name / error / saved) and pre-fills from the current persisted profile on construction so the form opens populated.
- New `Destination.Settings` and `Destination.SettingsEditName` routes wired through `MainScaffold`; the Progress screen now accepts an `onOpenSettings` callback passed from the scaffold.
- `UserProfileEntity` gained two `Float` columns: `musicVolume` and `effectsVolume` (default `1.0f`). New companion constant `DEFAULT_VOLUME`.
- `UserProfileDao` gained `updateMusicVolume(Float)` and `updateEffectsVolume(Float)` (mirroring the existing `updateName` / `updateDailyGoal` pattern). Each one writes a single column on the single-row `user_profile` table.
- `Migrations.MIGRATION_8_9` - two `ALTER TABLE ADD COLUMN` statements (REAL NOT NULL DEFAULT 1.0). `AppDatabase` bumped to v9; registered in `DatabaseModule` next to the previous migrations.
- `DictionarySeeder.CORE_DICTIONARY_VERSION` bumped from 7 to 13 across the eight section files. Per-type entries:
  - `conjunctions.json`: 2 → 62 (coordinating + 6 subordinating sub-types + correlative + conjunctive adverbs).
  - `interjections.json`: 7 → 67 (greetings, polite markers, affirmation / negation, surprise, joy, frustration, pain, attention getters, hesitation fillers).
  - `nouns.json`: 9 → 69 (people / family, body parts, time, food, animals, home / furniture, places, common objects, abstract, nature, education, work).
  - `prepositions.json`: 2 → 62 (place, time, direction, manner, possession, plus common multi-word prepositions like `because of`, `in spite of`, `according to`, `due to`, `instead of`, `next to`).
  - `adjectives.json`: 87 → 147 (colors, taste, more emotions, personality, weather, time / state, abstract qualities, physical descriptors — the previous "12" in the README table was stale).
  - `adverbs.json`: 105 → 165 (linking, frequency / period, direction, place, degree, certainty, manner descriptors — the previous "3" in the README table was stale).
- Total dictionary now **624 entries** across the eight per-type section files (was 68 at the Phase 4.6 baseline). `dictionary/README.md` table totals and version history updated to match.

### Phase 7 - World map beta + persistent player state

- New `ui/world/WorldScreen.kt` - horizontal Super Mario Bros-inspired level map. The canvas is fixed at 2200 dp so the user must swipe right to discover all 10 nodes; a branching dirt path leads to a small shop drawn from primitive shapes, a castle with two stone towers and a yellow flag stands on the last waypoint, and five clouds float in the sky band. Tap detection now reads `scrollX` from the host `rememberScrollState` so node hits remain accurate after scrolling. The protagonist still slides between waypoints with an `Animatable`.
- New `ui/world/WorldViewModel.kt` - `@HiltViewModel` that exposes `UserProfileEntity` as a `StateFlow` so the screen can render the player's persistent hearts and coins live from Room.
- New HUD: a `HeartsPill` and a `CoinsPill` sit next to the BETA badge in the header, both fed by the strings `world_hearts_format` and `world_coins_format`.
- New `UserProfileEntity` columns: `hearts: Int = 5` and `coins: Int = 0` (plus a `DEFAULT_HEARTS` companion constant). The hearts default keeps a fresh profile playable out of the box.
- New `UserProfileDao` methods: `addHearts(amount)` and `addCoins(amount)`, atomic increments symmetric with the existing `addXp`. `addHearts` rejects negative deltas, `addCoins` accepts them because the shop will eventually spend the balance.
- `MIGRATION_7_8` (`AppDatabase` bumped to v8) - two `ALTER TABLE ADD COLUMN` statements that bring `hearts` (default 5) and `coins` (default 0) to existing installs without losing data.
- The Test tab was removed from the bottom navigation and replaced by World. The new destination keeps the same slot in the tab order so muscle memory carries over.

### Phase 6.6 - Game feedback + Words screen polish

- `OptionFeedback` reshaped from `{Correct, Wrong, Missed}` to `{Neutral, CorrectPicked, RevealedCorrect, WrongPicked}`. The correct option now lights up in **green** when the user picked it, and in **blue** when the user missed it, so the red X on the wrong pick no longer shares its colour with the right answer.
- `WordMatchVerbsLevelScreen` reads `WordMatchVerbsViewModel.maxUnlockedVerbLevel()` and dims every card beyond the player's highest unlocked verb level with a `Lock` icon and a "Aprende el nivel anterior" hint. Tapping a locked card is a no-op.
- `ProgressScreen.StreakBanner` swaps the previous fire-glyph `Text` for the Material `Icons.Filled.LocalFireDepartment`, keeping the streak section iconography consistent with the rest of the app.

### Phase 6.5 - Per-category progression

- New `category_progress` table (DB v7, `MIGRATION_6_7`) with one row per tracked category (`VERBS_REGULAR`, `VERBS_IRREGULAR`, `ADJECTIVES`, `ADVERBS`, `NOUNS`, `CONJUNCTIONS`, `PREPOSITIONS`, `INTERJECTIONS`), seeded on migrate.
- New `CategoryProgressDao` with `observeAll()`, `get(key)`, `seedIfMissing()`, `grantXpAndMaybeUnlock(...)` (atomic XP + level promotion).
- New `WordTypeFilter` enum lifted out of `WordListScreen.kt` and shared across the Words and Progress surfaces. Carries `type` and `regular` literals so DAO queries can target the right slice.
- New `data/game/CategoryGating.kt` constants: `XP_PER_CORRECT_ANSWER = 10`, `XP_MIN_PER_LEVEL = 50`, `LEARNED_PCT_REQUIRED = 0.80f`, `DEFAULT_UNLOCKED_LEVEL = 1`.
- `WordDao` gained `countWordsAt`, `countLearnedAt` and `maxLevelByType` for the gating evaluator.
- `WordMatchVerbsViewModel` now accumulates per-category XP during a run and grants it via `CategoryProgressDao.grantXpAndMaybeUnlock` at end of run. The DAO atomically resets `xpSinceLevelUp` when the hybrid gate passes.
- `WordMatchQuestion` carries `category: WordTypeFilter` and `wordLevel: Int` so the gameplay loop can credit the right category without re-querying Room.
- `WordMatchGameState.InProgress` carries `correctXpByCategory: Map<String, Int>` for end-of-run aggregation.
- `ProgressViewModel` exposes a new `categoryProgress: StateFlow<List<CategoryProgressUi>>` combining `CategoryProgressDao.observeAll()` with `WordDao.getAllWords()`. `ProgressScreen` replaces the Difficulty-bucket "Your path" list with eight `CategoryProgressCard`s in canonical order.
- New `data/game/CategoryGating.kt` + `CategoryProgressUi` data class + `CategoryProgressCard` composable.
- New strings: `progress_category_*` and `game_wordmatch_level_locked*`.

### Phase 6 - Word Match Verbs mini-game

- New `ui/games/wordmatchverbs/` package - level selector, game screen, in-place end-content composable, model, util, and ViewModel for the first playable mini-game.
- `DistractorGenerator` - single-character substitution that prefers vowel swaps (`a-e-i-o-u`) and falls back to phonetically close consonants (`b-p-v`, `d-t`, `g-c-k`, `f-v`, `s-z`, `m-n`, `l-r`) to produce plausible misspellings.
- `WordDao.getCoreWordsForGame(level)` + `maxCoreLevel()` - filtered query for game-eligible verbs (`source='core'`, `forms != null`, `status != 'LEARNED'`, `level = :level`).
- `WordMatchAskType` enum + `correctAnswer(word)` that resolves the right conjugation from `forms`.
- `WordMatchGameState` sealed class with `Loading`, `Empty`, `InProgress`, `Finished` sub-states, the latter carrying the error breakdown.
- `WordMatchVerbsViewModel` - `@HiltViewModel` that builds questions, accumulates errors, exposes `startGame(level)`, `submitAnswer(picked)`, `acknowledgeAnswer()`.
- `WordMatchVerbsLevelScreen` - grid 2xN of level cards with the count of eligible verbs; disabled when the count is 0 or the level is locked.
- `WordMatchVerbsGameScreen(level: Int)` - accepts the level via nav arg, kicks off `startGame(level)` from `LaunchedEffect`, renders the question and three option cards with the four-state feedback that auto-advances after 1.5 s.
- `WordMatchVerbsEndContent` - score card, "Words you missed" list, "Play again" / "Back to games" actions; rendered in place inside the game screen so the same VM survives the transition.
- Branded loading / empty panel - full-screen `Brush.verticalGradient` in the primary blue (`#1CB0F6`) with a centred `CircularProgressIndicator` so the user never sees a plain "Loading..." flash.
- `Destination.WordMatchEnd` removed; the results UI now lives inside the play destination so there is no cross-VM state to share.
- Max 20 questions per run (`MAX_QUESTIONS_PER_GAME = 20`), randomly sampled from the eligible pool.
- New strings: `game_wordmatch_*` (title, prompts, score format, errors, perfect, loading, locked).

### Phase 5.5 - Real edit flow

- `WordFormScreen` now loads the existing word from Room when `wordId` is non-null, replacing the previous hard-coded "Hello" / "Practice" mockup.
- `WordDao.getUserWordById(id)` - one-shot lookup scoped to `source = 'user'`.
- `WordListViewModel.loadWordForEdit(id)` and `updateUserWord(word)`.
- `toUserEntity(preserveId: Boolean = false)` - keeps the source id when editing so `OnConflictStrategy.REPLACE` updates the same row instead of inserting a duplicate.
- `MainScaffold` routes `onSave` to `updateUserWord` when `effectiveId != null`, otherwise `addUserWord`.

### Phase 5 - Progress screen, tri-state status, word levels

- `LearningStatus` enum (`NOT_LEARNED` / `ALMOST` / `LEARNED`) replaces the legacy `learned: Boolean` column on both word tables.
- `level: Int` column on `CoreWordEntity`, `UserWordEntity` and `WordEntity` (view) - independent from `Difficulty` and used to gate mini-game availability.
- `MIGRATION_5_6` - recreates both word tables, translates `learned -> status`, defaults `level = 1`, drops the legacy column.
- `WordDao.setStatusCore` / `setStatusUser` (dual-table pattern); the rest of the DAO queries now compare against `status` literals.
- `WordDto.level`; `DictionarySeeder.CORE_DICTIONARY_VERSION = 3`; the JSON now distributes the entries across level 1-2 buckets.
- `WordCard` gains a `StatusMenuButton` (`DropdownMenu` with three options) plus a `LevelChip`; both render on every card (core and user).
- `WordFormScreen` exposes a numeric `level` field with input sanitisation.
- `ProgressViewModel` (`@HiltViewModel`) exposes `profile`, `stats`, `xp` (`level + xpIntoLevel + xpRequired + nextLevel`), `dailyXp` (estimated from today's reviews via `countReviewsSinceFlow`), and `units` aggregated by `Difficulty` for the original "Your path" list.
- `ProgressScreen` rewritten to consume the VM end-to-end - greeting with `profile.name`, streak, level/exp card, daily goal %, per-difficulty progress rows.

### Phase 4.6 - Dictionary split into per-type section files

- The single `assets/words.json` is replaced by eight per-type section files under `assets/dictionary/`: `verbs_irregular.json`, `verbs_regular.json`, `interjections.json`, `nouns.json`, `adjectives.json`, `adverbs.json`, `prepositions.json`, `conjunctions.json`. Every entry keeps its original JSON shape; only the file boundary moved.
- `assets/dictionary/README.md` - single source of truth for the section layout, load order, field rules per grammatical type, and authoring procedure.
- `JsonLoader.loadWords()` now enumerates `SECTION_FILES` in declaration order and concatenates the decoded `WordDto` lists. `JsonLoader.SECTION_FILES` must stay in sync with the README table.
- `JsonLoader.loadWordsFile(filePath)` - overload for tests/tools that need to validate a single section in isolation.
- `DictionarySeeder.CORE_DICTIONARY_VERSION` bumped to track per-section-file content.
- Cross-entry normalisation - `go` had 4 examples in the original file (the only outlier); dropped to 3 so every entry has a uniform example count.
- KDoc + comments in 7 files (`AppDatabase`, `WordDao`, `WordDto`, `CoreWordEntity`, `UserProfileEntity`, `WordEntity`, `DictionarySeeder`) updated to reference `assets/dictionary/` instead of `assets/words.json`.

### Phase 4.5 - Versioned seed

- `data/seed/DictionarySeeder.kt` - `@Singleton` that compares `CORE_DICTIONARY_VERSION` against `UserProfileEntity.coreDictionaryVersion` and re-imports the JSON when the bundled version is newer. Exposes `seedIfNeeded()` for the bootstrap and `forceReseed()` for future debug flows.
- `UserProfileEntity.coreDictionaryVersion: Int = 0` field.
- `MIGRATION_4_5` - `ALTER TABLE user_profile ADD COLUMN coreDictionaryVersion INTEGER NOT NULL DEFAULT 0`. Existing installs get `0` so the next launch automatically re-seeds the upgraded dictionary.
- `AppDatabase` bumped to `version = 5`.
- `MainActivity` now delegates the seed routine to `DictionarySeeder.seedIfNeeded()` instead of running it inline.
- `WordDao.insertCoreWords` / `insertUserWord` / `deleteUserWord` / `deleteAllCoreWords` are the public write API; the legacy `insertWord` / `insertWords` / `deleteWord` methods are gone.

### Phase 4 - Rich Words screen

- `assets/words.json` grew from 10 to 68+ entries, with bilingual examples tagged by CEFR level (`A1` / `A2` / `B1` / `B2`), richer categories, synonyms, antonyms and tags.
- `WordDto` no longer carries the `source` field (every JSON entry is by definition core; the discriminator now lives in which table the row lives in).
- `WordCard` redesigned: chevron + avatar + word + translation + badges + chips, with `AnimatedVisibility` that reveals pronunciation, verb forms, examples (with `LevelBadge`), synonyms, antonyms, tags and category on tap. Per-card expansion state survives rotation via `rememberSaveable` + custom `ExpansionSaver`.
- Dictionary badge (with `MenuBook` icon) for `core_words`; Mine badge (`Person` icon) for `user_words`.
- Live `(n)` counts on every Words chip, computed in a single pass over `words`.
- New strings: `words_badge_dictionary`, `words_section_*`, `words_forms_*`, `words_card_expand`, `words_card_collapse`, plus labels for verb forms.

### Phase 3 - Table split (two tables + view)

- `words` split into `core_words` (seeded) + `user_words` (user-added) + `words_view` (`UNION ALL`).
- `CoreWordEntity` and `UserWordEntity` entities with identical schemas; `WordEntity` becomes a `@DatabaseView` over `words_view` with `source` synthesised as a literal.
- `WordMappers.kt` - `toUserEntity()` / `toCoreEntity()` extensions that convert view rows to writer entities (dropping `source`, resetting `id` to `0` so AUTOINCREMENT owns the primary key).
- `MIGRATION_3_4` - idempotent split: creates both tables, copies existing rows by `source`, drops the legacy `words`, recreates `words_view`.
- `WORD_DAO` rewritten with a dual-table update pattern (`setLearned`, `setFavorite`, `recordReview`, `setNextReview`, `setNotes`, `setCustomDifficulty` each fan out to two `@Query` calls).
- `WordDao.deleteUserWord` / `deleteAllCoreWords` - explicit, intentional table targets.
- `WordMapper.mapToCoreEntity` always sets `id = 0` so the JSON never controls the primary key (closes the id-collision footgun).
- `AppDatabase` bumped to `version = 4`.

### Phase 2.5 - Words wired to Room

- `WordListViewModel` (`@HiltViewModel`) exposing `StateFlow<List<WordEntity>>` plus `deleteWord` / `addUserWord` operations.
- `WordEntity.id` is now `autoGenerate = true`; user-added words get fresh ids without colliding with the JSON seed.
- `WordDao.deleteWord(id)` - single-row removal that persists.
- `MIGRATION_2_3` - recreates the `words` table with `AUTOINCREMENT` and registers the migration in `DatabaseModule`.
- `user_profile` table (`UserProfileEntity` + `UserProfileDao`) - XP, streak, daily goal, display name.
- `MIGRATION_1_2` - brings older installs into v2 so the profile table exists.
- `UserLevel` - pure functions for XP / level conversion used by the upcoming Progress screen.
- `WordCard` gained a `WordEntity` overload so the screen hands the entity straight through.
- `WordFormScreen.onSave` now emits a fully built `WordEntity`; the parent ViewModel persists it via `insertWord`.
- Read-only defaults enforced both in the UI (edit/delete icons hidden) and in the VM (`deleteWord` rejects non-user rows).
- Renamed "Singulars" tab to **Vocabulary** - clearer label for non-verb words.

### Phase 2 - UI shell & visual mockups

- Bottom navigation shell with four tabs (Progress, Games, Test, Words).
- Visual mockups for Progress (streak, XP, daily goal, learning path), Games (2x3 grid), Test (selectable difficulty), Words (in-memory CRUD with confirmation dialog).
- Blue Material 3 palette.
- Comprehensive English comments across `ui/`, `data/`, `di/`.
- README and project documentation.

> Note: the Phase 2 release originally shipped with four tabs (Progress, Games, Test, Words). The Test tab was retired in Phase 7 and replaced by the World map, which makes the level selector feel like a journey rather than a menu.

### Phase 1 - Data layer

- Room database with a single `words` table.
- Gson-based `JsonLoader` (now enumerates the per-type section files under `assets/dictionary/` - see Phase 4.6).
- `WordMapper` translating DTOs to entities with safe defaults.
- Five `TypeConverters` for nested objects and string lists.
- One-time seed on first launch (`WordDao.countWords() == 0`).
- Hilt graph via `DatabaseModule`.

---

## Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feat/my-feature`).
3. Commit your changes (`git commit -m "feat: add my feature"`).
4. Push the branch (`git push origin feat/my-feature`).
5. Open a Pull Request describing the motivation and approach.

Please follow the existing code style (Kotlin official, 4-space indent, KDoc on public APIs) and keep the data layer free of UI dependencies.

When you bump the bundled dictionary, edit the relevant file(s) under `assets/dictionary/`, keep its `README.md` in sync if you reorder or add a section, then bump the `CORE_DICTIONARY_VERSION` constant in `DictionarySeeder` - the seeder will re-import automatically on next launch.

---

## License

Released under the **MIT License**. See the [LICENSE](LICENSE) file for the full text.

---

## Credits

English Vault is built on top of these outstanding open-source projects:

- [Kotlin](https://kotlinlang.org) - JetBrains.
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Google.
- [Material 3](https://m3.material.io) - Google.
- [AndroidX Navigation Compose](https://developer.android.com/jetpack/compose/navigation) - Google.
- [Room](https://developer.android.com/training/data-storage/room) - Google.
- [Hilt](https://dagger.dev/hilt/) - Google.
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) - JetBrains.
- [Gson](https://github.com/google/gson) - Google.
- [KSP](https://github.com/google/ksp) - Google.

> Duolingo is a trademark of Duolingo Inc. This project is an unaffiliated fan implementation and is not endorsed by Duolingo.