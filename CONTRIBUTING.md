# Contributing to VoidCraft

Thank you for helping improve VoidCraft. This project is a NeoForge mod for Minecraft `1.21.11`, so please keep gameplay changes small, testable, and consistent with the surrounding code style.

## Development Setup

Requirements:

- Java 21
- Git
- The Gradle wrapper included in this repository
- A Minecraft `1.21.11` / NeoForge `21.11.42` development environment

Useful commands:

```bash
./gradlew runClient
./gradlew build
find src/main/resources -name '*.json' -print0 | xargs -0 jq empty
git diff --check
```

If dependency caches are stale, run:

```bash
./gradlew --refresh-dependencies
```

## Pull Requests

Before opening a pull request:

- Keep the change focused on one bug fix, feature, or documentation update.
- Describe the player-visible behavior and the reason for the change.
- Update both English and Simplified Chinese language files when adding or changing user-facing text.
- Include screenshots or short clips for visual, GUI, shader, or rendering changes when possible.
- Run the relevant checks and mention any check you could not run.

## Code Style

- Follow the nearby Java style instead of introducing a new abstraction style.
- Prefer small, direct method names and clear control flow.
- Keep resource names, translation keys, and registry names stable unless the change explicitly requires a migration.
- Avoid broad refactors in feature or bug-fix pull requests.

## Issue Reports

Please use the bug report or feature request template. For crashes, include the full crash report or latest log, the Minecraft version, the NeoForge version, the VoidCraft version, and the list of other installed mods.
