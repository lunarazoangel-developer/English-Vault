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
- **Room storage v11** with five artefacts:
  - `core_words` — dictionary entries seeded from the `assets/dictionary/` section files. Conceptually read-only; the user can only update its user-state columns (`favorite`, `status`, `level`, `notes`, …) via the dual-table DAO pattern.
  - `user_words` — entries the learner added through the form. Fully mutable.
  - `words_view` — `UNION ALL` of both tables, exposed as the read-only `WordEntity` data class so the UI consumes a single model regardless of origin.
  - `user_profile` — single-row table for global XP, streak, daily goal, display name, dictionary seed version, the player's persistent hearts and coins counters, the music + effects volume sliders and the dark / light theme mode (Phase 8.2).
  - `category_progress` — one row per tracked grammatical category (`VERBS_REGULAR`, `ADJECTIVES`, …) holding cumulative XP, unlocked level and XP-since-last-promotion. Drives the per-category progression system on the Progress screen.
  - `skill_progress` — one row per language skill (`LISTENING`, `SPEAKING`, `READING`, `WRITING`, `GRAMMAR`) holding cumulative XP. Drives the Skill Progress bars on the Progress screen. Schema bump v9 → v10 via `MIGRATION_9_10`.
- **Eleven versioned migrations** that keep every previous install alive:
  - `MIGRATION_1_2` — adds the `user_profile` table.
  - `MIGRATION_2_3` — recreates `words` with `AUTOINCREMENT` ids.
  - `MIGRATION_3_4` — splits `words` into `core_words` + `user_words` + `words_view`.
  - `MIGRATION_4_5` — adds the `coreDictionaryVersion` column for versioned seeding.
  - `MIGRATION_5_6` — replaces the boolean `learned` column with a tri-state `status` enum and adds a `level: Int` to both word tables.
  - `MIGRATION_6_7` — adds `category_progress` and seeds one row per tracked category.
  - `MIGRATION_7_8` — adds the persistent `hearts` and `coins` counters to `user_profile` so the World map HUD can render the player's gamified state.
  - `MIGRATION_8_9` — adds the music and effects volume sliders to `user_profile` so the Settings screen can persist preferences before the audio engine ships.
  - `MIGRATION_9_10` — adds the `skill_progress` table and seeds one row per `Skill` (`LISTENING`, `SPEAKING`, `READING`, `WRITING`, `GRAMMAR`).
  - `MIGRATION_10_11` — adds the `consecutiveCorrect` counter to `core_words` and `user_words` and recreates `words_view` with the new column so the auto-marking pipeline (Phase 7.15) can persist each word's running racha.
  - `MIGRATION_11_12` — adds the `themeMode` column to `user_profile` so the Phase 8.2 dark / light toggle can persist across launches without data loss.
