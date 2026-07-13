#!/usr/bin/env python3
"""
Redistribute the `level` field across every entry in the bundled
core dictionary so that every part of speech reaches level 10.

Algorithm (per file):
  1. Load the JSON array (preserves key order and Unicode).
  2. Sort entries by (len(word), word.upper()) so the shortest
     words land in the first buckets and longer ones climb the
     scale; alphabetical order breaks ties deterministically.
  3. Split the sorted list into 10 buckets as evenly as possible
     (bucket_size = ceil(n / 10), capped at 10).
  4. Rewrite the top-level `level` field on every entry. Every
     other field — translation, examples, pronunciation, etc. —
     stays untouched.
  5. Write the file back with the same 4-space indentation and
     UTF-8 encoding the source uses.

Run from the project root:

    python tools/redistribute_levels.py
"""

from __future__ import annotations

import json
import math
import os
import sys
from typing import Iterable

PROJECT_ROOT = os.path.abspath(
    os.path.join(os.path.dirname(__file__), os.pardir)
)
DICT_DIR = os.path.join(
    PROJECT_ROOT,
    "app",
    "src",
    "main",
    "assets",
    "dictionary",
)

# Section files in the same order `JsonLoader.SECTION_FILES` expects.
SECTION_FILES = (
    "verbs_irregular.json",
    "verbs_regular.json",
    "interjections.json",
    "nouns.json",
    "adjectives.json",
    "adverbs.json",
    "prepositions.json",
    "conjunctions.json",
)


def redistribute(entries: list[dict], max_level: int = 10) -> list[dict]:
    """Return a copy of `entries` with the `level` field rewritten
    so the words are split into `max_level` buckets ordered by
    (length, alphabetical).

    Uses proportional rounding — `level = ceil((i + 1) * max_level /
    n)` — so every level is guaranteed to receive at least
    `floor(n / max_level)` words and the leftover entries spill into
    the higher levels. This keeps the ordering monotonic (shorter
    words in lower levels) while ensuring no level is empty even for
    the smallest sections (62-word prepositions / conjunctions).
    """
    if not entries:
        return entries

    # Sort by length ascending, then by uppercase word for a stable
    # tiebreaker that is language-neutral.
    sorted_entries = sorted(
        entries,
        key=lambda entry: (len(entry["word"]), entry["word"].upper()),
    )

    n = len(sorted_entries)
    for index, entry in enumerate(sorted_entries):
        # Map the sorted position to a level proportionally. Capped
        # to `max_level` so the very last word in any file always
        # lands in the top bucket (no spillover).
        level = max(1, min(max_level, math.ceil((index + 1) * max_level / n)))
        entry["level"] = level

    return sorted_entries


def load(path: str) -> list[dict]:
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def dump(path: str, entries: Iterable[dict]) -> None:
    # `ensure_ascii=False` keeps Spanish accents and IPA symbols
    # readable instead of `\u00e9`-style escape sequences.
    payload = json.dumps(
        list(entries),
        ensure_ascii=False,
        indent=4,
        separators=(",", ": "),
    )
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(payload)
        fh.write("\n")


def summarize(name: str, entries: list[dict]) -> dict[int, int]:
    counts: dict[int, int] = {}
    for entry in entries:
        level = entry.get("level", 0)
        counts[level] = counts.get(level, 0) + 1
    return counts


def main() -> int:
    if not os.path.isdir(DICT_DIR):
        print(f"Dictionary directory not found: {DICT_DIR}", file=sys.stderr)
        return 1

    print(f"Redistributing levels across {len(SECTION_FILES)} files")
    print(f"Source: {DICT_DIR}")
    print("-" * 64)

    grand_total = 0
    for fname in SECTION_FILES:
        path = os.path.join(DICT_DIR, fname)
        if not os.path.isfile(path):
            print(f"  {fname}: SKIPPED (missing)")
            continue

        entries = load(path)
        before_total = len(entries)
        redistributed = redistribute(entries)
        dump(path, redistributed)
        after = summarize(fname, redistributed)

        levels = sorted(after.items())
        spread = ", ".join(f"L{l}={c}" for l, c in levels)
        print(f"  {fname}: {before_total} words -> 10 levels ({spread})")
        grand_total += before_total

    print("-" * 64)
    print(f"Total redistributed: {grand_total} words")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())