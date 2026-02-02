# Bumenfeld Death Announcer

[![Gradle CI](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/gradle.yml/badge.svg)](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/gradle.yml)
[![Release](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/release.yml/badge.svg)](https://github.com/msdigital/bumenfeld-death-announcer/actions/workflows/release.yml)

Bumenfeld Death Announcer reacts to every player death, matches the damage source to a cause category, and broadcasts a localized title/subtitle plus a bold red `[DEATH]` chat line so your server can celebrate (or mourn) every dramatic exit.

## 1. Description & commands

### Core features
- **Localized event titles** - Death causes map to categories (`fire`, `fall`, `projectile`, etc.) and pull jokes/titles from the configured locale.
- **Styled chat fallback** - Every death also publishes the subtitle in chat with a bold red `[DEATH]` prefix so the message stands out.
- **Custom icons** - HUD and notifications can use PNG icons shipped in `Common/UI/Custom/icons`.
- **Config toggles** - `config.yml` exposes `language`, `notifications`, `chat-notifications`, and `hud-notifications` so you can disable each channel independently.
- **Admin tooling** - `/deathnotification` is the command hub:
  - `/deathnotification config <option> <value>` updates `language`, `notifications`, `chat-notifications`, `hud-notifications`, or `hud-display-seconds`, saves the change to `config.yml`, and reloads the plugin.
  - `/deathnotification reload` re-reads configuration/localization without restarting the server.
  - `/deathnotification test` simulates every death cause (2 seconds apart) so you can preview titles, icons, and chat output.

## 2. Installation & configuration

### Dependencies
- Install the MultipleHUD dependency (`MultipleHUD-1.0.4.jar`) in `/mods/` so HUDs can be layered safely.

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
hud-notifications: true   # enable/disable the custom HUD panel
hud-display-seconds: 4    # how long the HUD stays visible
```
Settings modified through `/deathnotification config ...` are persisted automatically.

## 3. Localization & assets
- Files live under `src/main/resources/localization/` and are copied to `mods/.../localization/` on first run, so servers can override them.
- Each JSON file contains a `titles` pool plus per-cause arrays (`fire`, `lava`, `melee`, etc.). Add custom jokes by creating new locale files with the same structure and pointing `language` to them.
- Localization files are refreshed automatically when the plugin build version changes, so bundled updates propagate to servers.
- HUD/notification icons live at `src/main/resources/Common/UI/Custom/icons` and are referenced by filename in code.

## 4. Build & release
- Build the production jar with `./gradlew clean release`.
- The output artifact is `build/libs/bumenfeld-death-announcer-<version>.jar`.
- Version metadata (ID/timestamp/commit) is injected during `processResources`, so release builds contain provenance.

## 5. Development notes
1. The plugin auto-creates `config.yml` and localization overrides inside the data directory, making it easy to tweak strings without rebuilding.
2. `/deathnotification test` is useful for QA - watch the HUD and chat output cycle through every cause every 2 seconds.
3. Keep translation files synchronized and update `config.yml` defaults when adding new options so server operators have working templates.

## 6. License
Licensed under **MSDigital No-Resale License v1.0** (see `LICENSE`).
**Attribution:** BlackJackV8 (MSDigital) — Official repo: <https://github.com/MSDigital/bumenfeld-death-announcer>