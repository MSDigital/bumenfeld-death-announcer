# Bumenfeld Death Announcer

Bumenfeld Death Announcer listens for every player death, guesses the damage category, and broadcasts a funny localized event title/subtitle so the server knows what just happened.

## Getting started

1. Run `./gradlew clean release` (or `./gradlew.bat clean release` on Windows) to build `build/libs/bumenfeld-death-announcer-<version>.jar`.
2. Drop the jar into `mods/com.bumenfeld.deathannouncer/`, and copy `manifest.json`, `config.yml`, and the localization bundle there (`mods/.../manifest.json`, `mods/.../config.yml`, `mods/.../localization/en.json`, `mods/.../localization/de.json`).
3. Pick a language in `config/config.yml` (`language: en` or `language: de`) or drop in additional localization files to support other languages.
4. Start the server; the plugin registers its death system automatically and begins announcing deaths with Hytale’s event titles.

## Configuration & localization

- `config/config.yml` exposes only `language`.
- Localization files live under `src/main/resources/localization`. Override them inside `mods/.../localization/` if you need to tweak the jokes without recompiling.

## Build & release

- The fat jar is handled by `appJar` and is named `build/libs/bumenfeld-death-announcer-<version>.jar`.
- `./gradlew build` still runs `appJar` to ensure the code compiles before producing artifacts.
- Toggle fat-jar bundling via `-PfatJar=false` to produce a thin jar.

## Next steps

1. Wire the config/localization selection into the death announcement logic so admins can add their own punchlines at runtime.
2. Add more language files or fetch translations from a remote source.
