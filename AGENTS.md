# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Werewolf is a Spigot/Bukkit Minecraft server plugin (`com.dogonfire.werewolf`), built with Maven against `paper-api` 26.3 (runtime minimum 26.1, enforced by `api-version` in `plugin.yml` and the Java 25 bytecode). It must still run on Spigot: the only Paper-only API calls live in `disguises/PaperSkinFactory`, which is only loaded after a runtime check. Source targets Java 25 (`maven.compiler.release` in `pom.xml`), which 26.x servers require anyway; CI (`.github/workflows/build.yml`) builds on JDK 25 for every push and PR; pushing a tag equal to the pom version (e.g. `0.12.0`) publishes a GitHub release with the jar.

## Commands

```bash
nix develop                # devshell with JDK 25 + Maven (flake.nix); prefix commands with `nix develop -c` otherwise
mvn clean package          # default goal; produces target/Werewolf-<version>.jar
mvn -B clean package       # what CI runs (there are no unit tests; this only checks it compiles)
```

- There is no test suite. You can only check that it compiles; to check behavior, drop the jar into a Spigot server's `plugins/` folder.
- Non-standard layout: `sourceDirectory` is `src/` (packages live directly under `src/com/...`), and resources come from `src/resources/` with Maven filtering on (`plugin.yml` gets `${project.version}`).
- All plugin dependencies (Vault, LibsDisguises, SkinsRestorer, WorldGuard, ProtocolLib, PlaceholderAPI, Vampire) are `provided` and resolved from the third-party repos listed in `pom.xml`. If one of those repos is unreachable, the build fails.
- Releases bump `<version>` in `pom.xml` (e.g. "Bump to v0.11.0"), then push a tag with the same version (e.g. `0.11.0`); CI attaches the jar to a GitHub release. Work goes to the `dev` branch and is merged into `master` through a PR.

## Architecture

**Central singleton.** `Werewolf.java` (the `JavaPlugin`) is the hub:
- It holds every config value as a public field. `loadSettings()`/`saveSettings()` map them to `config.yml`, so a new config option needs a field plus entries in both methods.
- It holds every manager in a static field, reached through static getters (`Werewolf.getWerewolfManager()`, `getClanManager()`, …). Other code calls these getters and `Werewolf.instance()` instead of receiving its dependencies.
- `onEnable()` sets things up in an order that matters: construct managers and listeners → detect a disguise plugin → detect optional plugins (Vault turns on `HuntManager`, bounties and signs; also Vampire, HealthBar, PlaceholderAPI) → register listeners → `loadSettings`/`saveSettings` → each manager's `load()` → recipes → repeating tasks. Some managers are only created when a feature is on (`huntManager` needs Vault, `werewolfScoreboardManager` needs `useScoreboards`, `InventoryListener` needs `dropArmorOnTransform`), so they can be null.

**Main game loop.** A sync repeating task calls `WerewolfManager.update()` every 100 ticks. It checks every online werewolf for transform/untransform, and gives one random player a chance at an "urge" message. That method handles night/full-moon transformations, the `/ww transform` time limit, infection progress and auto-cure. Moon phase and night timing live in `managers/MoonManager` (`getMoonPhase`, `isNight`, `isFullMoonNight`, `isTransformationNight` = the nights the moon forces outdoor werewolves to transform, `setMoonPhase`, dusk announcements); `isUnderOpenSky` and `allowedWorlds` are on `Werewolf`. Clans update on a separate 72000-tick task.

**Levels.** A werewolf's level is `WerewolfManager.getNumberOfTransformations` (`Transformations` in `werewolves.yml`); `Maturity.*` config keys are the levels that unlock perks. `grantFullMoonLevel` adds one level per transformation night, guarded by `LastLevelNight` so relogging doesn't grant more. Wolf forms entered with `Reason.CONTROLLED` are tracked in memory: they are not reverted at morning but expire after the `Transformation.*` time limit (`Reason.TIME_LIMIT`); every other wolf form is reverted at morning. Cooldowns (transform/howl/growl) are in memory too.

**Events.** `events/` holds cancellable Bukkit events fired at the choke points (plus `TrackerTargetEvent` and `WerewolfSniffEvent`, see Hunting gear): `WerewolfInfectionEvent` (`WerewolfManager.makeWerewolf`), `WerewolfCureEvent` (`unmakeWerewolf`), `WerewolfTransformEvent` (`setWerewolfSkin`/`unsetWerewolfSkin`, cancelling is ignored for `Reason.FORCED`) and `NewAlphaEvent` (`ClanManager.assignAlphaInClan`). Each of those methods takes a reason/cause and returns whether it went ahead; new code that changes werewolf state should go through them, and features that need to veto or react to a change should listen to these events instead of adding checks at every call site.

