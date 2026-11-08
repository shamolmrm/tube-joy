# Tube Joy: Water Sort Puzzle

Android puzzle game by **BahaLabs** — three sorting puzzles in one app: **Water Sort**, **Ball Sort** and **Nut Sort**, with unlimited levels and daily challenges. Free with ads.

Package: `com.bahalabs.tubejoy` · Version 1.0.0 · Category: Puzzle

## Tech stack
Kotlin · Jetpack Compose · Navigation Compose · Kotlin Coroutines · Gradle (Kotlin DSL, version catalog) · Google AdMob

## Project structure
| Module / folder | Purpose |
|---|---|
| `app/` | Android app — Compose UI, screens, ads |
| `engine/` | Pure-Kotlin puzzle engine (levels, moves, solving) with unit tests |
| `store/` | Play Store listing text, Play Console answers, Firebase Hosting site (`privacy.html`, `app-ads.txt`) |
| `docs/RELEASE.md` | Release steps |

## Build
1. Copy `admob.properties.example` → `admob.properties` and fill in your AdMob ids (the real file is gitignored).
2. Build:
```bash
./gradlew :engine:test          # engine tests
./gradlew :app:bundleRelease    # Play bundle (needs signing config)
```