- **Five `TypeConverter`s** for nested objects (`Forms`, `Pronunciation`, `Example`) and string lists.
- **Hilt DI graph** wiring `AppDatabase`, four DAOs (`WordDao`, `UserProfileDao`, `CategoryProgressDao`, `SkillProgressDao`), `JsonLoader`, `WordMapper`, `DictionarySeeder` and the seed routine.
- **Versioned seeding** via `DictionarySeeder` (`@Singleton`): bumping `CORE_DICTIONARY_VERSION` in code triggers an automatic re-import of the bundled JSON on next launch, without losing user-added words or learning state.
- **Pure XP/Level math** in `data.database.UserLevel` (quadratic curve) — reused by both the global XP card and the per-category levels.
- **Per-category progression (Phase 4.6)** — eight parallel tracks. Correct answers in the mini-game grant XP per category. Advancing to the next level requires a hybrid gate: at least `XP_MIN_PER_LEVEL` XP earned at the current level **and** at least `LEARNED_PCT_REQUIRED` of the words at that level marked `LEARNED`. The DAO wraps each grant + promotion in a single transaction.
- **Centralised promotion gate (Phase 7.14)** — every call site that mutates `category_progress.unlockedLevel` for a grammatical category now goes through `data/game/PromotionGate.evaluate(...)` so the strict "XP + learned percentage" rule is enforced uniformly. The previous Letter Soup and Listening paths silently bypassed the learned-percentage check by hardcoding `meetsLearnedPct = true`; centralisation removed that inconsistency.
- **Manual LEARNED triggers the gate (Phase 7.14)** — when the user marks a word as `LEARNED` from the Words screen, `WordListViewModel.setStatus` re-evaluates the gate for the word's grammatical category with `amount = 0` (no XP granted for the manual mark, just a re-check). If the gate fires, a `PromotionEvent` is broadcast on `data/game/PromotionNotifier` and the Progress screen shows the celebration overlay. Marking as `LEARNED` is no longer purely cosmetic — it can unlock the next level on its own.
- **Level-up celebration overlay (Phase 7.14)** — a full-screen `Box` over the Progress dashboard that renders a `KonfettiView` burst (2 s emitter, 120 max particles, four-color palette) plus a Material 3 badge "Adjectives — Level 2 unlocked" scaled in with `animateFloatAsState` (0.6 → 1.0 over 360 ms). Auto-dismisses after 2.5 s and calls `viewModel.consumePromotionEvent()` so the overlay never replays on configuration change.
- **Process-wide event bus (Phase 7.14)** — `data/game/PromotionNotifier` is a `@Singleton` Hilt component holding a `MutableSharedFlow<PromotionEvent>` (`replay = 0`, `extraBufferCapacity = 8`). Late subscribers do not receive stale events; only emissions produced while a screen is composed are visible. The Words screen, the three mini-game VMs and any future emitter wire up via constructor injection.
- **Bottom navigation shell** with four tabs: Progress, World, Games, Words. The Test tab was replaced by a beta SMB-style World map that turns the level selector into a horizontal scrolling journey, complete with a 10-node path, a branching shop and a castle finale.
- **Words screen wired to Room** via `WordListViewModel` (`@HiltViewModel`, `StateFlow`). Create + Delete persist.
- **Eight type-filter chips** on the Words screen — regular verbs, irregular verbs, adjectives, adverbs, nouns, conjunctions, prepositions, interjections — plus `All` and `Mine`, in a horizontally-scrollable row.
- **Live sort row** — A-Z, Z-A, level ascending (`ASC`), level descending (`DESC`) — combined with the type filter and persisted across rotations via `rememberSaveable`. **Default sort is `ASC` (level 1 → max)** so newly-marked words bubble up to the bottom of the visible list as the user progresses through a level.
- **Paginated word list (Phase 7.14)** — the Words screen renders the first **20** cards (`PAGE_SIZE`) and grows by another 20 whenever the last visible item is within 4 of the rendered end (`LOAD_AHEAD_THRESHOLD`). Implementation uses `LazyListState` + `snapshotFlow` so the trigger reacts to every scroll tick (a plain `LaunchedEffect` keyed on `listState` only runs when one of its captured values changes, and none do during scroll — that was the original bug). Changing the type filter, sort order or search query resets the page window to 20 and scrolls back to the top so the user always starts at the head of the freshly-narrowed list.
- **Debounced free-text search (Phase 7.14)** — `OutlinedTextField` below the sort row with a leading magnifying-glass icon and a trailing `✕` clear button. Input is captured into a raw `searchQuery` state for a responsive field, then copied into `debouncedQuery` 300 ms after the last keystroke (`SEARCH_DEBOUNCE_MILLIS`) so the filter pipeline only recomputes when the user pauses. The search matches against the word, its translation, its category tags and its word tags (case-insensitive substring).
- **Read-only defaults** — `core_words` rows show a Dictionary badge (Material `MenuBook` icon) and have no edit / delete affordances; `user_words` rows show a Mine badge (Material `Person` icon) plus edit / delete icons. The `WordListViewModel` re-checks `isUserAdded()` before any delete so the invariant holds even if the UI regresses.
- **Rich expandable cards** — each card collapses by default to the header + badges and expands in place (with smooth animation and per-card state persisted via `rememberSaveable`) to reveal pronunciation, verb forms, examples with CEFR badges, synonyms, antonyms, tags and category.
- **Live tab counts** — every chip in the Words screen shows the number of words matching its filter.
- **Tri-state learning progress** — every word carries a `LearningStatus` (`NOT_LEARNED` / `ALMOST` / `LEARNED`) with a dedicated status menu button on every card. Picking a status persists immediately via the dual-table DAO.
- **Auto-marcado por mini-juegos (Phase 7.15)** — cada respuesta correcta en cualquier mini-juego (Word Match Verbs, Listening, Letter Soup) sube automáticamente el `LearningStatus` de la palabra: `consecutiveCorrect >= 1` → `ALMOST`, `>= 3` → `LEARNED`. Las respuestas incorrectas resetean el contador a `0`. El botón manual de la card sigue funcionando con cualquier dirección — un `LEARNED` puesto a mano nunca se pierde por un fallo en un mini-juego, y un `NOT_LEARNED` marcado por el usuario puede volver a promocionarse con nuevos aciertos. Implementado por `data/game/AutoStatusEvaluator.kt` (helper puro con la regla "solo promueve, nunca degrada") + `WordDao.setConsecutiveCorrect` dual-table + un hook de `submitAnswer` en cada uno de los tres ViewModels de mini-juego. Schema bump v10 → v11 vía `MIGRATION_10_11`.
- **Word progression levels** — both core and user words have an independent `level: Int` that gates availability in mini-games, so the learner is never overwhelmed by the whole dictionary at once. The level chip on every card renders in the compact `L5` form (no "Level" prefix) so it never clips on narrow screens.
- **Progress screen** — `ProgressViewModel` exposes the global profile, level / xp slice, daily-goal estimate, streak and one `CategoryProgressUi` per tracked grammatical category. Each per-category card carries its own level (1..N), an XP bar, a learned-percentage bar and a hybrid-gate status message.
- **Skill Progress section (Phase 7.6)** — a new "Skills" block sits between the daily-goal card and "Progress by category" and renders five infinite progress bars (Listening / Speaking / Reading / Writing / Grammar) as a 2-column × 3-row grid (2 + 2 + 1). Each card carries the skill icon, the cumulative XP, an optional `Cycle N` chip and a cyclic `LinearProgressIndicator` that fills to 1000 XP and resets. There is no level cap and no gating — the bars are a "how am I doing" measure. Backed by the new `skill_progress` table (schema v10, `MIGRATION_9_10`) and the `Skill` enum. Mini-games grant XP to their matching skill on every run: Word Match Verbs and Letter Soup → `READING`; Listening → `LISTENING`. Speaking and Grammar stay at `0 XP` until a future mini-game exercises them.
- **Word Match Verbs mini-game (Phase 8.3 overhaul)** — tap a level card to start a run of up to 20 randomly-picked questions; each asks about the past simple or past participle of one verb at the chosen level. The base verb is rendered with its Spanish translation to reinforce the meaning, and the four option cards include three distractors from a richer `DistractorGenerator` that mixes single-character substitution (`a ↔ e ↔ i ↔ o ↔ u` and phonetically close consonants), forced-regularisation (`be → beed`, `go → goed`, `run → runed`), and forced-irregularisation with invented suffixes (`ask → askought / askain`, `help → holpe`). Every candidate that collides with the base verb, the other conjugation of the same verb, or the answer itself is filtered before the options are shuffled. When the user picks wrong the correct answer is revealed with a blue check while their pick gets a red X. The level selector is now **symmetric**: both `VERBS_REGULAR` and `VERBS_IRREGULAR` must reach the same `unlockedLevel` before either advances to the next (no more asymmetric unlock where one category races ahead of the other). At end of run the per-category XP grant fires, the hybrid gate is evaluated, and the next level unlocks automatically when both requirements are met on the symmetric vocabulary. The end-of-run screen renders an **XP summary card** (Phase 7.10) showing per-category XP earned plus the total XP credited to the `READING` skill.
- **Listening mini-game (Phase 7.5)** — third playable mini-game, gated by `category_progress.LISTENING`. Reachable from **Games → Listening**. Each question picks a random core word and asks the device's `TextToSpeech` engine to pronounce it via `audio/TtsPlayer` (`@Singleton`, lazy init on first use). The player picks the correct spelling from four options; the correct option is revealed with a blue check, the wrong pick with a red X. WORLD mode adds 3 lives, a 10-second per-question countdown, 2 re-listens and a 50/50 hint item. End-of-run summary reuses the Word Match Verbs / Letter Soup XP-grant pipeline so per-category XP and `LISTENING` skill XP both persist.
- **World map (Phase 7, beta)** — Super Mario Bros-inspired level selector rendered as a single Canvas. The map is 2200 dp wide so the user must scroll horizontally to discover all 10 nodes, which trace an almost-straight path across a grass-and-sky scene. A branching dirt path leads to a small shop drawn from primitive shapes, a castle with two stone towers and a yellow flag stands on the last waypoint, and five clouds float in the sky band. A HUD in the header shows the player's persistent hearts and coins (read live from `UserProfileEntity` through `WorldViewModel`). Tapping the next waypoint advances the protagonist with a smooth `Animatable` interpolation.
- **Settings hub (Phase 7.1)** — reachable from the Progress screen via the greeting button (tap "Hello, Name" to open). Three sections: **Profile** (rename the user in a dedicated sub-screen with form validation), **Appearance** (Phase 8.2 — persistent dark / light theme picker backed by `user_profile.themeMode`), and **Sound** (music and effects volume sliders in `[0.0, 1.0]`, both persisted via Room; music is wired as a placeholder until the audio engine lands). Schemas bumps: v8 → v9 brings `musicVolume` and `effectsVolume` columns (`MIGRATION_8_9`); v11 → v12 adds `themeMode` (`MIGRATION_11_12`).
- **Arcade-style dashboard (Phase 8.1)** — the Progress screen and the bottom navigation bar render in an arcade / "physical chip on a table" design language: solid saturated colors (pink `#ff007a`, cyan `#00d4ff`, gold `#ffd700`, lime `#5fb878`), flat cards with a 4dp colored left border per category, 3D pill buttons with offset shadows, solid-color progress bars with no gradient, and pixel / display typography. Default variant is dark on a deep purple background; the user can flip to a paper-style light variant from the Appearance section. The arcade components live under `ui/progress/arcade/` and are composed on top of a `LocalArcadePalette` so a single `CompositionLocalProvider` swap retints the whole UI without touching call sites. Color palette tokens: `ArcadeColors.kt` (Dark / Light variants). Components: `ArcadeCard`, `ArcadeButton`, `ArcadeIconButton`, `ArcadeProgressBar`, `ArcadeChip`, `ArcadeSwitch`, `ArcadeLabel`. The persistent chrome (bottom bar) deliberately uses `ArcadePalettes.Dark` directly so it stays anchored to the dark variant regardless of the user's theme choice.
- **Persistent theme mode (Phase 8.2)** — `user_profile.themeMode` column (`MIGRATION_11_12`, default `DARK`) is read by `MainActivity` and passed into both `EnglishVaultTheme(darkTheme = …)` and `MainScaffold(themeMode = …)`. The latter derives the matching `ArcadePalette` (Dark / Light) and provides it to arcade-aware children. The toggle in Settings writes back via `UserProfileDao.updateThemeMode` and the change is applied live at the root of the Compose tree without restarting the activity.
- **Audio foundation (Phase 7.2)** — `audio/SoundEffectPlayer` plays short SFX on game events, currently backed by `ToneGenerator` so the app ships zero audio assets. WordMatchVerbs plays a positive beep (`TONE_PROP_ACK`, 300 ms) when the user picks the correct answer, and the volume reacts live to the **Effects** slider in Settings because the VM observes `user_profile.effectsVolume` through a `StateFlow`. `SoundKey` already declares `Wrong` and `Victory` placeholders so the wrong-answer / end-of-run sounds are a one-line addition when the team is ready.
- **SoundPool-backed SFX + custom assets (Phase 7.3)** — `SoundEffectPlayer` now wraps `android.media.SoundPool` with `AudioAttributes(USAGE_GAME, CONTENT_TYPE_SONIFICATION)` and loads `res/raw/correct_sound.mp3` once per process. `setMaxStreams(4)` lets overlapping beeps stack without losing the first one. `SoundKey.Correct` reuses that single asset; the per-playback `volume` argument honours the Effects slider 1:1 (the previous `ToneGenerator`-based path was gated by the system notification-stream volume, which `SoundPool.setVolume` bypasses).
- **Letter Soup mini-game (Phase 8.4 rewrite)** — real word-search mechanic, gated by `category_progress.LETTER_SOUP`. The level selector mirrors `WordMatchVerbsLevelScreen`. The play screen renders a fixed **12×12** grid (was 8×8 / 10×10 auto-scale) so the longest words still fit comfortably. Each round places up to **5** words at the chosen level, picked from a pool that **excludes verbs** (regular and irregular) so the dashboard reflects non-verb vocabulary. Placements use a fresh `Direction` enum (`E, W, N, S, NE, NW, SE, SW`) so words can run horizontally, vertically, on any diagonal, **and reversed** (a word placed with `W` direction reads from the end of the visible chain back to the anchor). Cells can be shared with other placements only when the letter matches. The player underlines a word by tapping cells in sequence; the active chain extends by king-move adjacency (8 directions, single step), backspaces on a re-tap of an earlier cell, and commits by tapping the **last cell** of the chain again — or the first one, for left-handed players. There is no move budget; the run ends when every placement is found or the world-mode timer runs out. Commits are matched against each unfound placement's `original` **or its reverse** so the player can underline the word from either end. Correct answers reveal in green for 350 ms; wrong attempts flash the wrong cells in red for the same interval. The Spanish translations list is **always visible** above the board; the location hint highlights every cell of one unfound word for `HINT_TIMEOUT_MS`, the English hint swaps that chip's translation inline for the same window.
- **Dev toggle for both mini-games (Phase 7.4)** — a small `ModeToggleButton` in the top-right of each play screen flips between `NORMAL` and `WORLD` (or `HintMode.WORLD` for Letter Soup). Flipping the toggle restarts the active run in the new mode so the change is applied without navigating away. World mode adds **lives** (3), a **per-question countdown** (10 s) for Word Match Verbs / a **per-run countdown** (5 min) for Letter Soup, and **limited hint items** in Letter Soup (2 location + 2 English). Hiding the toggle is one boolean flip (`DEV_MODE_TOGGLE_ENABLED`) at the top of each screen; removing it is one delete of the `if (...) ModeToggleButton(...)` block. The dedicated player-inventory system that will eventually back these items is out of scope for the beta — `consumeHintItemIfWorldMode` / `consumeHintItemIfWorldMode` are intentional `TODO` stubs and the inventories are hard-coded caps surfaced through the `*HintsRemaining` state fields.
- **Dictionary levelled up to 10 (Phase 7.4)** — `tools/redistribute_levels.py` re-bucketed every entry's `level` field across all 10 levels. Previously the levels were uneven: verbs went up to 5, nouns / prepositions / conjunctions / interjections only reached 2. The script sorts each section by word length (alphabetical tiebreaker) and assigns `level = ceil((i + 1) * 10 / n)` so every level is populated in every category. **Entry count unchanged at 794; per-entry content untouched.** Word Match Verbs and Letter Soup both render up to 10 level cards on their selectors; total XP to unlock everything per category scales from 250 (5 × `XP_MIN_PER_LEVEL`) to 500 (10 × `XP_MIN_PER_LEVEL`). `DictionarySeeder.CORE_DICTIONARY_VERSION` bumped 13 → 14 so existing installs re-seed automatically.
- **Word Match Verbs: 3rd-person removed (Phase 7.4)** — `WordMatchAskType.THIRD_PERSON` removed from the rotation because it was deemed too predictable from the base verb. The game now alternates between `PAST_SIMPLE` and `PAST_PARTICIPLE`.

