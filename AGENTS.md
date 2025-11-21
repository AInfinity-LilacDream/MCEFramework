# Repository Guidelines

## Project Structure & Module Organization
Source lives under `src/main/java/mcevent/MCEFramework`, with `commands` for Paper executors, `games/*` per minigame, `generalGameObject` for shared abstractions (such as `MCEGame`, `MCESpecialItem`), and `tools` for utilities. Resources in `src/main/resources` hold `plugin.yml` plus default configs inside `MCEConfig/`. Gradle builds emit jars to `build/libs/`, and the dev Paper server keeps state in `run/`.

## Build, Test, and Development Commands
Use the wrapper (`./gradlew` on Unix, `.\gradlew.bat` on Windows).
- `./gradlew clean build` - compile against Paper 1.21.4 and run unit tests.
- `./gradlew shadowJar` - produce `build/libs/MCEFramework-1.0-SNAPSHOT-all.jar` with relocated dependencies.
- `./gradlew runServer` - launch a local Paper server in `run/`; place the shaded jar in `run/plugins/` when prompted for manual checks.

## Coding Style & Naming Conventions
Target Java 21 with 4 space indentation, same line braces, and explicit `@Override` on plugin hooks. Packages stay lowercase (`mcevent.MCEFramework.games.survivalGame`), classes use PascalCase, and shared helpers often start with `MCE` (for example `MCEGlowingEffectManager`). Constants are `UPPER_SNAKE_CASE`, config keys mirror their YAML path (such as `spectator.teleportDelay`), and Lombok is welcome for data holders but avoided in Bukkit entry points.

## Testing Guidelines
Add unit or integration coverage under `src/test/java`, naming files `<ClassUnderTest>Test`, and prefer MockBukkit or lightweight stubs over spinning up a server inside tests. Run `./gradlew test` before opening a PR and document any manual `runServer` steps (commands issued, expected scoreboard or chat output) so reviewers can reproduce them.

## Commit & Pull Request Guidelines
History follows a light Conventional Commits style (`fix:`, `feat:`), so keep short but descriptive messages and squash fixups. Pull requests must outline player facing impact, link issues, attach testing evidence (logs, screenshots, reproduction commands), and flag config migrations or data resets so ops can prepare.

## Configuration & Security Notes
Only commit redacted templates under `src/main/resources/MCEConfig`; keep production secrets on the server. When adding YAML options, document defaults in the relevant parser (for example `VotingSystemConfigParser`) and describe upgrade steps in the PR. Shadow relocates `co.aikar.*` and `fr.mrmicky.fastboard`, so avoid reflection on the original package names.
