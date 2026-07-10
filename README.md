# English Vault

> Your personal English vocabulary trainer — Android · Kotlin · Jetpack Compose.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Phase](https://img.shields.io/badge/Status-Phase%202.5%20%F0%9F%9A%80-yellow)](#roadmap)

A Duolingo-inspired vocabulary app with streak tracking, XP/level progression, mini-games, self-tests and a personal word database you fully own. Built offline-first on top of Room and a single seed JSON asset.

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

- 📚 **Offline dictionary** seeded from `assets/words.json` (10 entries covering regular verbs, irregular verbs, interjections and nouns) on the first launch only.
- 🗄 **Room storage v3** with two tables:
  - `words` — dictionary entries + user-owned learning state (favorite, learned, notes, reviewCount, lastReview, nextReview, customDifficulty), with AUTOINCREMENT primary keys.
  - `user_profile` — single-row table for XP, streak, daily goal and display name.
- 🔌 **Five `TypeConverter`s** for nested objects (`Forms`, `Pronunciation`, `Example`) and string lists.
- 💉 **Hilt DI graph** that wires `AppDatabase`, both DAOs (`WordDao`, `UserProfileDao`), `JsonLoader`, `WordMapper` and the seed routine.
- 🔁 **Versioned migrations** (`MIGRATION_1_2` introduces the user profile, `MIGRATION_2_3` recreates `words` with AUTOINCREMENT). Upgrades keep the app working without destructive resets.
- 🧠 **Pure XP/Level math** in `data.database.UserLevel` (quadratic curve) ready for the Progress screen to consume.
- 🧭 **Bottom navigation shell** with four tabs: Progress · Games · Test · Words.
- 🎨 **Duolingo-style blue palette** (`#1CB0F6` primary, `#4F46E5` secondary, `#FFC107` tertiary).
- 🧱 **Words screen wired to Room** via `WordListViewModel` (`@HiltViewModel`, `StateFlow`). Create + Delete persist; Edit is still visual.
- 🏷 **Read-only defaults** — seeded words cannot be edited or deleted; user-added words show a "Mine" badge and the destructive actions.

### Planned

- 🔁 **SRS-based review scheduling** powered by `lastReview` / `nextReview` fields (Phase 3).
- 🧮 **ProgressScreen wired to real data** — XP, level, streak and counters from `UserProfileDao` + `WordDao.observeProgressStats` (Phase 3).
- 🎮 **Mini-game implementations** (Word Match, Speed Quiz, Listening, …) (Phase 4).
- ⚙️ **User settings** — theme mode, daily goal editor, reminder time (Phase 3).
- ☁️ **Cloud sync / backup** (Phase 5).

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
| Persistence  | Room 2.7.2 + Type Converters                        |
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
   │  data/mapper        │   WordMapper
   │     WordMapper      │
   └──────────┬──────────┘
              │  mapToEntity / mapToEntityList
              ▼
   ┌──────────────────────────────────────┐
   │ data/database                        │
   │   AppDatabase (v3)                   │
   │   ├─ WordEntity  (AUTOINCREMENT id)  │
   │   ├─ WordDao     (Flow + update)     │
   │   ├─ UserProfileEntity  (XP/streak)  │
   │   ├─ UserProfileDao                  │
   │   └─ 5 TypeConverters                │
   └──────────┬───────────────────────────┘
              │  Flow<List<WordEntity>>
              ▼
   ┌─────────────────────┐
   │ WordListViewModel   │   @HiltViewModel — delete/add gating
   │  (StateFlow + ops)  │
   └──────────┬──────────┘
              │  collectAsState
              ▼
   ┌─────────────────────┐
   │       ui/words      │   WordListScreen + WordFormScreen + WordCard
   │   Compose UI        │
   └─────────────────────┘
```

Key principles:

- **DTO ↔ Entity separation**: `WordDto` mirrors JSON; `WordEntity` is the Room aggregate. The mapper keeps them in sync.
- **Reactive data flow**: DAO queries expose `Flow<…>`; ViewModels turn them into `StateFlow`; Compose collects with `collectAsState`.
- **Single source of truth for state**: the Room database. Compose observes; mutations go through the ViewModel.
- **Domain rules live in the VM**: `WordListViewModel.deleteWord` re-checks `isUserAdded()` so the read-only defaults invariant holds even if a stale dialog slips through.
- **Hilt singletons**: `AppDatabase`, `JsonLoader`, `WordMapper` are application-scoped.
- **Type safety**: enums (`Difficulty`) replace free-form strings as soon as data crosses the JSON boundary.
- **Versioned migrations**: every schema bump registers an explicit `Migration` rather than wiping user data.

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
        │   └── words.json             ← bundled dictionary (10 seed entries)
        ├── res/
        │   ├── values/
        │   │   ├── strings.xml        ← UI strings
        │   │   ├── colors.xml
        │   │   └── themes.xml
        │   └── mipmap-*/              ← launcher icons
        └── java/
            ├── com/example/englishvault/
            │   ├── EnglishVaultApp.kt ← @HiltAndroidApp
            │   ├── MainActivity.kt    ← single-activity host + one-time seed
            │   └── ui/
            │       ├── app/MainScaffold.kt          ← bottom nav + NavHost
            │       ├── components/                  ← AppBottomBar, PrimaryButton, SectionHeader
            │       ├── games/GamesScreen.kt
            │       ├── navigation/                  ← Destination + BottomNavItem
            │       ├── progress/ProgressScreen.kt   (still mocked; data ready in v3)
            │       ├── test/TestScreen.kt
            │       ├── theme/                       ← Duolingo-blue palette
            │       └── words/
            │           ├── WordListScreen.kt        ← wired to WordListViewModel
            │           ├── WordFormScreen.kt        ← onSave emits WordEntity
            │           ├── components/WordCard.kt
            │           └── viewmodel/WordListViewModel.kt   ← @HiltViewModel
            ├── data/
            │   ├── database/
            │   │   ├── AppDatabase.kt               (v3)
            │   │   ├── Migrations.kt                ← MIGRATION_1_2 + MIGRATION_2_3
            │   │   ├── UserLevel.kt                 ← XP/Level pure math
            │   │   ├── dao/
            │   │   │   ├── WordDao.kt                ← update + filtered + aggregate
            │   │   │   └── UserProfileDao.kt
            │   │   ├── entities/
            │   │   │   ├── WordEntity.kt
            │   │   │   ├── UserProfileEntity.kt
            │   │   │   └── ProgressStats.kt
            │   │   └── converters/                  ← 5 type converters
            │   ├── json/
            │   │   ├── dto/WordDto.kt
            │   │   └── loader/JsonLoader.kt
            │   └── mapper/WordMapper.kt
            └── di/DatabaseModule.kt    ← Hilt graph + migrations
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
> layer (WordDao, WordMapper, JsonLoader, UserProfileDao) and for the
> Words screen flow (WordListViewModel + filters) will land in a
> dedicated testing phase.

---

## 🗺 Roadmap

| Phase  | Status     | Scope                                                                          |
| :----: | :--------: | ------------------------------------------------------------------------------ |
| 1      | ✅ Done    | Data layer: DTO, mapper, entity, DAO, Room, one-time seed from JSON            |
| 2      | ✅ Done    | UI shell + bottom navigation + visual mockups + Duolingo-blue theme            |
| 2.5    | ✅ Done    | Real CRUD for Words screen (Room-backed) · user_profile table · Migrations      |
| 3      | 📋 Planned | Wire ProgressScreen to real data · XP rewards · SRS scheduling · settings UI    |
| 4      | 📋 Planned | Mini-game implementations (Word Match, Speed Quiz, Listening, …)                |
| 5      | 📋 Planned | Cloud sync, user accounts, multi-device                                        |

---

## 📝 Changelog

### Phase 2.5 — Words wired to Room (current)

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