# English Vault

> Your personal English vocabulary trainer — Android · Kotlin · Jetpack Compose.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Phase](https://img.shields.io/badge/Status-Phase%204.5%20%F0%9F%93%9A-brightgreen)](#roadmap)

A Duolingo-inspired vocabulary app with streak tracking, XP/level progression, mini-games, self-tests and a personal word database you fully own. Built offline-first on top of Room and a versioned seed JSON asset.

---

## Table of Contents

- [✨ Features](#-features)
- [📸 Screenshots](#-screenshots)
- [🧱 Tech Stack](#-tech-stack)
- [🏗 Architecture](#-architecture)
- [📁 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
- [🧪 Testing](#-testing)
- [🗺 Roadmap](#-roadmap)
- [📝 Changelog](#-changelog)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)
- [🙏 Credits](#-credits)

---

## ✨ Features

### Available now

- 📚 **Offline dictionary** seeded from `assets/words.json` (**68 entries** covering regular/irregular verbs, nouns, adjectives, adverbs, prepositions, conjunctions and interjections, each with 2–4 bilingual examples tagged by CEFR level).
- 🗄 **Room storage v5** with three artefacts:
  - `core_words` — dictionary entries seeded from JSON. Conceptually read-only; the user can only update its user-state columns (`favorite`, `learned`, `notes`, …) via the dual-table DAO pattern.
  - `user_words` — entries the learner added through the form. Fully mutable.
  - `words_view` — `UNION ALL` of both tables, exposed as the read-only `WordEntity` data class so the UI consumes a single model regardless of origin.
  - `user_profile` — single-row table for XP, streak, daily goal, display name and the dictionary seed version.
- 🔌 **Five `TypeConverter`s** for nested objects (`Forms`, `Pronunciation`, `Example`) and string lists.
- 💉 **Hilt DI graph** wiring `AppDatabase`, both DAOs (`WordDao`, `UserProfileDao`), `JsonLoader`, `WordMapper`, the new `DictionarySeeder` and the seed routine.
- 🔁 **Four versioned migrations** that keep every previous install alive:
  - `MIGRATION_1_2` — adds the `user_profile` table.
  - `MIGRATION_2_3` — recreates `words` with `AUTOINCREMENT` ids.
  - `MIGRATION_3_4` — splits `words` into `core_words` + `user_words` + `words_view`.
  - `MIGRATION_4_5` — adds the `coreDictionaryVersion` column for versioned seeding.
- 🌱 **Versioned seeding** via `DictionarySeeder` (`@Singleton`): bumps `CORE_DICTIONARY_VERSION` in code trigger an automatic re-import of the bundled JSON on next launch, without losing user-added words or learning state.
- 🧠 **Pure XP/Level math** in `data.database.UserLevel` (quadratic curve) ready for the Progress screen to consume.
- 🧭 **Bottom navigation shell** with four tabs: Progress · Games · Test · Words.
- 🎨 **Duolingo-style blue palette** (`#1CB0F6` primary, `#4F46E5` secondary, `#FFC107` tertiary).
- 🧱 **Words screen wired to Room** via `WordListViewModel` (`@HiltViewModel`, `StateFlow`). Create + Delete persist.
- 🏷 **Read-only defaults** — `core_words` rows show a `📖 Dictionary` badge and have no edit/delete affordances; `user_words` rows show an `✏️ Mine` badge plus edit/delete icons. The `WordListViewModel` re-checks `isUserAdded()` before any delete so the invariant holds even if the UI regresses.
- 📋 **Rich expandable cards** — each card collapses by default to the header + badges and expands in place (with smooth animation and per-card state persisted via `rememberSaveable`) to reveal pronunciation, verb forms, examples with CEFR badges, synonyms, antonyms, tags and category.
- 🔢 **Live tab counts** — every tab in the Words screen shows the number of words matching its filter (`Regular (10)`, `Irregular (12)`, `Vocabulary (42)`, `Mine (N)`).

### Planned

- 🔁 **SRS-based review scheduling** powered by `lastReview` / `nextReview` fields (Phase 5).
- 🧮 **ProgressScreen wired to real data** — XP, level, streak and counters from `UserProfileDao` + `WordDao.observeProgressStats` (Phase 5).
- 🎮 **Mini-game implementations** (Word Match, Speed Quiz, Listening, …) (Phase 6).
- ⚙️ **User settings** — theme mode, daily goal editor, reminder time (Phase 5).
- ☁️ **Cloud sync / backup** (Phase 7).
- 🔎 **Search + filters** inside the Words screen (search bar, difficulty / category chips) (Phase 5).

---

## 📸 Screenshots

> Placeholder — UI mockups will be added once the app is run on a device.

| Progress | Games | Test | Words |
| :------: | :---: | :--: | :---: |
|   _TBD_  | _TBD_ | _TBD_ | _TBD_ |

---

## 🧱 Tech Stack

| Layer        | Technology                                          |
| ------------ | --------------------------------------------------- |
| Language     | Kotlin 2.2.0                                        |
| UI           | Jetpack Compose (Material 3)                        |
| Navigation   | `androidx.navigation:navigation-compose` 2.9.3      |
| DI           | Hilt 2.57.1                                         |
| Persistence  | Room 2.7.2 + `@DatabaseView` + Type Converters      |
| Async        | Kotlin Coroutines 1.10.2 + `Flow` / `StateFlow`     |
| JSON         | Gson 2.13.1                                         |
| Build        | Gradle 9.x · AGP 8.11.1 · KSP 2.2.0-2.0.2           |
| Min SDK      | 28 (Android 9)                                      |
| Target SDK   | 36 (Android 16)                                     |

---

## 🏗 Architecture

English Vault follows a clean **offline-first** pipeline plus an MVVM-flavoured UI layer wired through Hilt.

```
   ┌─────────────────────┐
   │  assets/words.json  │
   └──────────┬──────────┘
              │  Gson (UTF-8)
              ▼
   ┌─────────────────────┐
   │  data/json/dto      │   WordDto + FormsDto + PronunciationDto + ExampleDto
   │      WordDto        │
   └──────────┬──────────┘
              │  JsonLoader.loadWords()  (Dispatchers.IO)
              ▼
   ┌─────────────────────┐
   │  data/mapper        │   WordMapper.mapToCoreEntity(...)
   │     WordMapper      │
   └──────────┬──────────┘
              │  List<CoreWordEntity>
              ▼
   ┌──────────────────────────────────────────────────────┐
   │ data/database  (AppDatabase v5)                      │
   │   ├─ CoreWordEntity   → core_words  (AUTOINCREMENT)  │
   │   ├─ UserWordEntity   → user_words  (AUTOINCREMENT)  │
   │   ├─ WordEntity       → words_view  (@DatabaseView)  │
   │   ├─ UserProfileEntity → user_profile (+ version)    │
   │   ├─ WordDao          → writes target the right       │
   │   │                     table, reads hit the view     │
   │   ├─ UserProfileDao                                   │
   │   └─ 5 TypeConverters                                 │
   └──────────┬───────────────────────────────────────────┘
              │  Flow<List<WordEntity>> via words_view
              ▼
   ┌─────────────────────┐
   │  data/seed          │   DictionarySeeder.seedIfNeeded()
   │   DictionarySeeder  │   (re-imports JSON when bundled
   └──────────┬──────────┘    version > stored version)
              │
              ▼
   ┌─────────────────────┐
   │ WordListViewModel   │   @HiltViewModel — add / delete
   │  (StateFlow + ops)  │   gating (isUserAdded)
   └──────────┬──────────┘
              │  collectAsState
              ▼
   ┌─────────────────────┐
   │       ui/words      │   WordListScreen + WordFormScreen
   │   Compose UI        │   + components/WordCard (expand /
   └─────────────────────┘   collapse, badges, all sections)
```

Key principles:

- **DTO ↔ Entity separation**: `WordDto` mirrors JSON; `CoreWordEntity` / `UserWordEntity` are the Room aggregates; `WordEntity` is the read-only view that unifies them. The mapper keeps DTO ↔ entity in sync.
- **Two tables, one view**: writes explicitly target `core_words` or `user_words`; reads always target `words_view`. The view's `source` literal (`'core'` / `'user'`) is what `isUserAdded()` checks, so the rest of the app stays source-agnostic.
- **Independent id sequences**: each table has its own `AUTOINCREMENT`. The JSON id is never carried into Room, so future JSON updates can never collide with user-added ids.
- **Dual-table update pattern**: state-mutating DAO methods (`setLearned`, `setFavorite`, …) cannot be expressed as a single `UPDATE words` because there is no unified table. They are implemented as default methods that fan out into two `@Query` calls — one per underlying table. Because the two sequences are independent, only the table that actually holds the id touches a row; the other is a no-op.
- **Reactive data flow**: DAO queries expose `Flow<…>`; ViewModels turn them into `StateFlow`; Compose collects with `collectAsState`.
- **Single source of truth for state**: the Room database. Compose observes; mutations go through the ViewModel.
- **Domain rules live in the VM**: `WordListViewModel.deleteWord` re-checks `isUserAdded()` so the read-only defaults invariant holds even if a stale dialog slips through.
- **Hilt singletons**: `AppDatabase`, `JsonLoader`, `WordMapper`, `DictionarySeeder` are application-scoped.
- **Type safety**: enums (`Difficulty`) replace free-form strings as soon as data crosses the JSON boundary.
- **Versioned migrations**: every schema bump registers an explicit `Migration` rather than wiping user data.
- **Versioned seeding**: the bundled `CORE_DICTIONARY_VERSION` constant is compared to `UserProfileEntity.coreDictionaryVersion` on every launch; when the bundled version is newer, `DictionarySeeder` wipes `core_words` and re-imports the JSON, leaving `user_words` and the learning state untouched.

---

## 📁 Project Structure

```
EnglishVault/
├── README.md                          ← you are here
├── build.gradle.kts                   ← top-level Gradle config
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml             ← version catalog
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/
        │   └── words.json             ← bundled dictionary (68 entries)
        ├── res/
        │   ├── values/
        │   │   ├── strings.xml        ← UI strings
        │   │   ├── colors.xml
        │   │   └── themes.xml
        │   └── mipmap-*/              ← launcher icons
        └── java/
            ├── com/example/englishvault/
            │   ├── EnglishVaultApp.kt ← @HiltAndroidApp
            │   ├── MainActivity.kt    ← single-activity host + bootstrap
            │   └── ui/
            │       ├── app/MainScaffold.kt          ← bottom nav + NavHost
            │       ├── components/                  ← AppBottomBar, PrimaryButton, SectionHeader
            │       ├── games/GamesScreen.kt
            │       ├── navigation/                  ← Destination + BottomNavItem
            │       ├── progress/ProgressScreen.kt   (still mocked; data ready in v5)
            │       ├── test/TestScreen.kt
            │       ├── theme/                       ← Duolingo-blue palette
            │       └── words/
            │           ├── WordListScreen.kt        ← tabs + counts + expandable cards
            │           ├── WordFormScreen.kt        ← onSave emits WordEntity
            │           ├── components/WordCard.kt   ← rich, expandable
            │           └── viewmodel/WordListViewModel.kt   ← @HiltViewModel
            └── data/
                ├── database/
                │   ├── AppDatabase.kt               (v5)
                │   ├── Migrations.kt                ← 4 migrations (1→2, 2→3, 3→4, 4→5)
                │   ├── UserLevel.kt                 ← XP/Level pure math
                │   ├── dao/
                │   │   ├── WordDao.kt                ← dual-table updates
                │   │   └── UserProfileDao.kt
                │   ├── entities/
                │   │   ├── CoreWordEntity.kt         ← @Entity(core_words)
                │   │   ├── UserWordEntity.kt         ← @Entity(user_words)
                │   │   ├── WordEntity.kt             ← @DatabaseView(words_view)
                │   │   ├── WordMappers.kt            ← toUserEntity() / toCoreEntity()
                │   │   ├── UserProfileEntity.kt
                │   │   └── ProgressStats.kt
                │   └── converters/                  ← 5 type converters
                ├── json/
                │   ├── dto/WordDto.kt
                │   └── loader/JsonLoader.kt
                ├── mapper/WordMapper.kt
                └── seed/DictionarySeeder.kt          ← versioned re-seed
            └── di/DatabaseModule.kt                  ← Hilt graph + migrations
```

---

## 🚀 Getting Started

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

1. `File → Open…` and select the `EnglishVault` directory.
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

## 🧪 Testing

```bash
# JVM unit tests
./gradlew :app:testDebugUnitTest

# Instrumented tests (requires a connected device or emulator)
./gradlew :app:connectedDebugAndroidTest
```

> The repository still ships with the default `ExampleUnitTest` and
> `ExampleInstrumentedTest` scaffolds. Real coverage for the data
> layer (`WordDao` dual-table pattern, `WordMapper`, `JsonLoader`,
> `UserProfileDao`), the seed flow (`DictionarySeeder` + the four
> migrations) and the Words screen flow (`WordListViewModel` +
> tab filters) will land in a dedicated testing phase.

---

## 🗺 Roadmap

| Phase  | Status     | Scope                                                                          |
| :----: | :--------: | ------------------------------------------------------------------------------ |
| 1      | ✅ Done    | Data layer: DTO, mapper, entity, DAO, Room, one-time seed from JSON            |
| 2      | ✅ Done    | UI shell + bottom navigation + visual mockups + Duolingo-blue theme            |
| 2.5    | ✅ Done    | Real CRUD for Words screen (Room-backed) · user_profile table · Migrations      |
| 3      | ✅ Done    | Split `words` into `core_words` + `user_words` + `words_view` · `@DatabaseView` |
| 4      | ✅ Done    | Rich expandable `WordCard` · Dictionary / Mine badges · tab counts · richer JSON |
| 4.5    | ✅ Done    | Versioned seed (`DictionarySeeder`, `CORE_DICTIONARY_VERSION`, `MIGRATION_4_5`) |
| 5      | 📋 Planned | Wire ProgressScreen · SRS review scheduling · settings UI · search + filters   |
| 6      | 📋 Planned | Mini-game implementations (Word Match, Speed Quiz, Listening, …)                |
| 7      | 📋 Planned | Cloud sync, user accounts, multi-device                                        |

---

## 📝 Changelog

### Phase 4.5 — Versioned seed (current)

- ➕ `data/seed/DictionarySeeder.kt` — `@Singleton` that compares `CORE_DICTIONARY_VERSION = 2` against `UserProfileEntity.coreDictionaryVersion` and re-imports the JSON when the bundled version is newer. Exposes `seedIfNeeded()` for the bootstrap and `forceReseed()` for future debug flows.
- ➕ `UserProfileEntity.coreDictionaryVersion: Int = 0` field.
- ➕ `MIGRATION_4_5` — `ALTER TABLE user_profile ADD COLUMN coreDictionaryVersion INTEGER NOT NULL DEFAULT 0`. Existing installs get `0` so the next launch automatically re-seeds the upgraded dictionary.
- ➕ `AppDatabase` bumped to `version = 5`.
- 🔁 `MainActivity` now delegates the seed routine to `DictionarySeeder.seedIfNeeded()` instead of running it inline.
- 🔁 `WordDao.insertCoreWords` / `insertUserWord` / `deleteUserWord` / `deleteAllCoreWords` are the public write API; the legacy `insertWord` / `insertWords` / `deleteWord` methods are gone.

### Phase 4 — Rich Words screen

- ➕ `assets/words.json` grew from 10 → 68 entries, with bilingual examples tagged by CEFR level (`A1` / `A2` / `B1` / `B2`), richer categories, synonyms, antonyms and tags.
- ➕ `WordDto` no longer carries the `source` field (every JSON entry is by definition core; the discriminator now lives in which table the row lives in).
- ➕ `WordCard` redesigned: chevron + avatar + word + translation + badges + chips, with `AnimatedVisibility` that reveals pronunciation, verb forms, examples (with `LevelBadge`), synonyms, antonyms, tags and category on tap. Per-card expansion state survives rotation via `rememberSaveable` + custom `ExpansionSaver`.
- ➕ `📖 Dictionary` badge (with `MenuBook` icon) for `core_words`; existing `✏️ Mine` badge (`Person` icon) for `user_words`.
- ➕ Live `(n)` counts on every Words tab, computed in a single pass over `words`.
- ➕ New strings: `words_badge_dictionary`, `words_section_*`, `words_forms_*`, `words_card_expand`, `words_card_collapse`, plus labels for verb forms.

### Phase 3 — Table split (two tables + view)

- 🗄 `words` split into `core_words` (seeded) + `user_words` (user-added) + `words_view` (`UNION ALL`).
- ➕ `CoreWordEntity` and `UserWordEntity` entities with identical schemas; `WordEntity` becomes a `@DatabaseView` over `words_view` with `source` synthesised as a literal.
- ➕ `WordMappers.kt` — `toUserEntity()` / `toCoreEntity()` extensions that convert view rows to writer entities (dropping `source`, resetting `id` to `0` so AUTOINCREMENT owns the primary key).
- ➕ `MIGRATION_3_4` — idempotent split: creates both tables, copies existing rows by `source`, drops the legacy `words`, recreates `words_view`.
- ➕ `WORD_DAO` rewritten with a dual-table update pattern (`setLearned`, `setFavorite`, `recordReview`, `setNextReview`, `setNotes`, `setCustomDifficulty` each fan out to two `@Query` calls).
- ➕ `WordDao.deleteUserWord` / `deleteAllCoreWords` — explicit, intentional table targets.
- ➕ `WordMapper.mapToCoreEntity` always sets `id = 0` so the JSON never controls the primary key (closes the id-collision footgun).
- 🔁 `AppDatabase` bumped to `version = 4`.

### Phase 2.5 — Words wired to Room

- ➕ `WordListViewModel` (`@HiltViewModel`) exposing `StateFlow<List<WordEntity>>` plus `deleteWord` / `addUserWord` operations.
- ➕ `WordEntity.id` is now `autoGenerate = true`; user-added words get fresh ids without colliding with the JSON seed.
- ➕ `WordDao.deleteWord(id)` — single-row removal that persists.
- ➕ `MIGRATION_2_3` — recreates the `words` table with `AUTOINCREMENT` and registers the migration in `DatabaseModule`.
- ➕ `user_profile` table (`UserProfileEntity` + `UserProfileDao`) — XP, streak, daily goal, display name.
- ➕ `MIGRATION_1_2` — brings older installs into v2 so the profile table exists.
- ➕ `UserLevel` — pure functions for XP ↔ level conversion used by the upcoming Progress screen.
- ➕ `WordCard` gained a `WordEntity` overload so the screen hands the entity straight through.
- ➕ `WordFormScreen.onSave` now emits a fully built `WordEntity`; the parent ViewModel persists it via `insertWord`.
- ➕ Read-only defaults enforced both in the UI (edit/delete icons hidden) and in the VM (`deleteWord` rejects non-user rows).
- ➕ Renamed "Singulars" tab to **Vocabulary** — clearer label for non-verb words.

### Phase 2 — UI shell & visual mockups

- ➕ Bottom navigation shell with four tabs (Progress · Games · Test · Words).
- ➕ Visual mockups for Progress (streak, XP, daily goal, learning path), Games (2×3 grid), Test (selectable difficulty), Words (in-memory CRUD with confirmation dialog).
- ➕ Duolingo-inspired blue Material 3 palette.
- ➕ Comprehensive English comments across `ui/`, `data/`, `di/`.
- ➕ README and project documentation.

### Phase 1 — Data layer

- ➕ Room database with a single `words` table.
- ➕ Gson-based `JsonLoader` for `assets/words.json`.
- ➕ `WordMapper` translating DTOs → entities with safe defaults.
- ➕ Five `TypeConverter`s for nested objects and string lists.
- ➕ One-time seed on first launch (`WordDao.countWords() == 0`).
- ➕ Hilt graph via `DatabaseModule`.

---

## 🤝 Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feat/my-feature`).
3. Commit your changes (`git commit -m "feat: add my feature"`).
4. Push the branch (`git push origin feat/my-feature`).
5. Open a Pull Request describing the motivation and approach.

Please follow the existing code style (Kotlin official, 4-space indent, KDoc on public APIs) and keep the data layer free of UI dependencies.

When you bump the bundled dictionary, edit only `assets/words.json` and the `CORE_DICTIONARY_VERSION` constant in `DictionarySeeder` — the seeder will re-import automatically on next launch.

---

## 📄 License

Released under the **MIT License**. See the [LICENSE](LICENSE) file for the full text.

---

## 🙏 Credits

English Vault is built on top of these outstanding open-source projects:

- [Kotlin](https://kotlinlang.org) — JetBrains.
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Google.
- [Material 3](https://m3.material.io) — Google.
- [AndroidX Navigation Compose](https://developer.android.com/jetpack/compose/navigation) — Google.
- [Room](https://developer.android.com/training/data-storage/room) — Google.
- [Hilt](https://dagger.dev/hilt/) — Google.
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) — JetBrains.
- [Gson](https://github.com/google/gson) — Google.
- [KSP](https://github.com/google/ksp) — Google.

> _Duolingo™ is a trademark of Duolingo Inc. This project is an
> unaffiliated fan implementation and is not endorsed by Duolingo._