**Wolf form.** `managers/WolfFormManager` owns what wolf form does to a player: per-clan `Stats` (speed, defense = multiplier on incoming damage, fist/item damage; separate alpha values, read in `DamageListener`), per-clan potion effects (`Werewolf.Effects.<ClanType>`, `NAME:amplifier:duration`), blocked potions and the diet's food list. `DisguiseTask` calls `applyWolfForm`, `WerewolfManager.update()` calls `refreshWolfForm` for everyone in wolf form, and `UndisguiseTask`/join call `removeWolfForm`, which removes only the effects it applied. The config lists are parsed in `WolfFormManager.load()` at the end of `loadSettings()`. `listeners/WerewolfRestrictionListener` enforces `BlockedCommands.*` (bypass: `werewolf.bypass.blockedcommands`), blocked potions, the `Diet.*` settings, and vetoes turning into a wolf in creative/spectator.


**Package roles:**
- `managers/` hold state and logic. Each owns its own YAML file in the plugin data folder: `werewolves.yml` (WerewolfManager), `clans.yml` (ClanManager), `hunters.yml` (HuntManager), `trophies.yml` (TrophyManager).
- `listeners/` turn Bukkit events into manager calls.
- `tasks/` are one-off `Runnable`s scheduled with `runTaskLater`. Transforming a player is asynchronous: `disguiseWerewolf()` schedules a `DisguiseTask`, and `undisguiseWerewolf()` schedules an `UndisguiseTask`.
- `commands/Commands.java` handles `/werewolf` (`/ww`), `/growl` and `/howl` in one big `if/else` chain on `args[0]`. Any new subcommand also needs a `usage` entry and its permission nodes in `src/resources/plugin.yml`.

**Disguises.** `disguises/WerewolfDisguiseAPI` hides which skin backend is in use. `init(backend)` runs after config is loaded and takes `Disguises.Backend`: `auto` picks `PaperSkinFactory` (built in, uses Paper's `Player#setPlayerProfile`) on Paper, otherwise `LibsDisguisesFactory`, then `SkinsRestorerFactory`; `builtin`/`libsdisguises`/`skinsrestorer` force one. All implement `IWerewolfDisguiseFactory`. If none is usable, `disguisesEnabled` is false and werewolves keep their normal skin. The werewolf skin textures and signatures, and the fake account names/UUIDs for the four werewolf types (Alpha, Witherfang/potion, Bloodmoon/WerewolfBite, Silvermane/WildBite), are defaults in `Werewolf.java` and saved to config.yml (`migrateSwappedClanSkins` fixes configs from before Bloodmoon and Silvermane got their own skins).

**Custom items.** Silver sword, silver armor, wolfbane, infection/cure potions, the lore book and the werewolf/vampire tracker compasses get a `PersistentDataContainer` tag (key `werewolf:ww-item`) in `ItemManager`. Code checks that tag to recognize an item, not its display name. Crafting recipes are in `items/Recipes.java`, and each one is turned on or off by a `craftable*Enabled` flag.

**Hunting gear.** All four silver armor pieces share the `SilverArmor` tag; `ItemManager.ArmorPiece` maps the slot worn to its `Items.SilverArmor.<Piece>.Purity/Defense`. Purity (summed %) is the chance to cancel a bite infection (`HuntingGearListener`, and `VampireHuntingListener` for VampireRevamp, registered only when it is present); defense is applied in `DamageListener.applySilverArmor` against werewolf/vampire attackers. Only humans (`WerewolfAPI.isHuman`) may wear it or use/craft trackers. Silver sword hits on wolf form skip the clan defence and take `SilverSwordMultiplier` in `DamageListener` (not in `DamageManager.getDamageFromItemInHand`). One sync task in `onEnable` runs every 20 ticks: `TrackerManager.update` (tracker compass locks), `ScentManager.update` (`/ww track` particle trail, sniffed targets) and `ItemManager.update` (silver sword penalty, taking silver armor off non-humans). Tracker locks, scent targets and sniff cooldowns are in memory. `TrackerTargetEvent` and `WerewolfSniffEvent` are cancellable hooks for region/PvP plugins.

**Localization.** The repo's `lang/` folder is bundled into the jar. `LanguageManager` reads `plugins/Werewolf/lang/<language>.yml`, copying the bundled file there first if it is missing, so server admins can edit it. Keys missing from that file fall back to the bundled file for the same language, then to bundled `english.yml`. Each language key is a `LANGUAGESTRING` enum constant whose name matches a top-level YAML key holding a list of variants (one is chosen at random). Placeholders such as `$PlayerName`, `$Amount`, `$Type` and `$ServerName` are filled from fields set right before the lookup. A new message needs an enum constant plus an entry in `lang/english.yml` (other languages fall back to it). Without the english entry players see `<KEY> MISSING`.
