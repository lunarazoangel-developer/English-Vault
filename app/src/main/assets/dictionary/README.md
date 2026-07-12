# English Vault — Bundled Dictionary

This folder ships with the APK and is the source of truth for the **core
dictionary** that `DictionarySeeder` imports into the `core_words` Room
table on first launch (and on every `forceReseed()`).

JSON has no native comment syntax, so **this README plays the role of the
"section comments"** you'd normally sprinkle through the file. Keep it in
sync with the section order declared in `JsonLoader.SECTION_FILES`.

## Section order (also the load order)

The order below **defines the order entries appear in the Words screen**
because `JsonLoader` enumerates files in exactly this sequence and Room
keeps insert order. Reordering a section here means reordering it in the
loader constant — the two must stay aligned.

| #  | File                    | Entries | Type             | Notes                                                  |
|----|-------------------------|---------|------------------|--------------------------------------------------------|
| 1  | `verbs_irregular.json`  | 16      | verb             | V1–V16 alphabetised: be, buy, come, eat, give, …       |
| 2  | `verbs_regular.json`    | 17      | verb             | V17–V33 alphabetised: drive, learn, like, …            |
| 3  | `interjections.json`    | 67      | interjection     | Greetings, polite markers, affirmation / negation, surprise, joy, frustration, pain, attention getters, hesitation fillers. |
| 4  | `nouns.json`            | 69      | noun             | People / family, body parts, time, food, animals, home / furniture, places, common objects, abstract / communication, nature, education, work. |
| 5  | `adjectives.json`       | 147     | adjective        | Quality, size, emotion, personality, color, taste, weather, time / state, abstract and physical descriptors. |
| 6  | `adverbs.json`          | 165     | adverb           | Frequency, time, place, direction, manner, degree, certainty, affirmation, addition / linking. |
| 7  | `prepositions.json`     | 62      | preposition      | Place, time, direction, manner, possession, and common multi-word prepositions (`because of`, `in spite of`, `according to`, `due to`, `instead of`, `next to`, etc.). |
| 8  | `conjunctions.json`     | 62      | conjunction      | Coordinating, subordinating (time / condition / concession / cause / purpose / result / comparison / manner / place), correlative and conjunctive adverbs. |
| **Total**                   | **624** |                  |                                                        |

Verbs come first because they cover the most mini-game content today;
everything else is grouped by grammatical type, alphabetised inside the
group.

## File shape

Every `.json` is a flat JSON array; each element matches `WordDto`:

```json
{
  "word":            "string",
  "translation":     "string",
  "type":            "verb | noun | adjective | adverb | preposition | conjunction | interjection",
  "regular":         "true | false | null",
  "forms":           "{ base, thirdPerson, presentParticiple, pastSimple, pastParticiple } | null",
  "pronunciation":   "{ ipa: string, audio: string | null }",
  "category":        ["string"],
  "synonyms":        ["string"],
  "antonyms":        ["string"],
  "examples":        [ { "english": "string", "spanish": "string", "level": "A1|A2|B1|B2" } ],
  "tags":            ["string"],
  "difficulty":      "EASY | MEDIUM | HARD",
  "level":           1 | 2
}
```

### Field rules per type

| Type             | `regular`        | `forms`        | Notes                                                              |
|------------------|------------------|----------------|--------------------------------------------------------------------|
| verb             | **required**     | **required**   | `regular=true` ⇒ `pastSimple == pastParticiple == base + "ed"`.    |
| noun             | `null`           | `null`         |                                                                    |
| adjective        | `null`           | `null`         |                                                                    |
| adverb           | `null`           | `null`         |                                                                    |
| preposition      | `null`           | `null`         |                                                                    |
| conjunction      | `null`           | `null`         |                                                                    |
| interjection     | `null`           | `null`         | Multi-word heads are fine (`"thank you"`).                         |

### Conventions

- Alphabetical ordering inside each file (`word` ascending, case-insensitive).
- Exactly 3 examples per entry, mixing CEFR levels (typically A1 → A2 → B1+).
- `synonyms` and `antonyms` may be empty arrays when no good word exists.
- `pronunciation.audio` is always `null` until real audio assets are added.
- `category` carries 1+ semantic buckets; `tags` carries cross-cutting labels.

## Adding a new entry

1. Pick the matching file (or create a new one — see below).
2. Insert it **in alphabetical position** inside the array.
3. Match the field rules above (`regular`, `forms`, etc.).
4. Bump `DictionarySeeder.CORE_DICTIONARY_VERSION` so devices re-seed.
5. Done — users pick up the new word on next launch.

## Adding a new section

1. Create `app/src/main/assets/dictionary/<section>.json` with the same flat-array shape.
2. Append its name to `JsonLoader.SECTION_FILES` **at the position it should appear** in the load order.
3. Document it in the table above so this README stays the single source of truth.
4. Bump `DictionarySeeder.CORE_DICTIONARY_VERSION`.

The loader logs the file it processes; if a section is missing from the
constant, it's silently skipped, so always check the log after adding.

## Versioning

`DictionarySeeder.CORE_DICTIONARY_VERSION` is the contract that drives
re-seeding. Bump it whenever the bundled dictionary changes:

- **1** — implicit, 10 entries (never explicitly tracked).
- **2** — 68 entries with rich examples / categories / tags.
- **3** — same 68 entries, distributed across two `level` buckets
  (34 in level 1, 34 in level 2) for the mini-games progression.
- **4** — same 68 entries, split across these 8 section files plus this
  index. Per-entry content unchanged except `go` (4 → 3 examples for
  cross-entry consistency).
- **5, 6, 7** — schema / migration bumps; total stays at 68 entries.
- **8** — `conjunctions.json` extended from 2 to 62 entries
  (coordinating, subordinating, correlative, conjunctive adverbs).
  Total dictionary now **128 entries**.
- **9** — `interjections.json` extended from 7 to 67 entries
  (greetings, polite markers, affirmation / negation, surprise,
  joy, frustration, pain, attention getters, hesitation fillers).
  Total dictionary now **193 entries**.
- **10** — `nouns.json` extended from 9 to 69 entries (people /
  family, body parts, time, food, animals, home / furniture,
  places, common objects, abstract / communication, nature,
  education, work). Total dictionary now **262 entries**.
- **11** — `prepositions.json` extended from 2 to 62 entries
  (place, time, direction, manner, possession, and common
  multi-word prepositions). Total dictionary now
  **324 entries**.
- **12** — `adjectives.json` extended from 87 to 147 entries
  (colors, taste, more emotions, personality, weather,
  time / state, abstract qualities, and physical descriptors).
  Total dictionary now **459 entries**.
- **13** — `adverbs.json` extended from 105 to 165 entries
  (linking, frequency / period, direction, place, degree,
  certainty, and more manner descriptors). Total dictionary
  now **624 entries**.
