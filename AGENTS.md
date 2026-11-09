# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Werewolf is a Spigot/Bukkit Minecraft server plugin (`com.dogonfire.werewolf`), built with Maven against `spigot-api` 26.3 (runtime minimum 26.1; see `MIN`/`MAX` in `Werewolf.java`). Source targets Java 25 (`maven.compiler.release` in `pom.xml`), which 26.x servers require anyway; CI builds on JDK 25.

## Commands

```bash
nix develop                # devshell with JDK 25 + Maven (flake.nix); prefix commands with `nix develop -c` otherwise
mvn clean package          # default goal; produces target/Werewolf-<version>.jar
mvn -B test                # what GitHub Actions runs on every push (compiles only — there are no unit tests)
```

- There is no test suite. You can only check that it compiles; to check behavior, drop the jar into a Spigot server's `plugins/` folder.
- Non-standard layout: `sourceDirectory` is `src/` (packages live directly under `src/com/...`), and resources come from `src/resources/` with Maven filtering on (`plugin.yml` gets `${project.version}`).
- All plugin dependencies (Vault, LibsDisguises, SkinsRestorer, WorldGuard, ProtocolLib, PlaceholderAPI, Vampire) are `provided` and resolved from the third-party repos listed in `pom.xml`. If one of those repos is unreachable, the build fails.
- Releases bump `<version>` in `pom.xml` (e.g. "Bump to v0.11.0"). Work goes to the `dev` branch and is merged into `master` through a PR.

## Architecture

**Central singleton.** `Werewolf.java` (the `JavaPlugin`) is the hub:
- It holds every config value as a public field. `loadSettings()`/`saveSettings()` map them to `config.yml`, so a new config option needs a field plus entries in both methods.
- It holds every manager in a static field, reached through static getters (`Werewolf.getWerewolfManager()`, `getClanManager()`, …). Other code calls these getters and `Werewolf.instance()` instead of receiving its dependencies.
- `onEnable()` sets things up in an order that matters: version check → construct managers and listeners → detect a disguise plugin → detect optional plugins (Vault turns on `HuntManager`, bounties and signs; also Vampire, HealthBar, PlaceholderAPI) → register listeners → `loadSettings`/`saveSettings` → each manager's `load()` → recipes → repeating tasks. Some managers are only created when a feature is on (`huntManager` needs Vault, `werewolfScoreboardManager` needs `useScoreboards`, `InventoryListener` needs `dropArmorOnTransform`), so they can be null.

**Main game loop.** A sync repeating task calls `WerewolfManager.update()` every 100 ticks. That method handles night/full-moon transformations, infection progress and auto-cure. Moon phase and night timing helpers (`isFullMoonInWorld`, `isNightInWorld`, `isUnderOpenSky`, `allowedWorlds`) are on `Werewolf`. Clans update on a separate 72000-tick task.

**Package roles:**
- `managers/` hold state and logic. Each owns its own YAML file in the plugin data folder: `werewolves.yml` (WerewolfManager), `clans.yml` (ClanManager), `hunters.yml` (HuntManager), `trophies.yml` (TrophyManager).
- `listeners/` turn Bukkit events into manager calls.
- `tasks/` are one-off `Runnable`s scheduled with `runTaskLater`. Transforming a player is asynchronous: `disguiseWerewolf()` schedules a `DisguiseTask`, and `undisguiseWerewolf()` schedules an `UndisguiseTask`.
- `commands/Commands.java` handles `/werewolf` (`/ww`), `/growl` and `/howl` in one big `if/else` chain on `args[0]`. Any new subcommand also needs a `usage` entry and its permission nodes in `src/resources/plugin.yml`.

**Disguises.** `disguises/WerewolfDisguiseAPI` hides which skin plugin is in use. `init()` chooses `LibsDisguisesFactory` first, then `SkinsRestorerFactory`. Both implement `IWerewolfDisguiseFactory`. MySkin is detected but no longer supported. If neither plugin is present, `disguisesEnabled` is false and werewolves keep their normal skin. The werewolf skin textures and signatures, and the fake account UUIDs for the four werewolf types (Alpha, Witherfang/potion, Bloodmoon, Silvermane), are hardcoded in `Werewolf.java`.

**Custom items.** Silver sword, silver armor, wolfbane, infection/cure potions and the lore book get a `PersistentDataContainer` tag (key `werewolf:ww-item`) in `ItemManager`. Code checks that tag to recognize an item, not its display name. Crafting recipes are in `items/Recipes.java`, and each one is turned on or off by a `craftable*Enabled` flag.

**Localization.** `LanguageManager` reads `plugins/Werewolf/lang/<language>.yml`. If that file is missing, it **downloads it at runtime from `raw.githubusercontent.com/dogonfire/werewolf/master/lang/`**; the `lang/` folder in this repo is not packaged in the jar. Each language key is a `LANGUAGESTRING` enum constant whose name matches a top-level YAML key holding a list of variants (one is chosen at random). Placeholders such as `$PlayerName`, `$Amount`, `$Type` and `$ServerName` are filled from fields set right before the lookup. A new message needs an enum constant plus an entry in each `lang/*.yml` (at minimum `english.yml`). Otherwise players see `<KEY> MISSING`.
