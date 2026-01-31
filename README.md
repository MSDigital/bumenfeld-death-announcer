# Bumenfeld Death Announcer

[![Gradle CI](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/gradle.yml/badge.svg)](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/gradle.yml)
[![Release](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/release.yml/badge.svg)](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/release.yml)

Bumenfeld Death Announcer reacts to every player death, matches the damage source to a cause category, and broadcasts a localized title/subtitle plus a bold red `[DEATH]` chat line so your server can celebrate (or mourn) every dramatic exit.

## 1. Description & commands

### Core features
- **Localized event titles** – Death causes map to categories (`fire`, `fall`, `projectile`, etc.) and pull jokes/titles from the configured locale.
- **Styled chat fallback** – Every death also publishes the subtitle in chat with a bold red `[DEATH]` prefix so the message stands out.
- **Icon support** – Notifications include Hytale `ItemStack` icons (the same assets the server already ships) to visually signal what killed the player.
- **Config toggles** – `config.yml` exposes `language`, `notifications`, and `chat-notifications` so you can disable the HUD alert or chat message independently.
- **Admin tooling** – `/deathnotification` is the command hub:
  - `/deathnotification config <option> <value>` updates `language`, `notifications`, or `chat-notifications`, saves the change to `config.yml`, and reloads the plugin.
  - `/deathnotification reload` re-reads configuration/localization without restarting the server.
  - `/deathnotification test` simulates every death cause (2 seconds apart) so you can preview titles, icons, and chat output.

## 2. Installation & configuration

### Server setup
1. Build the plugin with **Java 25 (Temurin 25)**:
   ```bash
   ./gradlew clean release
   ```
   The release artifact is `build/libs/bumenfeld-death-announcer-<version>.jar`.
2. Drop the JAR into `/mods/` of your Hytale server.
3. Start the server once so the plugin exports `mods/com.Bumenfeld_DeathAnnouncer/config.yml` and the localization folder.
4. Adjust `config.yml` as needed and run `/deathnotification reload` or restart the server to apply the changes.

### Configuration keys (`config.yml`)
```yaml
language: en              # built-in locales: en/de, add new {code}.json under localization/
notifications: true       # enable/disable HUD event title notifications
chat-notifications: true  # enable/disable the red [DEATH] chat message
```
Settings modified through `/deathnotification config …` are persisted automatically.

## 3. Localization & assets
- Files live under `src/main/resources/localization/` and are copied to `mods/.../localization/` on first run, so servers can override them.
- Each JSON file contains a `titles` pool plus per-cause arrays (`fire`, `lava`, `melee`, etc.). Add custom jokes by creating new locale files with the same structure and pointing `language` to them.
- Icon identifiers reference Hytale’s `ui/icons` assets via `ItemStack`, so feel free to expand `DeathAnnouncementSystem.ICONS` if you want custom visuals for new causes.

## 4. Build & release
- `appJar` packages the plugin and, when `fatJar=true`, bundles runtime dependencies (toggle with `-PfatJar=false` for a thin jar).
- `./gradlew build` still compiles the code before producing the artifact; `./gradlew release` hooks into the release workflow that uploads artifacts to GitHub Releases.
- Add `-PdeployOutputPath=/absolute/path/to/server/mods` to automatically copy the freshly built `/build/libs/*.jar` artifact to another location (the copy happens after `build`/`assemble`/`release` and skips when the property is absent).
- Version metadata (ID/timestamp/commit) is injected during `processResources`, so release builds contain provenance.

## 5. Development notes
1. The plugin auto-creates `config.yml` and localization overrides inside the data directory, making it easy to tweak strings without rebuilding.
2. `/deathnotification test` is useful for QA—watch the HUD and chat output cycle through every cause every 2 seconds.
3. Keep translation files synchronized and update `config.yml` defaults when adding new options so server operators have working templates.