### Planned

- **Custom audio assets** — drop `sfx_correct.ogg` / `sfx_wrong.ogg` / `sfx_victory.ogg` / `bgm_world.ogg` / `bgm_game.ogg` into `res/raw/` and swap `ToneGenerator` for `SoundPool` (`SoundEffectPlayer`'s public API stays the same).
- **Background music** — `MediaPlayer` reading the music slider live, looped per-screen (World map + mini-games only).
- **SRS-based review scheduling** powered by `lastReview` / `nextReview` fields (Phase 7.1).
- **Other mini-games** — Speed Quiz, Memory Cards, Translation Race, plus **Speaking** and **Grammar** mini-games that feed the empty skill bars (Phase 7.1+).
- **Difficulty / category chips** inside the Words screen — currently the screen exposes type chips (regular / irregular verbs, adjectives, …) and a sort row, plus the new free-text search (Phase 7.14). A separate difficulty filter (Easy / Medium / Hard) and a learning-status filter (Not learned / Almost / Learned) are queued for a follow-up phase.
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
| Celebration  | `nl.dionsegijn:konfetti-compose` 2.0.5              |

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
     | data/database  (AppDatabase v10)                      |
     |   +- CoreWordEntity   -> core_words  (AUTOINCREMENT)   |
     |   +- UserWordEntity   -> user_words  (AUTOINCREMENT)   |
     |   +- WordEntity       -> words_view  (@DatabaseView)   |
     |   +- UserProfileEntity -> user_profile (+ version,     |
     |   |                       hearts, coins, volumes)      |
     |   +- CategoryProgressEntity -> category_progress       |
     |   +- SkillProgressEntity -> skill_progress            |
     |   +- WordDao                                            |
     |   +- UserProfileDao                                     |
     |   +- CategoryProgressDao                                |
     |   +- SkillProgressDao                                   |
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
             |       |-- components/                  <- ArcadeBottomBar, PrimaryButton, LevelUpCelebration
              |       |   |-- ArcadeBottomBar.kt         <- arcade-style fixed-chrome bottom nav (Phase 8.1)
              |       |   `-- LevelUpCelebration.kt    <- KonfettiView + Material badge overlay (Phase 7.14)
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
              |       |   |   `-- arcade/                <- arcade-style UI (Phase 8.1)
              |       |   |       |-- ArcadePalette.kt           <- ArcadePalette + ArcadePalettes + LocalArcadePalette
              |       |   |       |-- ArcadeFonts.kt             <- display / pixel / body font tokens
              |       |   |       `-- components/
              |       |   |           |-- ArcadeCard.kt
              |       |   |           |-- ArcadeButton.kt
              |       |   |           |-- ArcadeProgressBar.kt
              |       |   |           |-- ArcadeChip.kt
              |       |   |           `-- ArcadeSwitch.kt
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
                 |   |-- AppDatabase.kt               (v12)
                 |   |-- Migrations.kt                <- 11 migrations (1->2 through 11->12; latest: MIGRATION_11_12 adds user_profile.themeMode, Phase 8.2)
                |   |-- UserLevel.kt                 <- XP/Level pure math (global + per-category)
                |   |-- dao/
                |   |   |-- WordDao.kt                <- dual-table updates, game queries, per-category counts
                 |   |   |-- UserProfileDao.kt
                 |   |   `-- CategoryProgressDao.kt    <- per-category XP / unlocked level
                 |   |-- entities/
                 |   |   |-- CoreWordEntity.kt         <- @Entity(core_words), status + level + consecutiveCorrect
                 |   |   |-- UserWordEntity.kt         <- @Entity(user_words), status + level + consecutiveCorrect
                 |   |   |-- WordEntity.kt             <- @DatabaseView(words_view)
                 |   |   |-- CategoryProgressEntity.kt <- @Entity(category_progress)
                 |   |   |-- LearningStatus.kt         <- enum: NOT_LEARNED / ALMOST / LEARNED + ordinal helper
                 |   |   |-- WordMappers.kt            <- toUserEntity() / toCoreEntity()
                 |   |   |-- UserProfileEntity.kt
                 |   |   `-- ProgressStats.kt
                 |   `-- converters/                  <- 5 type converters
|-- game/
                 |   |-- CategoryGating.kt            <- XP thresholds, learned %, tracked categories
                 |   |-- PromotionGate.kt             <- centralised hybrid gate (Phase 7.14)
                 |   |-- PromotionNotifier.kt         <- @Singleton SharedFlow<PromotionEvent> bus (Phase 7.14)
                 |   `-- AutoStatusEvaluator.kt       <- consecutive-correct → LearningStatus (Phase 7.15)
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
| 7.14   |  Done     | Words pagination + debounced search + level-up celebration + centralised gate   |
| 7.15   |  Done     | Auto-marcado de `LearningStatus` desde mini-juegos (racha de aciertos consecutivos) |
| 8      |  Planned   | Cloud sync, user accounts, multi-device                                        |
| 8.1    |  Done     | Arcade-style UI (Phase 8.1) — Progress screen + Settings + bottom bar re-skinned |
| 8.2    |  Done     | Persistent dark / light theme toggle persisted to `user_profile.themeMode`        |
| 8.3    |  Done     | Word Match Verbs: symmetric verb-category unlock + 4 distractors + translation hint |
| 8.4    |  Done     | Letter Soup: real word-search mechanic (8 directions + 12×12 + no-verbs pool + translation hint) |

---

## Changelog

### Phase 8.4 - Letter Soup: real word-search mechanic on a 12×12 grid

- **`Direction` enum on `LetterSoupWord`.** Eight reading directions (`E, W, N, S, NE, NW, SE, SW`). The word's full chain of cells is produced by stepping `length` times in this direction, so a single constructor flag covers horizontal, vertical, diagonal and reversed placements. `canPlace` updated to match any combination of anchor + direction that keeps the chain inside the board and does not overlap a cell holding a *different* letter (matching letters may share cells, so placements can cross).
- **`BoardGenerator.tryPlace` rewritten.** Computes a valid anchor range per direction (the last letter must still fit inside `[0, boardSize)`), retries up to `400` times (`MAX_PLACEMENT_ATTEMPTS`) before giving up on a word. The `MAX_PLACEMENT_ATTEMPTS` bump absorbs the new combinatorics of 8 directions vs. the previous 2.
- **12×12 fixed grid.** `LetterSoupBoard.DEFAULT_BOARD_SIZE = 12` replaces the previous 8 + 10 conditional. `EXTENDED_BOARD_SIZE` and `EXTENDED_THRESHOLD` removed. `MAX_WORD_LENGTH = 12` in `LetterSoupViewModel` so the longest words still fit across diagonals. Board auto-resize logic removed from the screen.
- **`LetterSoupCell` slimmed to three roles.** `Soup`, `InSelection` (new — the cell is currently part of the player's underlining chain), and `WordFixed`. `WordCorrect` and `WordWrong` deleted — the word-search mechanic never reveals which cells belong to an unfound word, so they cannot exist as a distinct role.
- **`LetterSoupGameState` rewritten.** Replaced `selectedCell`, `movesLeft`, `lastSwapFailedCells`, `lastFixedWord` with `selectedCells: List<Pair<Int, Int>>` (the chain in progress), `wrongFlashCells` (transient red flash), `lastFoundWord` (green flash on the just-fixed placement), and `highlightedPlacement: LetterSoupWord?` (the location hint target). `WRONG_FLASH_TIMEOUT_MS = 350` (was 600 ms) — short enough that the player can start a new chain quickly without losing feedback. `MAX_MOVES` removed.
- **`LetterSoupViewModel.onCellTapped` — five-branch input model.** (1) Empty selection → start new chain (and clear flash so the previous commit does not bleed). (2) Tap the **last** cell of a 2+ cell chain → commit. (3) Tap the **first** cell of a 2+ cell chain → commit (alternative gesture for left-handed players). (4) Tap a middle cell of the chain → truncate (backspace). (5) Tap a king-move-adjacent cell → extend. Anything else is ignored. **Taps are never blocked** during the wrong / found flash — the player can start a new chain immediately without waiting for the animation to finish.
- **`commitSelection` matches the typed string against each unfound placement's `original` OR its reverse.** This means the player can underline the word from either end without losing — a placement stored as `direction = W` with `original = "cat"` can be underlined as `CAT` (left-to-right) or as `TAC` (right-to-left), and both count. The `original` is also surfaced on the play screen so the player still sees the canonical form.
- **`LetterSoupViewModel.startGame` filters out verbs.** After fetching the words for the level from `wordDao.getCoreWordsByLengthAndLevel(level, MIN, MAX)`, the VM drops every row whose `type == "verb"` so Letter Soup only ever offers non-verb vocabulary. `maxLetterSoupLevel` and `wordsAtLevel` apply the same filter. No DAO or schema change required.
- **`LetterSoupViewModel.commitSelection` grants XP and applies auto-marking per-fixed-placement.** Same pipeline as the other two mini-games, gated by `PromotionGate.evaluate(...)` and the `consecutiveCorrect` bump so that finding a word in Letter Soup pushes the same auto-mark onto the underlying `WordEntity`.
- **`LetterSoupGameScreen.LetterCell` redesigned for the new mechanic.** Removes `isSelected` (single-cell selection), `isFailedSwap`, `showWrongMarker`. Adds `isInSelection` (amber border 3 dp) and a distinct `isLastSelected` (orange-red border 4 dp) for the cell the player should re-tap to commit. The wrong-letter hint badge removed entirely — the wrong path is now a transient red flash on the submitted cells only.
- **Translation hint preserved on the play screen.** The Spanish → English flip on the first unfixed chip keeps working (English hint button); the location hint now highlights every cell of one unfound word's chain for `HINT_TIMEOUT_MS`.
- **Selected-cell border is now visibly distinct.** Previous code used `MaterialTheme.colorScheme.tertiary` (a muted purple) which blended with the dark background and made the selection hard to follow. The new version uses `Color(0xFFFFB300)` (amber) for the chain plus `Color(0xFFFF3D00)` (red-orange) for the last cell so the commit-ready cell is unmissable.

### Phase 8.3 - Word Match Verbs: symmetric unlock + 4 distractors + translation hint

- **Symmetric verb-category unlock.** `WordMatchVerbsViewModel.maxUnlockedVerbLevel()` now returns `min(VERBS_REGULAR.unlockedLevel, VERBS_IRREGULAR.unlockedLevel)` instead of the previous `max(...)`. The user can only advance to the next level when **both** the regular track and the irregular track have reached it. Stops the asymmetric unlock path where earning enough XP for one track was enough to open the higher level on the other.
- **Four options per question (was three).** `WordMatchVerbsViewModel.startGame` calls `DistractorGenerator.generate(correct, count = 3, baseWord = word.word, otherValidForm = otherForm)` instead of `count = 2`. The 4-option layout was already supported by the screen; the change is purely on the generator + VM.
- **`DistractorGenerator` mixes invented-verb forms with single-char typos.** New strategies in the generator's STRATEGIES list: `regularize` (append `-ed` / `-d`, e.g. `be → beed`, `go → goed`, `run → runed`), `regularizeEn` (append `-en`, e.g. `go → goen`), `irregularize` (append irregular suffix from `IRREGULAR_SUFFIXES = {"ought", "ain", "ept", "oke", "ung", "own"}`, e.g. `ask → askought / askain / askept`), `irregularizeWithVowelShift` (change the last vowel and append an irregular suffix, e.g. `ask → eskought`, `help → holpe`), and `doubleMutation` (regularize + a vowel swap, e.g. `beed → baed`). All strategies return `null` on inputs too short to mutate (avoids infinite loops).
- **Distractor post-filters.** Every candidate generated is now checked case-insensitively against three sets before being admitted: (a) the `correct` answer, (b) the `baseWord` (the verb root, so a fake distractor like `asked` is never offered as an option when the root is `ask`), and (c) the `otherValidForm` (the OTHER conjugation of the same verb — `was` is never offered when answering for `been`, and vice-versa). The retry cap is `count * 10` so most levels find three unique distractors in two to three seconds.
- **Translation hint on the prompt card.** The prompt card now renders the Spanish translation directly under the base verb. The "puzzle" stays the same (pick the correct conjugation from the four options), but the player who is learning what the verb means gets reinforcement on every question — the answer-distractors do not show translations because doing so would trivialise the recognition task.
- **Selection border is now visibly distinct.** Previous code used `MaterialTheme.colorScheme.tertiary` (muted purple) which blended with the dark background and made the selection hard to follow. The new version uses `Color(0xFFFFB300)` (amber) at 3 dp for the chain and `Color(0xFFFF3D00)` (red-orange) at 4 dp for the last cell so the commit-ready cell is unmissable. The wrong-flash timeout was reduced from 600 ms to 350 ms.

### Phase 8.2 - Persistent theme mode

- **`UserProfileEntity.themeMode: String` column** with companion constants `THEME_MODE_DARK = "DARK"`, `THEME_MODE_LIGHT = "LIGHT"`, `DEFAULT_THEME_MODE = "DARK"`. A fresh install lands in dark mode so the design iteration underway has a stable default.
- **`MIGRATION_11_12`** — `ALTER TABLE user_profile ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'DARK'`. Existing installs pick up `DARK` on next launch without losing data.
- **`AppDatabase`** bumped 11 → 12. **`DatabaseModule`** registers the new migration alongside the previous ten.
- **`UserProfileDao.updateThemeMode(mode)`** — `@Query("UPDATE user_profile SET themeMode = :mode WHERE id = :id")` with default primary key. No validation in the DAO; the validation lives in the VM where the user can only pick one of the two strings.
- **`SettingsViewModel.themeMode: StateFlow<String>`** derived from `profile.map { it?.themeMode ?: DEFAULT }`. New `setThemeMode(mode)` method sanitises the input (`mode` must be `THEME_MODE_DARK` or `THEME_MODE_LIGHT`; anything else is a silent no-op) and writes via the DAO. The flow feeds the `SettingsScreen.AppearanceSection` so the picker stays in sync with the database after a toggle.
- **`EnglishVaultTheme(darkTheme: Boolean, …)`** — the previous default `darkTheme = isSystemInDarkTheme()` is gone. The caller (`MainActivity`) is the single source of truth for the theme.
- **`MainActivity.setContent`** reads `userProfileDao.observeProfile()` via `collectAsState` and derives both `isDark = themeMode == THEME_MODE_DARK` for the M3 theme and a `themeMode: String` for `MainScaffold(themeMode = …)`. The toggle in Settings is reflected live at the next recomposition — no activity restart needed.
- **`SettingsScreen.AppearanceSection`** — new section with two pill buttons (Dark / Light), accent `palette.primary`, M3-default theme-aware variant for now. A future Phase 8.1 will re-skin the whole Settings screen in the arcade style.
- New `strings.xml` keys: `settings_section_appearance`, `settings_theme_label`, `settings_theme_dark`, `settings_theme_light`, `settings_theme_hint`.

### Phase 8.1 - Arcade-style UI for the Progress screen, Settings + bottom navigation bar

- **Arcade palette tokens** live in `ui/progress/arcade/ArcadePalette.kt`. `data class ArcadePalette(primary, secondary, highlight, success, background, surface, surfaceDark, border, ink, textMain, textDim, shadow, switchOff)` with `categoryColor(filter)` and `skillAccent(index)` helpers and a `shadowOf(color)` darkener for offset 3D buttons.
- **`ArcadePalettes` companion object** holds the two static palettes. `Dark` is the canonical design (deep purple background, cream text, pink/cyan/gold/lime accents). `Light` is the paper-style variant (cream background, dark text, same accents) — exactly the same hue for category and accent colors so the design language is invariant under theme.
- **`LocalArcadePalette = staticCompositionLocalOf { ArcadePalettes.Dark }`** — a single CompositionLocal. `MainScaffold` provides the active palette at the root of the Compose tree so every arcade-aware child can retint by reading the local.
- **Five `ArcadeCard` variants** (`Card`, `Button`, `IconButton`, `ProgressBar`, `Chip`, `Switch`, `Label`) — solid colors, no gradients, no blur. The 3D button effect is built from two stacked `Box`es (one shadow, one face), animated with `interactionSource.collectIsPressedAsState()` + `animateDpAsState()` (no opacity drops). Progress bars animate with `animateFloatAsState` driven by a Newton-Raphson solver for `cubic-bezier(0.16, 1, 0.3, 1)`.
- **`ProgressScreen` rewritten from scratch.** Renders on `palette.background` (dark or light per the active palette), every text uses `palette.textMain` / `palette.textDim` / `palette.ink`, every card border is `palette.categoryColor(filter)`, every progress bar is `palette.xxx`. Section headers in the display font (placeholder `FontFamily.SansSerif + ExtraBold`), labels in the pixel font (placeholder `FontFamily.Monospace + Bold`). KDoc explains how to swap in real Bungee / Press Start 2P TTFs when the font files land in `res/font/`.
- **`ArcadeBottomBar` (new, replaces the M3 `NavigationBar`)** — always uses `ArcadePalettes.Dark` directly because the chrome is intentionally theme-invariant. The active tab is a flat (no-shadow) pill with `palette.primary` fill + `palette.ink` text/icon; inactive tabs are transparent with `palette.textDim`. Same nav semantics as the previous `AppBottomBar`: popUpTo start destination + saveState + launchSingleTop + restoreState. `ArcadeBottomBar.kt` lives in `ui/components/` because it is a top-level chrome element, not a screen-specific widget.
- **`MainScaffold(themeMode: String)`** — accepts the persisted theme mode, derives the matching `ArcadePalette`, wraps the entire subtree in `CompositionLocalProvider(LocalArcadePalette provides palette)` so every arcade-aware screen reflects the toggle instantly. The bottom bar `ArcadeBottomBar` is wired as the new `bottomBar` slot.
- **`AppBottomBar.kt` and `ui.components.SectionHeader.kt` removed.** Their callers were fully migrated; no remaining references.
- **`ProgressScreen` no longer reads from `MaterialTheme.colorScheme`** for backgrounds, surfaces, or text colors — it reads from `LocalArcadePalette.current`. The M3 theme still controls the rest of the app (Games, World, Words screens) via the regular `EnglishVaultTheme(darkTheme = ...)`.
- **No new dependencies.** The whole feature is plain Compose with the 2025.06 BOM that was already in `gradle/libs.versions.toml`.

### Phase 7.15 - Auto-marcado de palabras desde mini-juegos

- **Esquema bump v10 → v11 (`MIGRATION_10_11`).** Nueva columna `consecutiveCorrect INTEGER NOT NULL DEFAULT 0` en `core_words` y `user_words`, y `words_view` recreado para proyectarla. Sin pérdida de datos: la columna tiene `DEFAULT 0`, así que cada fila existente conserva su `status` actual y la racha arranca vacía hasta que el usuario juega un mini-juego.
- **`WordDao.setConsecutiveCorrect(id, value, timestamp)`** — nuevo método dual-table (par `setConsecutiveCorrectCore` / `setConsecutiveCorrectUser`) que actualiza la racha + `lastReview` en una sola escritura. El caller decide el `value` (incremento o reset); el DAO nunca interpreta.
- **`data/game/AutoStatusEvaluator.kt`** — helper puro que mapea `(LearningStatus actual, consecutiveCorrect)` a un `LearningStatus` candidato con la regla "solo promueve, nunca degrada":
  - `consecutiveCorrect >= 3` → candidato `LEARNED`.
  - `consecutiveCorrect >= 1` → candidato `ALMOST`.
  - si no → candidato = `current`.
  - resultado = `LearningStatus.max(current, candidato)` (un `LEARNED` manual sobrevive a cualquier reset de racha; un `NOT_LEARNED` manual puede re-promocionarse con nuevos aciertos).
- **`LearningStatus.Companion.max(a, b)`** — comparador ordinal (`LEARNED > ALMOST > NOT_LEARNED`) usado por el evaluator y expuesto como util privado del companion.
- **Modelos de pregunta enriquecidos.** `WordMatchQuestion`, `ListeningQuestion` y `LetterSoupWord` ahora llevan un `wordId: Int` para que el VM de cada mini-juego pueda escribir sobre la fila exacta sin un lookup extra por texto. `BoardGenerator.generate` acepta un nuevo `wordIds: Map<String, Int>` que se estampa a cada `LetterSoupWord` durante la colocación.
- **Hooks en los tres ViewModels de mini-juego.** Cada `submitAnswer` (Word Match Verbs, Listening) llama a un `applyAutoStatus(wordId, isCorrect)` que bumpea/resetea `consecutiveCorrect` y, si el evaluator devuelve un status mayor, persiste el `LearningStatus` vía `wordDao.setStatus`. En `LetterSoupViewModel` el hook vive en `attemptSwap` y se dispara por cada placement fijado (el swap fallido NO resetea la racha porque no es un fallo de conocimiento por palabra). Todas las escrituras van dentro de un `viewModelScope.launch { runCatching { … } }` para no bloquear la UI ni crashear el juego si Room falla.
- **Botón manual intacto.** `WordListViewModel.setStatus` y `WordCard.StatusMenuButton` no se tocan. La regla "solo promueve" garantiza que un `LEARNED` puesto a mano sobrevive a cualquier cantidad de respuestas incorrectas en mini-juegos; un `NOT_LEARNED` marcado manualmente vuelve a promocionarse cuando el usuario acumule nuevos aciertos consecutivos.
- **Sin cambios de UI.** El `Flow<List<WordEntity>>` re-emite cuando cualquier campo cambia, así que el ícono del status menu en `WordCard` cambia de color en cuanto la promoción se persiste. La sección "Available now" del README ahora menciona el comportamiento junto a "Tri-state learning progress".

### Phase 7.14 - Words search + pagination, level-up centralisation, celebration overlay

- **Words screen pagination (infinite scroll, 20 cards per page).** The list starts with `PAGE_SIZE = 20` cards and grows by another 20 whenever the last visible item is within `LOAD_AHEAD_THRESHOLD = 4` of the rendered end. The trigger is wired with `LazyListState` + `snapshotFlow { layoutInfo }` — a plain `LaunchedEffect` keyed on `listState` never re-fires during scroll because none of its captured values change. `LaunchedEffect(selectedType, sortOrder, debouncedQuery)` resets `visibleCount` to 20 and `scrollToItem(0)` whenever the user changes a filter or query, so the user always starts at the head of the freshly-narrowed list.
- **Debounced free-text search.** `OutlinedTextField` below the sort row with a leading `Icons.Filled.Search` and a trailing `✕` clear button (visible only when the query is non-empty). `searchQuery` is updated on every keystroke for a responsive field; `debouncedQuery` lags by `SEARCH_DEBOUNCE_MILLIS = 300 ms` and is what the filter pipeline reads. The match is a case-insensitive substring against `word`, `translation`, `category` and `tags`. New strings `words_search_hint` and `words_search_clear_cd`.
- **LevelChip shortened to `L5`.** `WordCard.LevelChip` now renders `"L$level"` instead of `R.string.words_level_badge` ("Level %1$d") so the chip never clips on narrow screens. The string resource is left in `strings.xml` as a harmless orphan.
- **Sort options renamed.** The two level sort chips now read `ASC` / `DESC` instead of `Level 1 → 5` / `Level 5 → 1` (`words_sort_level_asc`, `words_sort_level_desc`). Default sort is `SortOrder.LEVEL_ASC` so newly-marked words bubble up as the user advances through a level.
- **`PromotionGate` centralises the hybrid gate.** New `data/game/PromotionGate.kt` exposes a single `evaluate(categoryKey, amount, wordDao, categoryProgressDao)` that reads the row, computes `meetsXp` + `meetsLearnedPct` + `targetUnlockedLevel`, calls `grantXpAndMaybeUnlock` and returns a `PromotionOutcome` (`Skipped` / `Held` / `Promoted(previousLevel, newLevel)`). Every grammatical-category call site now goes through this helper, removing the previous inconsistency where `LetterSoupViewModel.grantPerCategoryXp` and `ListeningViewModel.grantPerCategoryXp` hardcoded `meetsLearnedPct = true`. The synthetic buckets `LETTER_SOUP` and `LISTENING` keep their XP-only gate because they are not tied to per-word learned status — they are routed to their dedicated `grant*LevelXp` helpers inside the same VMs and stay out of `PromotionGate`.
- **`PromotionNotifier` — process-wide SharedFlow bus.** New `data/game/PromotionNotifier.kt` is a `@Singleton` that exposes a `SharedFlow<PromotionEvent>` with `replay = 0` and `extraBufferCapacity = 8`. Every call site that observes a `PromotionOutcome.Promoted` (Words screen, three mini-game VMs) emits a `PromotionEvent(categoryKey, previousLevel, newLevel, timestamp)` so any listening screen can render a celebration. Late subscribers do not replay past events.
- **Manual LEARNED re-evaluates the gate.** `WordListViewModel.setStatus` now reads the previous word via the new `WordDao.getWordById(id)` one-shot, and on the `→LEARNED` transition calls `PromotionGate.evaluate(categoryKey, amount = 0, …)`. No XP is granted for the manual mark (per the design request), but the gate fires immediately when the user reaches the XP + learned-percentage threshold by hand, the category is promoted, and a `PromotionEvent` is broadcast so the celebration runs even when no mini-game has been played yet.
- **Level-up celebration overlay.** New `ui/common/LevelUpCelebration.kt` wraps `nl.dionsegijn:konfetti.compose:2.0.5`'s `KonfettiView` (2 s emitter, 120 max particles, four-color palette `0xfce18a 0xff726d 0xb48def 0xf4306d`) plus a Material 3 badge with the category name and the new level, scaled in with `animateFloatAsState(0.6 → 1.0, 360 ms)`. The whole overlay is wrapped in `AnimatedVisibility` with a semi-transparent black backdrop, sits in a `Box` over the Progress screen so it covers every other composable, auto-dismisses after 2.5 s and calls `ProgressViewModel.consumePromotionEvent()` so it never replays on configuration change.
- **`ProgressViewModel` exposes the bus as a StateFlow.** New `promotionEvent: StateFlow<PromotionEvent?>` driven by a `viewModelScope.launch { promotionNotifier.events.collect { … } }` subscriber. The host composable reads it with `collectAsState` and passes it to `LevelUpCelebrationOverlay`. New `consumePromotionEvent()` setter clears the StateFlow after the celebration finishes.
- **WordDAO: `getWordById(id)` one-shot.** New `suspend fun getWordById(id: Int): WordEntity?` backed by `SELECT * FROM words_view WHERE id = :id LIMIT 1`. Used by `WordListViewModel.setStatus` to fetch the row before mutating its `status`.
- **`WordTypeFilter.classifyOrNull(word)`** — nullable variant of `classify(word)` returning `null` when the word belongs to no tracked grammatical bucket. Lives in the `companion object` so it can be called as `WordTypeFilter.classifyOrNull(word)` from non-instance contexts (notably `PromotionGate`).
- **Tech stack: `nl.dionsegijn:konfetti-compose` 2.0.5** added to `gradle/libs.versions.toml` and `app/build.gradle.kts`.

### Phase 7.13 - Skill XP grant hardened with `seedIfMissing` at DAO level

- **`SkillProgressDao.grantXp` now wraps a raw `@Query` with a defensive `seedIfMissing`**, mirroring the Phase 7.11 fix for `category_progress`. Without this, an install that for any reason lacked the row (very old pre-migration install, a wiped table, or a path that bypassed `MIGRATION_9_10`) silently dropped every skill XP grant because Room's `@Update` only touches existing rows.
- The raw `@Query` is renamed `grantXpInternal`; the default `grantXp` (the only public entry point callers should use) seeds the row first and then delegates to `grantXpInternal`. Signature stays the same so the three mini-game VMs need zero changes.
- The XP summary added in Phase 7.10 makes the silent failure visible: the summary card on the Word Match Verbs end screen now reliably reflects the run's skill XP. After a 10-question run the Reading card on the Progress screen actually moves from `0 XP` to its new value.

### Phase 7.12 - ProgressScreen `categoryProgress` flow switched to `Eagerly`

- **`ProgressViewModel.categoryProgress` now uses `SharingStarted.Eagerly`** instead of `SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)`. With `WhileSubscribed`, the upstream was cancelled 5 s after the user left the Progress tab and re-subscribed on return — but the re-subscription wasn't always re-firing Room's invalidation on `words_view` (a `UNION ALL` over `core_words` + `user_words`), which left the learned bar stale when the user marked words from the Words tab.
- `Eagerly` keeps the upstream alive for the whole `viewModelScope` lifetime, so any write — game XP grant, word status change, `skill_progress` grant — propagates immediately. Other ProgressViewModel flows still use `WhileSubscribed`; the `skills` flow was already `Eagerly`.
- Minimal cost: a few extra Room Flow collectors alive while the ProgressViewModel is in scope (the entire app session for users who ever visited the Progress tab). Acceptable for an offline-first vocabulary trainer.

### Phase 7.11 - Defensive `seedIfMissing` in Word Match Verbs grant

- **`WordMatchVerbsViewModel.tryUnlockCategory` now calls `categoryProgressDao.seedIfMissing(categoryKey)`** before reading the row. Without this, an install that for any reason lacked the row (very old pre-migration installs, a wiped table, or any path that bypassed `MIGRATION_6_7`) would silently drop every XP grant because Room's `@Update` only touches existing rows.
- The XP summary added in Phase 7.10 made the silent failure visible (the summary showed the run's XP but the Progress screen never moved). This fix closes the loop: the run's XP is now actually persisted to `category_progress`, so the next time the Progress screen reads the row, the XP bar / learned bar reflect the new totals.
- The same `seedIfMissing` pattern was already used by `ListeningViewModel.grantPerCategoryXp` and `LetterSoupViewModel.grantPerCategoryXp`. Word Match Verbs is now consistent with them.

### Phase 7.10 - XP summary at the end of Word Match Verbs

- **New "XP earned this run" card on the Word Match Verbs end screen.** Renders the XP the run credited, broken down by grammatical category (e.g. `Verbs Regular: +50 XP`, `Nouns: +20 XP`) and by skill (`Reading: +70 XP`). When the run earned zero XP the card collapses to an empty-state message.
- **`WordMatchGameState.Finished` gains a `correctXpByCategory` field** so the end screen can render the breakdown. The VM already accumulated the per-category XP during the run via `submitAnswer`; this change only forwards it through the `Finished` state so the UI can read it.
- **Diagnostic value:** if the user sees the XP breakdown at end-of-run but not on the Progress screen, the bug is in the Progress UI display (Phase 7.9 already aligned the card to `unlockedLevel`). If they don't see XP at the end screen either, the bug is in the grant pipeline itself.
- **Only Word Match Verbs is wired in this phase** — Listening and Letter Soup carry the same data but their end screens were not updated. Can be replicated later.

### Phase 7.9 - Category card level aligned with the hybrid gate

- **Category card now uses `CategoryProgressEntity.unlockedLevel` for both the level chip and the learned-bar bucket**, instead of `UserLevel.levelFromXp(xpTotal).coerceIn(1, maxLevel)` (the XP-derived "rank"). Previously the two diverged: earning enough XP for level 2 (100 XP) made `derivedLevel = 2` while `unlockedLevel` stayed at 1 because the hybrid gate (50 XP + 80% learned) wasn't met. The learned bar then filtered at `level == 2` and showed 0% even when the user had marked level-1 words as LEARNED — those words were never counted because the bucket was at the wrong level.
- Aligning the displayed level with `unlockedLevel` keeps the card in sync with what `tryUnlockCategory` / `grantPerCategoryXp` actually advance. Marking a level-1 word as LEARNED now updates the learned bar immediately; the bar fills toward 80% and the level chip climbs when the gate passes.
- **Only `ProgressViewModel.buildCategoryProgress` is touched** — the data layer, DAOs, mini-game VMs and gating logic were already correct (they all operated on `unlockedLevel`).

### Phase 7.8 - Category XP bar uses promotion cycle

- **XP bar in `CategoryProgressCard` now reflects the current promotion cycle** instead of the cumulative level curve. Previously the bar was driven by `UserLevel.levelProgress(row.xpTotal)` which derives a quadratic range per level (`0..100` at level 1, `0..300` at level 2, …). After a promotion the bar would immediately look almost empty because the next threshold was far away.
- The bar now uses `row.xpSinceLevelUp` (resets to zero every time `categoryProgressDao.grantXpAndMaybeUnlock` promotes the category) divided by `CategoryGating.XP_MIN_PER_LEVEL` (50). The bar fills from `0 / 50` to `50 / 50` and resets to `0 / 50` on the next promotion — same rhythm as the gate message ("Need X more XP at this level") and matches the user's mental model.
- **No schema / DAO / mini-game changes** — only `ProgressViewModel.buildCategoryProgress` swaps two lines. The `xpIntoLevel` / `xpRequired` fields on `CategoryProgressUi` are kept (same names) so the UI continues to consume them unchanged.

### Phase 7.7 - Mini-game XP grants wired to per-category + per-skill

- **Per-category grants now fire for every mini-game** —
  `WordMatchVerbsViewModel`, `LetterSoupViewModel` and
  `ListeningViewModel` all accumulate XP keyed by
  `WordTypeFilter.name` (verbs regular / irregular, nouns, …)
  and grant each non-zero bucket to its dedicated row in
  `category_progress` at end-of-run. The previous Letter Soup
  behaviour (single `LETTER_SOUP` bucket only) and the previous
  Listening behaviour (XP accumulated but never granted) are both
  fixed.
- **Skill grants added to all three mini-games** — each correct
  answer credits the run's total XP to a row in `skill_progress`
  via the new `SkillProgressDao.grantXp(key, amount)` method.
  Current mapping: `WordMatchVerbs` → READING,
  `LetterSoup` → READING, `Listening` → LISTENING. Speaking and
  Grammar stay at `0 XP` until future mini-games exercise them.
- **Listening and Letter Soup keep their single-bucket gating**
  — the `CATEGORY_KEY="LISTENING"` and `CATEGORY_KEY="LETTER_SOUP"`
  rows still drive level unlock for those games (they have no
  per-word learned status, so the XP-only rule applies). Each
  correct answer credits BOTH the grammatical bucket and the
  single bucket.
- **No DAO / schema / UI changes** — only the three VMs were
  touched (one constructor param + one end-of-run call each, plus
  the per-category loop inlined from `WordMatchVerbsViewModel`'s
  existing pattern).

### Phase 7.6 - Skill Progress section (Listening / Speaking / Reading / Writing / Grammar)

- **New `skill_progress` table** — one row per language skill holding cumulative XP. Schema bump v9 → v10 via `MIGRATION_9_10`, which creates the table and seeds five rows (`LISTENING`, `SPEAKING`, `READING`, `WRITING`, `GRAMMAR`) so the UI never observes a missing key. Coexists with `category_progress` and `user_profile.totalXp` without touching either.
- **`Skill` enum + `SkillProgressDao`** — single source of truth for the five skills, each carrying a stable `key` (DB-persisted), a `@StringRes labelRes`, and a Material icon (`Headphones`, `Mic`, `MenuBook`, `Edit`, `Spellcheck`). The DAO exposes `observeAll()`, `get(key)`, `seedIfMissing(key)`, `seedAll(keys)` and `grantXp(key, amount)`.
- **New "Skills" section on the Progress screen** — rendered between the daily-goal card and "Progress by category" as a 3-column grid of `SkillCard`s (5 cards → 3 in the first row, 2 in the second). Each card shows the skill icon + name, the total XP as the headline number, an optional `Cycle N` chip once the user has completed at least one cycle, and a cyclic `LinearProgressIndicator` that fills to 1000 XP and resets to zero at each cycle boundary. There is no level cap and no gating — the bars are an "infinite" progress measure, like the user requested.
- **`SkillProgressUi` + `ProgressViewModel.skills`** — the ViewModel exposes a `StateFlow<List<SkillProgressUi>>` derived from `SkillProgressDao.observeAll()` and the canonical `Skill.ALL` order (Listening → Speaking → Reading → Writing → Grammar).
- **Mini-game XP grant to skills is NOT wired yet.** Each skill starts at `0 XP`. Mapping which mini-game credits which skill is deferred to a follow-up phase so this change stays scoped to the data + UI layer.

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